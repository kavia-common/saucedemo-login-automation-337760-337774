package com.kavia.pages;

import com.kavia.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * LoginPage Page Object for SauceDemo.
 *
 * Encapsulates all interactions with the SauceDemo login page.
 *
 * Invariants:
 * - Elements are located by stable IDs used by SauceDemo.
 * - Uses explicit waits for stability in CI environments.
 * - Error UI elements (icons, close button) are queried after a failed login attempt.
 */
public class LoginPage {

    private static final Logger LOG = LoggerFactory.getLogger(LoginPage.class);

    private final WebDriver driver;

    // Locators using stable SauceDemo IDs and selectors
    private final By username = By.id("user-name");
    private final By password = By.id("password");
    private final By loginButton = By.id("login-button");
    private final By error = By.cssSelector("[data-test='error']");
    private final By errorCloseButton = By.cssSelector("[data-test='error'] button.error-button");
    private final By errorIcon = By.cssSelector("svg.error_icon");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    // PUBLIC_INTERFACE
    /**
     * Opens the SauceDemo login page at the default URL.
     */
    public void open() {
        open("https://www.saucedemo.com/");
    }

    // PUBLIC_INTERFACE
    /**
     * Opens the SauceDemo login page at the provided URL.
     *
     * @param url the URL to navigate to
     */
    public void open(String url) {
        LOG.info("Navigating to: {}", url);
        driver.get(url);
    }

    // PUBLIC_INTERFACE
    /**
     * Performs login by entering username and password, then clicking the login button.
     *
     * @param user the username to enter
     * @param pass the password to enter
     */
    public void login(String user, String pass) {
        LOG.info("Logging in with username: '{}'", user);

        WebElement u = WaitUtil.waitForElementVisible(driver, username, 5);
        u.clear();
        u.sendKeys(user);

        WebElement p = WaitUtil.waitForElementVisible(driver, password, 5);
        p.clear();
        p.sendKeys(pass);

        WaitUtil.waitForElementClickable(driver, loginButton, 5).click();
        LOG.info("Login button clicked");
    }

    // PUBLIC_INTERFACE
    /**
     * Checks whether a login error message is currently displayed.
     *
     * @return true if the error container is visible
     */
    public boolean isErrorVisible() {
        boolean visible = driver.findElements(error).size() > 0;
        LOG.debug("Error visible: {}", visible);
        return visible;
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the text of the login error message, if visible.
     *
     * @return the error message text, or empty string if not visible
     */
    public String getErrorText() {
        if (!isErrorVisible()) return "";
        String text = driver.findElement(error).getText();
        LOG.debug("Error text: {}", text);
        return text;
    }

    // PUBLIC_INTERFACE
    /**
     * Checks whether error icons (red exclamation marks) are displayed on input fields
     * after a failed login attempt.
     *
     * @return true if at least one error icon SVG element is present
     */
    public boolean areErrorIconsVisible() {
        List<WebElement> icons = driver.findElements(errorIcon);
        boolean visible = icons.size() > 0;
        LOG.debug("Error icons visible: {} (count: {})", visible, icons.size());
        return visible;
    }

    // PUBLIC_INTERFACE
    /**
     * Clicks the close/dismiss button on the error message container.
     * This removes the error message from the UI.
     *
     * Precondition: An error message must be visible on the page.
     */
    public void clickErrorCloseButton() {
        LOG.info("Clicking error close button");
        WebElement closeBtn = WaitUtil.waitForElementClickable(driver, errorCloseButton, 5);
        closeBtn.click();
        LOG.info("Error close button clicked");
    }

    // PUBLIC_INTERFACE
    /**
     * Checks if the current page is the SauceDemo login page
     * by verifying the URL does not contain inventory or other post-login paths.
     *
     * @return true if the current URL is the login page
     */
    public boolean isLoginPage() {
        String currentUrl = driver.getCurrentUrl();
        boolean isLogin = currentUrl.contains("saucedemo.com")
                && !currentUrl.contains("inventory")
                && !currentUrl.contains("cart")
                && !currentUrl.contains("checkout");
        LOG.debug("Is login page: {} (URL: {})", isLogin, currentUrl);
        return isLogin;
    }

    // PUBLIC_INTERFACE
    /**
     * Waits for the browser to redirect to the login page (e.g., after logout).
     * Uses an explicit wait for the URL to no longer contain post-login paths,
     * then verifies the login page elements are present.
     *
     * Contract:
     * - Input: timeoutSeconds - maximum time to wait for redirect
     * - Output: true if the login page is reached within the timeout
     * - Errors: returns false if timeout elapses without reaching login page
     *
     * @param timeoutSeconds maximum seconds to wait for the redirect
     * @return true if the login page is displayed within the timeout
     */
    public boolean waitForLoginPage(int timeoutSeconds) {
        LOG.info("Waiting up to {}s for redirect to login page", timeoutSeconds);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            // Wait until URL no longer contains "inventory" (the post-login page)
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("inventory")));
            // Additionally wait for the login button to be present, confirming login page loaded
            wait.until(ExpectedConditions.presenceOfElementLocated(loginButton));
            boolean result = isLoginPage();
            LOG.info("Wait for login page complete: result={}", result);
            return result;
        } catch (Exception e) {
            LOG.warn("Timed out waiting for login page redirect: {}", e.getMessage());
            // Final attempt: check current URL directly
            String currentUrl = driver.getCurrentUrl();
            LOG.info("Current URL after timeout: {}", currentUrl);
            return isLoginPage();
        }
    }
}
