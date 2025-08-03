package com.example.orderservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServiceConfig.class);
    
    // Service Configuration
    public static final String SERVICE_NAME = getEnv("SERVICE_NAME", "order-service");
    public static final int ORDINAL = getEnvAsInt("ORDINAL", 1);
    public static final String SERVICE_ID = SERVICE_NAME + "-" + ORDINAL;
    
    // Port Configuration - Simplified to just admin and FIX
    public static final int ADMIN_PORT = getEnvAsInt("ADMIN_PORT", 9090 + (ORDINAL - 1));
    public static final int FIX_PORT = getEnvAsInt("FIX_PORT", 8080 + (ORDINAL - 1));
    
    // Consul Configuration
    public static final String CONSUL_HOST = getEnv("CONSUL_HOST", "localhost");
    public static final int CONSUL_PORT = getEnvAsInt("CONSUL_PORT", 8500);
    public static final String CONSUL_ADDRESS = CONSUL_HOST + ":" + CONSUL_PORT;
    
    // FIX Configuration
    public static final String FIX_CLIENT_ID = getEnv("FIX_CLIENT_ID", "ORDER_SERVICE_" + String.format("%03d", ORDINAL));
    public static final String FIX_TARGET_COMP_ID = getEnv("FIX_TARGET_COMP_ID", "EXCHANGE");
    public static final String FIX_SENDER_COMP_ID = getEnv("FIX_SENDER_COMP_ID", "ORDER_SERVICE");
    public static final int FIX_HEARTBEAT_INTERVAL = getEnvAsInt("FIX_HEARTBEAT_INTERVAL", 30);
    public static final String FIX_VERSION = getEnv("FIX_VERSION", "FIX.4.4");
    
    // Mock Order Generation
    public static final int MOCK_ORDER_INITIAL_DELAY = getEnvAsInt("MOCK_ORDER_INITIAL_DELAY", 5);
    public static final int MOCK_ORDER_INTERVAL = getEnvAsInt("MOCK_ORDER_INTERVAL", 10);
    public static final boolean MOCK_ORDER_ENABLED = getEnvAsBoolean("MOCK_ORDER_ENABLED", true);
    
    // Service Tags
    public static final String[] SERVICE_TAGS = getEnv("SERVICE_TAGS", "java,order-service,admin,fix").split(",");
    
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
        logger.info("=== Service Configuration ===");
        logger.info("Service Name: {}", SERVICE_NAME);
        logger.info("Ordinal: {}", ORDINAL);
        logger.info("Service ID: {}", SERVICE_ID);
        logger.info("Admin Port: {}", ADMIN_PORT);
        logger.info("FIX Port: {}", FIX_PORT);
        logger.info("Consul Address: {}", CONSUL_ADDRESS);
        logger.info("FIX Client ID: {}", FIX_CLIENT_ID);
        logger.info("FIX Target Comp ID: {}", FIX_TARGET_COMP_ID);
        logger.info("FIX Sender Comp ID: {}", FIX_SENDER_COMP_ID);
        logger.info("FIX Heartbeat Interval: {}", FIX_HEARTBEAT_INTERVAL);
        logger.info("FIX Version: {}", FIX_VERSION);
        logger.info("Mock Order Enabled: {}", MOCK_ORDER_ENABLED);
        logger.info("Mock Order Initial Delay: {}", MOCK_ORDER_INITIAL_DELAY);
        logger.info("Mock Order Interval: {}", MOCK_ORDER_INTERVAL);
        logger.info("Service Tags: {}", String.join(", ", SERVICE_TAGS));
        logger.info("=============================");
    }
} 