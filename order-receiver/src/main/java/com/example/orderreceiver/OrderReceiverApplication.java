package com.example.orderreceiver;

import com.example.orderreceiver.admin.AdminServer;
import com.example.orderreceiver.consul.ConsulRegistration;
import com.example.orderreceiver.tcp.OrderTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderReceiverApplication {
    private static final Logger logger = LoggerFactory.getLogger(OrderReceiverApplication.class);
    
    private final OrderTcpServer tcpServer;
    private final AdminServer adminServer;
    private final ConsulRegistration consulRegistration;
    
    public OrderReceiverApplication() {
        // Print configuration at startup
        ReceiverConfig.printConfiguration();
        
        this.tcpServer = new OrderTcpServer(ReceiverConfig.TCP_PORT);
        this.adminServer = new AdminServer(ReceiverConfig.ADMIN_PORT, this);
        this.consulRegistration = new ConsulRegistration(
            ReceiverConfig.SERVICE_NAME, 
            ReceiverConfig.SERVICE_ID, 
            ReceiverConfig.TCP_PORT,
            ReceiverConfig.ADMIN_PORT
        );
    }
    
    public void start() {
        try {
            logger.info("Starting Order Receiver (ID: {})...", ReceiverConfig.SERVICE_ID);
            
            // Start TCP server
            tcpServer.start();
            logger.info("TCP server started on port {}", ReceiverConfig.TCP_PORT);
            
            // Start admin server
            adminServer.start();
            logger.info("Admin server started on port {}", ReceiverConfig.ADMIN_PORT);
            
            // Register with Consul
            consulRegistration.register();
            logger.info("Registered with Consul");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            
            logger.info("Order Receiver started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start Order Receiver", e);
            System.exit(1);
        }
    }
    
    public void run() {
        start();
        
        // Keep the main thread alive
        try {
            logger.info("Order Receiver is running. Press Ctrl+C to stop.");
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.info("Order Receiver interrupted");
        } finally {
            shutdown();
        }
    }
    
    public void shutdown() {
        logger.info("Shutting down Order Receiver...");
        
        try {
            consulRegistration.deregister();
            adminServer.shutdown();
            tcpServer.shutdown();
            logger.info("Order Receiver shutdown complete");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
    
    // Method to toggle reject mode
    public void setRejectMode(boolean rejectAll) {
        tcpServer.setRejectMode(rejectAll);
        String mode = rejectAll ? "REJECT ALL" : "FILL ALL";
        logger.info(">>> Order processing mode set to: {} <<<", mode);
    }
    
    // Method to get OrderManager
    public OrderManager getOrderManager() {
        return tcpServer.getOrderManager();
    }
    
    public static void main(String[] args) {
        OrderReceiverApplication app = new OrderReceiverApplication();
        app.run();
    }
} 