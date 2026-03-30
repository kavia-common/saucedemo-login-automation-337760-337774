package com.kavia.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InventoryPage Page Object for SauceDemo.
 *
 * Represents the inventory/products page that is displayed after successful login.
 * Successful login redirects to /inventory.html.
 */
public class InventoryPage {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryPage.class);

    private final WebDriver driver;
    private final By inventoryContainer = By.id("inventory_container");

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
}
