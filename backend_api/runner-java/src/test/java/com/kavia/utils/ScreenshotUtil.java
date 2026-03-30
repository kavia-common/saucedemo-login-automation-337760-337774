package com.kavia.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for capturing browser screenshots during test execution.
 *
 * Contract:
 * - Input: a WebDriver instance that supports TakesScreenshot
 * - Output: screenshot as byte[] or saved to disk
 * - Errors: returns null/empty on failure, never throws
 */
public final class ScreenshotUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final String SCREENSHOT_DIR = "target/screenshots";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtil() {
        // Utility class — prevent instantiation
    }

    // PUBLIC_INTERFACE
    /**
     * Takes a screenshot and returns it as a byte array.
     * Returns null if the driver does not support screenshots or an error occurs.
     *
     * @param driver the WebDriver instance
     * @return screenshot bytes (PNG), or null on failure
     */
    public static byte[] takeScreenshotAsBytes(WebDriver driver) {
        if (driver == null) {
            LOG.warn("Cannot take screenshot: driver is null");
            return null;
        }
        if (!(driver instanceof TakesScreenshot)) {
            LOG.warn("Cannot take screenshot: driver does not support TakesScreenshot");
            return null;
        }
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            LOG.error("Failed to capture screenshot as bytes", e);
            return null;
        }
    }

    // PUBLIC_INTERFACE
    /**
     * Takes a screenshot and saves it to the target/screenshots directory.
     * The filename is generated using the scenario name and a timestamp.
     *
     * @param driver       the WebDriver instance
     * @param scenarioName a descriptive name for the screenshot file
     * @return the Path where the screenshot was saved, or null on failure
     */
    public static Path takeScreenshotToFile(WebDriver driver, String scenarioName) {
        if (driver == null || !(driver instanceof TakesScreenshot)) {
            LOG.warn("Cannot take screenshot: driver is null or does not support TakesScreenshot");
            return null;
        }
        try {
            // Ensure the screenshot directory exists
            Path dir = Paths.get(SCREENSHOT_DIR);
            Files.createDirectories(dir);

            // Build a safe filename from the scenario name
            String safeName = (scenarioName != null ? scenarioName : "unknown")
                    .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                    .substring(0, Math.min(scenarioName != null ? scenarioName.length() : 7, 60));

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String filename = safeName + "_" + timestamp + ".png";
            Path filePath = dir.resolve(filename);

            // Capture and write
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(filePath, screenshotBytes);

            LOG.info("Screenshot saved: {}", filePath.toAbsolutePath());
            return filePath;
        } catch (IOException e) {
            LOG.error("Failed to save screenshot to file for scenario: {}", scenarioName, e);
            return null;
        }
    }
}
