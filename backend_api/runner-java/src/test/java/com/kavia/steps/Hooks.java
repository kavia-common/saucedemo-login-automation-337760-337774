package com.kavia.steps;

import com.kavia.support.WebDriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.openqa.selenium.WebDriver;

/**
 * Hooks manage WebDriver lifecycle.
 */
public class Hooks {

    private final World world;

    public Hooks(World world) {
        this.world = world;
    }

    @Before
    public void beforeScenario() {
        WebDriver driver = WebDriverFactory.create();
        world.setDriver(driver);
    }

    @After
    public void afterScenario() {
        if (world.getDriver() != null) {
            world.getDriver().quit();
        }
    }
}
