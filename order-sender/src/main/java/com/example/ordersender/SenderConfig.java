package com.example.ordersender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenderConfig {
    private static final Logger logger = LoggerFactory.getLogger(SenderConfig.class);
    
    // Service Configuration
    public static final String SENDER_ID = getEnv("SENDER_ID", "order-sender-1");
    public static final String SERVICE_NAME = getEnv("SERVICE_NAME", "order-sender");
    public static final String SERVICE_ADDRESS = getEnv("SERVICE_ADDRESS", "localhost");
    
    // Port Configuration
    public static final int ADMIN_PORT = getEnvAsInt("ADMIN_PORT", 9200);
    
    // Consul Configuration
    public static final String CONSUL_HOST = getEnv("CONSUL_HOST", "localhost");
    public static final int CONSUL_PORT = getEnvAsInt("CONSUL_PORT", 8500);
    public static final String CONSUL_ADDRESS = CONSUL_HOST + ":" + CONSUL_PORT;
    
    // Receiver Service Discovery
    public static final String RECEIVER_SERVICE_NAME = getEnv("RECEIVER_SERVICE_NAME", "order-receiver");
    
    // Mock Order Generation
    public static final int MOCK_ORDER_INITIAL_DELAY = getEnvAsInt("MOCK_ORDER_INITIAL_DELAY", 5);
    public static final int MOCK_ORDER_INTERVAL = getEnvAsInt("MOCK_ORDER_INTERVAL", 15);
    public static final boolean MOCK_ORDER_ENABLED = getEnvAsBoolean("MOCK_ORDER_ENABLED", true);
    
    // TCP Configuration
    public static final int TCP_TIMEOUT_MS = getEnvAsInt("TCP_TIMEOUT_MS", 5000);
    public static final int TCP_RETRY_ATTEMPTS = getEnvAsInt("TCP_RETRY_ATTEMPTS", 3);
    
    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            logger.info("Using default value for {}: {}", key, defaultValue);
            return defaultValue;
        }
        logger.info("Using environment value for {}: {}", key, value);
        return value;
    }
    
    private static int getEnvAsInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            logger.info("Using default value for {}: {}", key, defaultValue);
            return defaultValue;
        }
        try {
            int intValue = Integer.parseInt(value);
            logger.info("Using environment value for {}: {}", key, intValue);
            return intValue;
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for {}: {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    private static boolean getEnvAsBoolean(String key, boolean defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            logger.info("Using default value for {}: {}", key, defaultValue);
            return defaultValue;
        }
        boolean boolValue = Boolean.parseBoolean(value);
        logger.info("Using environment value for {}: {}", key, boolValue);
        return boolValue;
    }
    
    public static void printConfiguration() {
        logger.info("=== Order Sender Configuration ===");
        logger.info("Sender ID: {}", SENDER_ID);
        logger.info("Service Name: {}", SERVICE_NAME);
        logger.info("Service Address: {}", SERVICE_ADDRESS);
        logger.info("Admin Port: {}", ADMIN_PORT);
        logger.info("Consul Address: {}", CONSUL_ADDRESS);
        logger.info("Receiver Service Name: {}", RECEIVER_SERVICE_NAME);
        logger.info("Mock Order Enabled: {}", MOCK_ORDER_ENABLED);
        logger.info("Mock Order Initial Delay: {}", MOCK_ORDER_INITIAL_DELAY);
        logger.info("Mock Order Interval: {}", MOCK_ORDER_INTERVAL);
        logger.info("TCP Timeout: {}ms", TCP_TIMEOUT_MS);
        logger.info("TCP Retry Attempts: {}", TCP_RETRY_ATTEMPTS);
        logger.info("================================");
    }
} 