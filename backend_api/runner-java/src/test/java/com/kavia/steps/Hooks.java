package com.kavia.steps;

import com.kavia.support.WebDriverFactory;
import com.kavia.utils.ConfigReader;
import com.kavia.utils.ScreenshotUtil;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hooks manage WebDriver lifecycle and cross-cutting concerns
 * for each Cucumber scenario.
 *
 * Responsibilities:
 * - Before: create a fresh WebDriver instance and configure timeouts
 * - After: capture screenshot on failure, then quit the driver
 */
public class Hooks {

    private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);

    private final World world;

    public Hooks(World world) {
        this.world = world;
    }

    /**
     * Before hook: runs before every Cucumber scenario.
     * Creates a new WebDriver instance and stores it in the World context.
     *
     * @param scenario the current Cucumber scenario (injected by Cucumber)
     */
    @Before(order = 0)
    public void beforeScenario(Scenario scenario) {
        LOG.info("========== SCENARIO START: {} ==========", scenario.getName());
        LOG.info("Tags: {}", scenario.getSourceTagNames());

        // Read configuration for implicit wait timeout
        int implicitWaitSeconds = ConfigReader.getIntProperty("implicit.wait.seconds", 2);

        // Create the WebDriver instance
        WebDriver driver = WebDriverFactory.create();
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(implicitWaitSeconds));
        // Note: window().maximize() removed due to Chrome 146 CDP incompatibility.
        // Maximization is handled via --start-maximized Chrome option in WebDriverFactory.

        // Store driver in shared World context
        world.setDriver(driver);
        world.setScenarioName(scenario.getName());

        LOG.info("WebDriver initialized for scenario: {}", scenario.getName());
    }

    /**
     * After hook: runs after every Cucumber scenario.
     * Takes a screenshot on failure for debugging, then quits the driver.
     *
     * @param scenario the current Cucumber scenario (injected by Cucumber)
     */
    @After(order = 0)
    public void afterScenario(Scenario scenario) {
        WebDriver driver = world.getDriver();

        try {
            // Capture screenshot on failure for debugging
            if (scenario.isFailed() && driver != null) {
                LOG.warn("Scenario FAILED: {} — capturing screenshot", scenario.getName());
                byte[] screenshot = ScreenshotUtil.takeScreenshotAsBytes(driver);
                if (screenshot != null && screenshot.length > 0) {
                    scenario.attach(screenshot, "image/png", "failure-screenshot");
                    LOG.info("Screenshot attached to report for failed scenario: {}", scenario.getName());
                }
            }
        } catch (Exception e) {
            LOG.error("Error capturing screenshot for scenario: {}", scenario.getName(), e);
        } finally {
            // Always quit the driver to prevent resource leaks
            if (driver != null) {
                try {
                    driver.quit();
                    LOG.info("WebDriver quit for scenario: {}", scenario.getName());
                } catch (Exception e) {
                    LOG.error("Error quitting WebDriver for scenario: {}", scenario.getName(), e);
                }
            }
        }

        String status = scenario.isFailed() ? "FAILED" : "PASSED";
        LOG.info("========== SCENARIO END: {} [{}] ==========", scenario.getName(), status);
    }
}
