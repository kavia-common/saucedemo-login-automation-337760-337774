package com.kavia.support;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.time.Duration;

/**
 * WebDriverFactory
 *
 * Strategy:
 * - If SAUCE_USERNAME and SAUCE_ACCESS_KEY are present, create a RemoteWebDriver to Sauce Labs.
 * - Otherwise, create a local Chrome driver.
 *
 * Contract:
 * - Callers own the returned driver and must quit it.
 */
public final class WebDriverFactory {

    private WebDriverFactory() { }

    // PUBLIC_INTERFACE
    public static WebDriver create() {
        String sauceUser = System.getenv("SAUCE_USERNAME");
        String sauceKey = System.getenv("SAUCE_ACCESS_KEY");
        String browser = System.getProperty("qa.browser", "chrome");

        try {
            if (sauceUser != null && !sauceUser.isBlank() && sauceKey != null && !sauceKey.isBlank()) {
                MutableCapabilities caps = new MutableCapabilities();
                // Minimal W3C capabilities; Sauce Labs will infer additional defaults.
                caps.setCapability("browserName", browser);

                // Basic build/name metadata for Sauce Labs UI.
                caps.setCapability("name", "SauceDemo Login Cucumber Run");
                caps.setCapability("build", "kavia-backend-api");

                URL url = new URL("https://" + sauceUser + ":" + sauceKey + "@ondemand.saucelabs.com:443/wd/hub");
                RemoteWebDriver driver = new RemoteWebDriver(url, caps);
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
                return driver;
            }

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");

            WebDriver driver = new org.openqa.selenium.chrome.ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
            return driver;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create WebDriver. Ensure ChromeDriver is available, or set SAUCE_* env vars.", e);
        }
    }
}
