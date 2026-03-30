package com.kavia.pages;

import com.kavia.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoginPage Page Object for SauceDemo.
 *
 * Encapsulates all interactions with the SauceDemo login page.
 *
 * Invariants:
 * - Elements are located by stable IDs used by SauceDemo.
 * - Uses explicit waits for stability in CI environments.
 */
public class LoginPage {

    private static final Logger LOG = LoggerFactory.getLogger(LoginPage.class);

    private final WebDriver driver;

    // Locators using stable SauceDemo IDs
    private final By username = By.id("user-name");
    private final By password = By.id("password");
    private final By loginButton = By.id("login-button");
    private final By error = By.cssSelector("[data-test='error']");

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
}
