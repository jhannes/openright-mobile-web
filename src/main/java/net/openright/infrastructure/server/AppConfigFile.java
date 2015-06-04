package net.openright.infrastructure.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfigFile {
    private static Logger log = LoggerFactory.getLogger(AppConfigFile.class);

    private long nextCheckTime = 0;
    private long lastLoadTime = 0;
    private Properties properties = new Properties();
    private final File configFile;

    public AppConfigFile(File configFile) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        this.configFile = configFile;
    }

    public String getProperty(String propertyName, String defaultValue) {
        String result = getProperty(propertyName);
        if (result == null) {
            log.trace("Missing property {} in {}", propertyName, properties.keySet());
            return defaultValue;
        }
        return result;
    }

    public String getRequiredProperty(String propertyName) {
        String result = getProperty(propertyName);
        if (result == null) {
            throw new RuntimeException("Missing property " + propertyName + " in " + configFile);
        }
        return result;
    }

    private String getProperty(String propertyName) {
        if (System.getProperty(propertyName) != null) {
            log.trace("Reading {} from system properties", propertyName);
            return System.getProperty(propertyName);
        }
        if (System.getenv(propertyName.replace('.', '_')) != null) {
            log.trace("Reading {} from environment", propertyName);
            return System.getenv(propertyName.replace('.', '_'));
        }

        ensureConfigurationIsFresh();
        return properties.getProperty(propertyName);
    }

    private synchronized void ensureConfigurationIsFresh() {
        if (System.currentTimeMillis() < nextCheckTime) return;
        nextCheckTime = System.currentTimeMillis() + 10000;
        log.trace("Rechecking {}", configFile);

        if (!configFile.exists()) {
            log.error("Missing configuration file {}", configFile);
        }

        if (lastLoadTime >= configFile.lastModified()) return;
        lastLoadTime = configFile.lastModified();
        log.debug("Reloading {}", configFile);

        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            properties.clear();
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + configFile, e);
        }
    }
}
