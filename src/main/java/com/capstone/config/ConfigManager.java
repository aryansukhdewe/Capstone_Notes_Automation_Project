package com.capstone.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigManager — Singleton utility for reading configuration properties.
 *
 * Updated to support both config.properties (classpath) AND .env files (project root),
 * as well as OS-level environment variables and System properties.
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    private final Properties properties;
    private static final String CONFIG_FILE = "config.properties";

    // Private constructor — enforces Singleton
    private ConfigManager() {
        properties = new Properties();
        loadProperties();
        loadEnvFile(); // Automatically pull in .env variables
    }

    /**
     * Double-checked locking for thread-safe lazy initialization.
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
                log.info("Configuration loaded successfully from {}", CONFIG_FILE);
            } else {
                log.warn("{} not found on classpath. Relying on .env or System properties.", CONFIG_FILE);
            }
        } catch (IOException e) {
            log.error("Failed to load config.properties: {}", e.getMessage());
        }
    }

    /**
     * Reads the .env file from the project root directory if it exists.
     */
    private void loadEnvFile() {
        File envFile = new File(".env");
        if (envFile.exists()) {
            try (FileInputStream fis = new FileInputStream(envFile)) {
                properties.load(fis);
                log.info("Environment variables loaded successfully from .env file");
            } catch (IOException e) {
                log.error("Found .env file but could not read it: {}", e.getMessage());
            }
        } else {
            log.debug("No .env file found at project root.");
        }
    }

    /**
     * Gets a property value with a strict fallback hierarchy:
     * 1. System Properties (-D command line args)
     * 2. OS Environment Variables (handles format matching like GEMINI_API_KEY vs gemini.api.key)
     * 3. Loaded properties (from .env or config.properties)
     */
    public String get(String key) {
        // 1. Check System Property (-D flag overrides)
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            log.debug("Using system property override for key '{}'", key);
            return value.trim();
        }

        // 2. Check OS Environment Variables
        // Automatically checks uppercase format (e.g., gemini.api.key -> GEMINI_API_KEY)
        String envKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(envKey);
        if (value == null) {
            value = System.getenv(key); // fallback to exact string match
        }
        if (value != null && !value.isBlank()) {
            log.debug("Using OS environment variable for key '{}'", key);
            return value.trim();
        }

        // 3. Check files (.env and config.properties)
        value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Missing required config key: '" + key + "'. " +
                    "Ensure it exists in config.properties, the .env file, or is passed as an environment variable.");
        }
        return value.trim();
    }

    public String get(String key, String defaultValue) {
        try {
            return get(key);
        } catch (RuntimeException e) {
            log.debug("Key '{}' not found, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    // Alias for get() to map correctly to GeminiApiClient snippet
    public String getString(String key) {
        return get(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    // Convenience shorthand methods
    public String getBaseUrl()    { return get("base.url"); }
    public String getApiBaseUrl() { return get("api.base.url"); }
    public String getBrowser()    { return get("browser", "chrome"); }
    public boolean isHeadless()   { return getBoolean("headless"); }
    public int getTimeout()       { return getInt("browser.timeout"); }
    public int getApiTimeout()    { return getInt("api.response.time.ms"); }
    public String getUserEmail()  { return get("user.email"); }
    public String getUserPassword(){ return get("user.password"); }
}