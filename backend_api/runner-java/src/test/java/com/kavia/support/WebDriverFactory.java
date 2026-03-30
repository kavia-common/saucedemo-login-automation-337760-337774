package com.kavia.support;

import com.kavia.utils.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.Duration;

/**
 * WebDriverFactory creates and configures WebDriver instances.
 *
 * <h3>Flow: CreateWebDriver</h3>
 * <p>Single canonical entrypoint for obtaining a configured WebDriver.</p>
 *
 * <h3>Strategy</h3>
 * <ul>
 *   <li>If SAUCE_USERNAME and SAUCE_ACCESS_KEY are present → RemoteWebDriver to Sauce Labs.</li>
 *   <li>Otherwise → local browser driver.</li>
 * </ul>
 *
 * <h3>Supported local browsers</h3>
 * <ul>
 *   <li><b>chrome</b> (default) – managed by WebDriverManager (downloads driver automatically).</li>
 *   <li><b>firefox</b> – managed by WebDriverManager.</li>
 *   <li><b>edge / msedge</b> – uses a <em>preinstalled</em> msedgedriver binary.
 *       The binary path is resolved via the {@code webdriver.edge.driver} config property
 *       (which can be overridden by the {@code WEBDRIVER_EDGE_DRIVER} env var or
 *       {@code -Dwebdriver.edge.driver} system property). No network download is attempted.</li>
 * </ul>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Callers own the returned driver and <b>must</b> call {@code quit()} when done.</li>
 *   <li>Browser type: configurable via {@code qa.browser} system property or
 *       {@code browser.name} in config.properties (default: "chrome").</li>
 *   <li>Headless mode: configurable via {@code browser.headless} (default: true).</li>
 * </ul>
 *
 * <h3>Failure modes</h3>
 * <ol>
 *   <li>Missing msedgedriver path for Edge → RuntimeException with actionable message.</li>
 *   <li>WebDriverManager download failure for Chrome/Firefox → RuntimeException.</li>
 *   <li>Sauce Labs credentials invalid → RemoteWebDriver construction failure.</li>
 * </ol>
 */
public final class WebDriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(WebDriverFactory.class);

    /**
     * Config property key for the preinstalled msedgedriver binary path.
     * Resolved via ConfigReader: env var WEBDRIVER_EDGE_DRIVER → system property
     * webdriver.edge.driver → config.properties value.
     */
    private static final String EDGE_DRIVER_PATH_KEY = "webdriver.edge.driver";

    private WebDriverFactory() {
        // Utility class — prevent instantiation
    }

    // PUBLIC_INTERFACE
    /**
     * Creates a new WebDriver instance based on configuration.
     *
     * <p>Resolution order for browser type:</p>
     * <ol>
     *   <li>System property {@code qa.browser}</li>
     *   <li>{@code config.properties} key {@code browser.name}</li>
     *   <li>Default: "chrome"</li>
     * </ol>
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

        LOG.info("[CreateWebDriver] Starting driver creation (browser={}, headless={})", browser, headless);

        try {
            // Remote execution via Sauce Labs
            if (sauceUser != null && !sauceUser.isBlank() && sauceKey != null && !sauceKey.isBlank()) {
                return createRemoteDriver(sauceUser, sauceKey, browser);
            }

            // Local execution
            return createLocalDriver(browser, headless);
        } catch (Exception e) {
            LOG.error("[CreateWebDriver] Failed to create WebDriver (browser={})", browser, e);
            throw new RuntimeException(
                "Failed to create WebDriver for browser '" + browser
                + "'. Ensure the browser driver is available, or set SAUCE_* env vars. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a RemoteWebDriver for Sauce Labs execution.
     *
     * @param sauceUser Sauce Labs username
     * @param sauceKey  Sauce Labs access key
     * @param browser   browser name capability
     * @return configured RemoteWebDriver
     * @throws Exception if remote connection fails
     */
    private static WebDriver createRemoteDriver(String sauceUser, String sauceKey, String browser) throws Exception {
        LOG.info("[CreateWebDriver] Creating RemoteWebDriver for Sauce Labs (browser={})", browser);

        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("browserName", browser);
        caps.setCapability("name", "SauceDemo Login Cucumber Run");
        caps.setCapability("build", "kavia-backend-api");

        URL url = new URL("https://" + sauceUser + ":" + sauceKey + "@ondemand.saucelabs.com:443/wd/hub");
        RemoteWebDriver driver = new RemoteWebDriver(url, caps);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));

        LOG.info("[CreateWebDriver] RemoteWebDriver created successfully");
        return driver;
    }

    /**
     * Creates a local WebDriver instance.
     *
     * <p>Chrome and Firefox use WebDriverManager for automatic driver management.
     * Edge uses a preinstalled msedgedriver binary (no network download).</p>
     *
     * @param browser  the browser identifier (chrome, firefox, edge, msedge)
     * @param headless whether to run in headless mode
     * @return configured local WebDriver
     */
    private static WebDriver createLocalDriver(String browser, boolean headless) {
        LOG.info("[CreateWebDriver] Creating local WebDriver (browser={}, headless={})", browser, headless);

        WebDriver driver;

        switch (browser) {
            case "firefox":
                driver = createFirefoxDriver(headless);
                break;

            case "edge":
            case "msedge":
                driver = createEdgeDriver(headless);
                break;

            case "chrome":
            default:
                driver = createChromeDriver(headless);
                break;
        }

        LOG.info("[CreateWebDriver] Local WebDriver created successfully ({})", browser);
        return driver;
    }

    /**
     * Creates a Chrome WebDriver with WebDriverManager.
     *
     * @param headless whether to enable headless mode
     * @return configured ChromeDriver
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        if (headless) {
            chromeOptions.addArguments("--headless=new");
        }
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--window-size=1920,1080");
        return new ChromeDriver(chromeOptions);
    }

    /**
     * Creates a Firefox WebDriver with WebDriverManager.
     *
     * @param headless whether to enable headless mode
     * @return configured FirefoxDriver
     */
    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        if (headless) {
            firefoxOptions.addArguments("--headless");
        }
        return new FirefoxDriver(firefoxOptions);
    }

    /**
     * Creates an Edge WebDriver using a preinstalled msedgedriver binary.
     *
     * <p>The driver binary path is resolved from configuration, not downloaded at runtime.
     * This is required because the msedgedriver CDN (msedgedriver.azureedge.net) may be
     * unreachable in restricted/CI environments.</p>
     *
     * <p>Resolution order for the binary path:</p>
     * <ol>
     *   <li>Environment variable: {@code WEBDRIVER_EDGE_DRIVER}</li>
     *   <li>System property: {@code -Dwebdriver.edge.driver=/path/to/msedgedriver}</li>
     *   <li>Config property: {@code webdriver.edge.driver} in config.properties</li>
     * </ol>
     *
     * @param headless whether to enable headless mode
     * @return configured EdgeDriver
     * @throws RuntimeException if msedgedriver path is not configured or binary is not found
     */
    private static WebDriver createEdgeDriver(boolean headless) {
        // Resolve the msedgedriver binary path from configuration
        String edgeDriverPath = ConfigReader.getProperty(EDGE_DRIVER_PATH_KEY);

        if (edgeDriverPath == null || edgeDriverPath.isBlank()) {
            String errorMsg = "Edge WebDriver requires a preinstalled msedgedriver binary, but no path was configured. "
                    + "Please set one of the following:\n"
                    + "  1. Environment variable: WEBDRIVER_EDGE_DRIVER=/path/to/msedgedriver\n"
                    + "  2. System property: -Dwebdriver.edge.driver=/path/to/msedgedriver\n"
                    + "  3. Config property in config.properties: webdriver.edge.driver=/path/to/msedgedriver\n"
                    + "This is needed because msedgedriver.azureedge.net may be unreachable in restricted environments.";
            LOG.error("[CreateWebDriver] {}", errorMsg);
            throw new RuntimeException(errorMsg);
        }

        File driverFile = new File(edgeDriverPath);
        if (!driverFile.exists()) {
            String errorMsg = "msedgedriver binary not found at configured path: " + edgeDriverPath
                    + ". Please verify the path is correct and the binary is installed.";
            LOG.error("[CreateWebDriver] {}", errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (!driverFile.canExecute()) {
            LOG.warn("[CreateWebDriver] msedgedriver at '{}' may not be executable. "
                    + "Attempting to proceed anyway.", edgeDriverPath);
        }

        LOG.info("[CreateWebDriver] Using preinstalled msedgedriver at: {}", edgeDriverPath);

        // Set the system property that Selenium uses to locate msedgedriver
        System.setProperty("webdriver.edge.driver", edgeDriverPath);

        EdgeOptions edgeOptions = new EdgeOptions();
        if (headless) {
            edgeOptions.addArguments("--headless=new");
        }
        edgeOptions.addArguments("--no-sandbox");
        edgeOptions.addArguments("--disable-dev-shm-usage");
        edgeOptions.addArguments("--disable-gpu");
        edgeOptions.addArguments("--window-size=1920,1080");

        EdgeDriverService service = new EdgeDriverService.Builder()
                .usingDriverExecutable(driverFile)
                .build();

        return new EdgeDriver(service, edgeOptions);
    }
}
