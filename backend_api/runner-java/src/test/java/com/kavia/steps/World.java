package com.kavia.steps;

import org.openqa.selenium.WebDriver;

/**
 * Scenario-scoped state holder.
 */
public class World {
    private WebDriver driver;

    // PUBLIC_INTERFACE
    public WebDriver getDriver() {
        return driver;
    }

    // PUBLIC_INTERFACE
    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }
}
