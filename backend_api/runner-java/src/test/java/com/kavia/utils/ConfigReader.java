package com.kavia.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration reader utility for the automation framework.
 *
 * Loads properties from classpath resource "config.properties" and provides
 * typed accessors. Environment variables take precedence over file properties.
 *
 * Contract:
 * - Input: config.properties on classpath, environment variables
 * - Output: typed configuration values with defaults
 * - Thread-safe: uses static initialization
 */
public final class ConfigReader {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigReader.class);
    private static final Properties PROPS = new Properties();
    private static final String CONFIG_FILE = "config.properties";

    static {
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                PROPS.load(input);
                LOG.info("Loaded {} properties from {}", PROPS.size(), CONFIG_FILE);
            } else {
                LOG.warn("Configuration file '{}' not found on classpath; using defaults.", CONFIG_FILE);
            }
        } catch (IOException e) {
            LOG.error("Error loading configuration file '{}'", CONFIG_FILE, e);
        }
    }

    private ConfigReader() {
        // Utility class — prevent instantiation
    }

    // PUBLIC_INTERFACE
    /**
     * Gets a configuration property by key.
     * Environment variables (with dots replaced by underscores, uppercased) take precedence.
     *
     * @param key the property key (e.g., "base.url")
     * @return the property value, or null if not found
     */
    public static String getProperty(String key) {
        // Check environment variable first (e.g., "base.url" -> "BASE_URL")
        String envKey = key.replace(".", "_").toUpperCase();
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        // Check system property (e.g., -Dbase.url=...)
        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue;
        }

        // Fall back to properties file
        return PROPS.getProperty(key);
    }

    // PUBLIC_INTERFACE
    /**
     * Gets a configuration property with a default value.
     *
     * @param key          the property key
     * @param defaultValue the default value if property is not found
     * @return the property value, or defaultValue if not found
     */
    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    // PUBLIC_INTERFACE
    /**
     * Gets an integer configuration property with a default value.
     *
     * @param key          the property key
     * @param defaultValue the default value if property is not found or invalid
     * @return the integer property value, or defaultValue if not found/invalid
     */
    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOG.warn("Invalid integer value '{}' for key '{}'; using default {}", value, key, defaultValue);
            return defaultValue;
        }
    }

    // PUBLIC_INTERFACE
    /**
     * Gets a boolean configuration property with a default value.
     *
     * @param key          the property key
     * @param defaultValue the default value if property is not found
     * @return the boolean property value, or defaultValue if not found
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
