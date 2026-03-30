package com.kavia.steps;

import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * Scenario-scoped state holder for sharing context between step definitions.
 *
 * This class acts as a lightweight dependency-injection container for Cucumber scenarios.
 * Each scenario gets its own instance via Cucumber's PicoContainer integration.
 *
 * Contract:
 * - Holds the WebDriver instance for the current scenario
 * - Stores the scenario name for logging/reporting
 * - Provides a generic key-value store for inter-step data sharing
 */
public class World {

    private WebDriver driver;
    private String scenarioName;
    private final Map<String, Object> scenarioData = new HashMap<>();

    // PUBLIC_INTERFACE
    /**
     * Gets the WebDriver instance for the current scenario.
     *
     * @return the WebDriver instance, or null if not yet initialized
     */
    public WebDriver getDriver() {
        return driver;
    }

    // PUBLIC_INTERFACE
    /**
     * Sets the WebDriver instance for the current scenario.
     *
     * @param driver the WebDriver instance to use
     */
    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    // PUBLIC_INTERFACE
    /**
     * Gets the name of the current scenario.
     *
     * @return the scenario name
     */
    public String getScenarioName() {
        return scenarioName;
    }

    // PUBLIC_INTERFACE
    /**
     * Sets the name of the current scenario.
     *
     * @param scenarioName the scenario name
     */
    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    // PUBLIC_INTERFACE
    /**
     * Stores a value in the scenario-scoped data map for sharing between steps.
     *
     * @param key   the key to store the value under
     * @param value the value to store
     */
    public void put(String key, Object value) {
        scenarioData.put(key, value);
    }

    // PUBLIC_INTERFACE
    /**
     * Retrieves a value from the scenario-scoped data map.
     *
     * @param key the key to look up
     * @return the stored value, or null if not found
     */
    public Object get(String key) {
        return scenarioData.get(key);
    }

    // PUBLIC_INTERFACE
    /**
     * Retrieves a typed value from the scenario-scoped data map.
     *
     * @param key  the key to look up
     * @param type the expected type of the value
     * @param <T>  the type parameter
     * @return the stored value cast to the specified type, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = scenarioData.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }
}
