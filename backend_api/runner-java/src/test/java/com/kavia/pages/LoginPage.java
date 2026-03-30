package com.kavia.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * LoginPage Page Object for https://www.saucedemo.com/
 *
 * Invariants:
 * - Elements are located by stable IDs used by SauceDemo.
 */
public class LoginPage {

    private final WebDriver driver;

    private final By username = By.id("user-name");
    private final By password = By.id("password");
    private final By loginButton = By.id("login-button");
    private final By error = By.cssSelector("[data-test='error']");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    // PUBLIC_INTERFACE
    public void open() {
        driver.get("https://www.saucedemo.com/");
    }

    // PUBLIC_INTERFACE
    public void login(String user, String pass) {
        WebElement u = driver.findElement(username);
        u.clear();
        u.sendKeys(user);

        WebElement p = driver.findElement(password);
        p.clear();
        p.sendKeys(pass);

        driver.findElement(loginButton).click();
    }

    // PUBLIC_INTERFACE
    public boolean isErrorVisible() {
        return driver.findElements(error).size() > 0;
    }

    // PUBLIC_INTERFACE
    public String getErrorText() {
        if (!isErrorVisible()) return "";
        return driver.findElement(error).getText();
    }
}
