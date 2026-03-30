package com.kavia.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * InventoryPage Page Object.
 *
 * Successful login redirects to /inventory.html.
 */
public class InventoryPage {

    private final WebDriver driver;
    private final By inventoryContainer = By.id("inventory_container");

    public InventoryPage(WebDriver driver) {
        this.driver = driver;
    }

    // PUBLIC_INTERFACE
    public boolean isLoaded() {
        return driver.getCurrentUrl().contains("inventory.html") && driver.findElements(inventoryContainer).size() > 0;
    }
}
