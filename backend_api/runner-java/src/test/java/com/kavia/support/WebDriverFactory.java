package com.kavia.support;

import com.kavia.utils.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;

/**
 * WebDriverFactory creates and configures WebDriver instances.
 *
 * Strategy:
 * - If SAUCE_USERNAME and SAUCE_ACCESS_KEY are present, create a RemoteWebDriver to Sauce Labs.
 * - Otherwise, create a local browser driver (Chrome or Firefox) managed by WebDriverManager.
 *
 * Contract:
 * - Callers own the returned driver and must quit it.
 * - Browser type is configurable via "qa.browser" system property or config.properties.
 * - Headless mode is configurable via "browser.headless" property (default: true).
 */
public final class WebDriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(WebDriverFactory.class);

    private WebDriverFactory() {
        // Utility class — prevent instantiation
    }

    // PUBLIC_INTERFACE
    /**
     * Creates a new WebDriver instance based on configuration.
     *
     * Resolution order for browser type:
     *   1. System property "qa.browser"
     *   2. config.properties "browser.name"
     *   3. Default: "chrome"
     *
     * @return a fully configured WebDriver instance
     * @throws RuntimeException if driver creation fails
     */
    public static WebDriver create() {
        String sauceUser = System.getenv("SAUCE_USERNAME");
        String sauceKey = System.getenv("SAUCE_ACCESS_KEY");
        String browser = ConfigReader.getProperty("qa.browser",
                ConfigReader.getProperty("browser.name", "chrome")).toLowerCase();
        boolean headless = ConfigReader.getBooleanProperty("browser.headless", true);

        try {
            // Remote execution via Sauce Labs
            if (sauceUser != null && !sauceUser.isBlank() && sauceKey != null && !sauceKey.isBlank()) {
                return createRemoteDriver(sauceUser, sauceKey, browser);
            }

            // Local execution
            return createLocalDriver(browser, headless);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create WebDriver. Ensure browser driver is available, or set SAUCE_* env vars.", e);
        }
    }

    /**
     * Creates a RemoteWebDriver for Sauce Labs execution.
     */
    private static WebDriver createRemoteDriver(String sauceUser, String sauceKey, String browser) throws Exception {
        LOG.info("Creating RemoteWebDriver for Sauce Labs (browser={})", browser);

        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("browserName", browser);
        caps.setCapability("name", "SauceDemo Login Cucumber Run");
        caps.setCapability("build", "kavia-backend-api");

        URL url = new URL("https://" + sauceUser + ":" + sauceKey + "@ondemand.saucelabs.com:443/wd/hub");
        RemoteWebDriver driver = new RemoteWebDriver(url, caps);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));

        LOG.info("RemoteWebDriver created successfully");
        return driver;
    }

    /**
     * Creates a local WebDriver instance with WebDriverManager.
     */
    private static WebDriver createLocalDriver(String browser, boolean headless) {
        LOG.info("Creating local WebDriver (browser={}, headless={})", browser, headless);

        WebDriver driver;

        switch (browser) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (headless) {
                    firefoxOptions.addArguments("--headless");
                }
                driver = new FirefoxDriver(firefoxOptions);
                break;

            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                if (headless) {
                    chromeOptions.addArguments("--headless=new");
                }
                chromeOptions.addArguments("--no-sandbox");
                chromeOptions.addArguments("--disable-dev-shm-usage");
                chromeOptions.addArguments("--disable-gpu");
                chromeOptions.addArguments("--window-size=1920,1080");
                driver = new ChromeDriver(chromeOptions);
                break;
        }

        LOG.info("Local WebDriver created successfully ({})", browser);
        return driver;
    }
}
