package com.kavia.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Utility class providing explicit wait helpers for Selenium WebDriver.
 *
 * These methods wrap WebDriverWait with common ExpectedConditions
 * to improve test stability and reduce flaky failures.
 *
 * Contract:
 * - Input: WebDriver + locator/conditions + timeout
 * - Output: located WebElement or boolean
 * - Errors: throws TimeoutException if condition is not met within timeout
 */
public final class WaitUtil {

    private static final Logger LOG = LoggerFactory.getLogger(WaitUtil.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private WaitUtil() {
        // Utility class — prevent instantiation
    }

    // PUBLIC_INTERFACE
    /**
     * Waits until an element is visible on the page.
     *
     * @param driver         the WebDriver instance
     * @param locator        the By locator for the element
     * @param timeoutSeconds maximum time to wait in seconds
     * @return the located WebElement once visible
     */
    public static WebElement waitForElementVisible(WebDriver driver, By locator, int timeoutSeconds) {
        LOG.debug("Waiting up to {}s for element visible: {}", timeoutSeconds, locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // PUBLIC_INTERFACE
    /**
     * Waits until an element is visible using the default timeout.
     *
     * @param driver  the WebDriver instance
     * @param locator the By locator for the element
     * @return the located WebElement once visible
     */
    public static WebElement waitForElementVisible(WebDriver driver, By locator) {
        return waitForElementVisible(driver, locator, DEFAULT_TIMEOUT_SECONDS);
    }

    // PUBLIC_INTERFACE
    /**
     * Waits until an element is clickable on the page.
     *
     * @param driver         the WebDriver instance
     * @param locator        the By locator for the element
     * @param timeoutSeconds maximum time to wait in seconds
     * @return the located WebElement once clickable
     */
    public static WebElement waitForElementClickable(WebDriver driver, By locator, int timeoutSeconds) {
        LOG.debug("Waiting up to {}s for element clickable: {}", timeoutSeconds, locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // PUBLIC_INTERFACE
    /**
     * Waits until an element is clickable using the default timeout.
     *
     * @param driver  the WebDriver instance
     * @param locator the By locator for the element
     * @return the located WebElement once clickable
     */
    public static WebElement waitForElementClickable(WebDriver driver, By locator) {
        return waitForElementClickable(driver, locator, DEFAULT_TIMEOUT_SECONDS);
    }

    // PUBLIC_INTERFACE
    /**
     * Waits until an element is present in the DOM (may not be visible).
     *
     * @param driver         the WebDriver instance
     * @param locator        the By locator for the element
     * @param timeoutSeconds maximum time to wait in seconds
     * @return the located WebElement once present
     */
    public static WebElement waitForElementPresent(WebDriver driver, By locator, int timeoutSeconds) {
        LOG.debug("Waiting up to {}s for element present: {}", timeoutSeconds, locator);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    // PUBLIC_INTERFACE
    /**
     * Waits until the page URL contains a specific substring.
     *
     * @param driver         the WebDriver instance
     * @param urlFragment    the URL substring to wait for
     * @param timeoutSeconds maximum time to wait in seconds
     * @return true if the URL contains the fragment within timeout
     */
    public static boolean waitForUrlContains(WebDriver driver, String urlFragment, int timeoutSeconds) {
        LOG.debug("Waiting up to {}s for URL to contain: {}", timeoutSeconds, urlFragment);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    // PUBLIC_INTERFACE
    /**
     * Waits until the page title contains a specific substring.
     *
     * @param driver         the WebDriver instance
     * @param titleFragment  the title substring to wait for
     * @param timeoutSeconds maximum time to wait in seconds
     * @return true if the title contains the fragment within timeout
     */
    public static boolean waitForTitleContains(WebDriver driver, String titleFragment, int timeoutSeconds) {
        LOG.debug("Waiting up to {}s for title to contain: {}", timeoutSeconds, titleFragment);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.titleContains(titleFragment));
    }
}
