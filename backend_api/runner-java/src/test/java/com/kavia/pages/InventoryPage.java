package com.kavia.pages;

import com.kavia.utils.WaitUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * InventoryPage Page Object for SauceDemo.
 *
 * Represents the inventory/products page that is displayed after successful login.
 * Successful login redirects to /inventory.html.
 *
 * Responsibilities:
 * - Verify inventory page is loaded
 * - Interact with the hamburger (burger) menu
 * - Perform logout
 * - Inspect product listing and images
 */
public class InventoryPage {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryPage.class);

    private final WebDriver driver;

    // Locators
    private final By inventoryContainer = By.id("inventory_container");
    private final By burgerMenuButton = By.id("react-burger-menu-btn");
    private final By logoutLink = By.id("logout_sidebar_link");
    private final By burgerMenuWrap = By.cssSelector(".bm-menu-wrap");
    private final By inventoryItemImages = By.cssSelector(".inventory_item_img img");

    public InventoryPage(WebDriver driver) {
        this.driver = driver;
    }

    // PUBLIC_INTERFACE
    /**
     * Checks whether the inventory page is fully loaded.
     * Validates both URL and presence of the inventory container element.
     *
     * @return true if the inventory page is loaded
     */
    public boolean isLoaded() {
        boolean urlMatch = driver.getCurrentUrl().contains("inventory.html");
        boolean containerPresent = driver.findElements(inventoryContainer).size() > 0;
        boolean loaded = urlMatch && containerPresent;
        LOG.debug("InventoryPage loaded check: urlMatch={}, containerPresent={}, result={}", urlMatch, containerPresent, loaded);
        return loaded;
    }

    // PUBLIC_INTERFACE
    /**
     * Checks if the inventory page is loaded within a specific timeout.
     * Useful for performance_glitch_user where page load is slower.
     *
     * @param timeoutSeconds maximum seconds to wait for the page to load
     * @return true if the inventory page loaded within the timeout
     */
    public boolean isLoadedWithinTimeout(int timeoutSeconds) {
        try {
            WaitUtil.waitForUrlContains(driver, "inventory.html", timeoutSeconds);
            WaitUtil.waitForElementVisible(driver, inventoryContainer, timeoutSeconds);
            return isLoaded();
        } catch (Exception e) {
            LOG.warn("Inventory page did not load within {}s: {}", timeoutSeconds, e.getMessage());
            return false;
        }
    }

    // PUBLIC_INTERFACE
    /**
     * Opens the hamburger (burger) side menu by clicking the menu button.
     * Waits for the menu button to be clickable before interacting.
     */
    public void openBurgerMenu() {
        LOG.info("Opening hamburger menu");
        WebElement menuBtn = WaitUtil.waitForElementClickable(driver, burgerMenuButton, 5);
        menuBtn.click();
        // Wait for the menu to be visible (animation completes)
        try {
            WaitUtil.waitForElementVisible(driver, logoutLink, 5);
        } catch (Exception e) {
            LOG.warn("Logout link not immediately visible after opening menu, proceeding: {}", e.getMessage());
        }
        LOG.info("Hamburger menu opened");
    }

    // PUBLIC_INTERFACE
    /**
     * Clicks the logout link in the side menu.
     * Precondition: The hamburger menu must be open.
     */
    public void clickLogout() {
        LOG.info("Clicking logout link");
        WebElement logout = WaitUtil.waitForElementClickable(driver, logoutLink, 5);
        logout.click();
        LOG.info("Logout link clicked");
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the count of product items visible on the inventory page.
     *
     * @return number of product item images found
     */
    public int getProductImageCount() {
        List<WebElement> images = driver.findElements(inventoryItemImages);
        LOG.debug("Product images count: {}", images.size());
        return images.size();
    }

    // PUBLIC_INTERFACE
    /**
     * Checks if any product images on the inventory page are broken.
     * A broken image is detected by checking if the image src contains
     * the SauceDemo placeholder/broken image indicator (e.g., "sl-404" in the path)
     * or by checking the naturalWidth property (0 means image failed to load).
     *
     * @return true if at least one product image appears broken
     */
    public boolean hasBrokenProductImages() {
        List<WebElement> images = driver.findElements(inventoryItemImages);
        if (images.isEmpty()) {
            LOG.warn("No product images found on inventory page");
            return false;
        }

        int brokenCount = 0;
        for (WebElement img : images) {
            String src = img.getAttribute("src");
            // problem_user images have a different/broken image path
            // Check if image source contains the broken image indicator
            if (src != null && src.contains("WithGarbageOnItToBreakStuff")) {
                brokenCount++;
                continue;
            }
            // Also check naturalWidth via JavaScript (0 means image did not load)
            try {
                Long naturalWidth = (Long) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].naturalWidth;", img);
                if (naturalWidth != null && naturalWidth == 0) {
                    brokenCount++;
                }
            } catch (Exception e) {
                LOG.debug("Could not check naturalWidth for image: {}", e.getMessage());
            }
        }

        LOG.debug("Broken product images: {}/{}", brokenCount, images.size());
        return brokenCount > 0;
    }
}
