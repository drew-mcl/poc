package com.example.orderreceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReceiverConfig {
    private static final Logger logger = LoggerFactory.getLogger(ReceiverConfig.class);
    
    // Service Configuration
    public static final String SERVICE_NAME = getEnv("SERVICE_NAME", "order-receiver");
    public static final int ORDINAL = getEnvAsInt("ORDINAL", 1);
    public static final String SERVICE_ID = SERVICE_NAME + "-" + ORDINAL;
    
    // Port Configuration
    public static final int TCP_PORT = getEnvAsInt("TCP_PORT", 9000 + (ORDINAL - 1));
    public static final int ADMIN_PORT = getEnvAsInt("ADMIN_PORT", 9100 + (ORDINAL - 1));
    
    // Consul Configuration
    public static final String CONSUL_HOST = getEnv("CONSUL_HOST", "localhost");
    public static final int CONSUL_PORT = getEnvAsInt("CONSUL_PORT", 8500);
    public static final String CONSUL_ADDRESS = CONSUL_HOST + ":" + CONSUL_PORT;
    
    // Service Tags
    public static final String[] SERVICE_TAGS = getEnv("SERVICE_TAGS", "java,order-receiver,tcp,admin").split(",");
    
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
    
    public static void printConfiguration() {
        logger.info("=== Order Receiver Configuration ===");
        logger.info("Service Name: {}", SERVICE_NAME);
        logger.info("Ordinal: {}", ORDINAL);
        logger.info("Service ID: {}", SERVICE_ID);
        logger.info("TCP Port: {}", TCP_PORT);
        logger.info("Admin Port: {}", ADMIN_PORT);
        logger.info("Consul Address: {}", CONSUL_ADDRESS);
        logger.info("Service Tags: {}", String.join(", ", SERVICE_TAGS));
        logger.info("===================================");
    }
} 