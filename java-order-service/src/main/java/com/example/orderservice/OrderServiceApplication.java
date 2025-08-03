package com.example.orderservice;

import com.example.orderservice.consul.ConsulRegistration;
import com.example.orderservice.admin.AdminServer;
import com.example.orderservice.fix.FixServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderServiceApplication {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceApplication.class);
    
    private final AdminServer adminServer;
    private final FixServer fixServer;
    private final ConsulRegistration consulRegistration;
    private final OrderManager orderManager;
    private final ScheduledExecutorService scheduler;
    
    public OrderServiceApplication() {
        // Print configuration at startup
        ServiceConfig.printConfiguration();
        
        // Create OrderManager first (without server dependencies)
        this.orderManager = new OrderManager();
        
        // Create servers with OrderManager reference
        this.fixServer = new FixServer(ServiceConfig.FIX_PORT, this.orderManager);
        this.adminServer = new AdminServer(ServiceConfig.ADMIN_PORT, this.orderManager);
        
        // Set the application reference in OrderManager for server control
        this.orderManager.setApplication(this);
        
        this.consulRegistration = new ConsulRegistration(
            ServiceConfig.SERVICE_NAME, 
            ServiceConfig.SERVICE_ID, 
            ServiceConfig.FIX_PORT, 
            ServiceConfig.ADMIN_PORT
        );
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public void start() {
        try {
            logger.info("Starting Order Service (ID: {})...", ServiceConfig.SERVICE_ID);
            
            // Start FIX server
            fixServer.start();
            logger.info("FIX server started on port {}", ServiceConfig.FIX_PORT);
            
            // Start admin server
            adminServer.start();
            logger.info("Admin server started on port {}", ServiceConfig.ADMIN_PORT);
            
            // Register with Consul
            consulRegistration.register();
            logger.info("Registered with Consul");
            
            // Start mock order generation if enabled
            if (ServiceConfig.MOCK_ORDER_ENABLED) {
                startMockOrderGeneration();
            }
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            
            logger.info("Order Service started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start Order Service", e);
            System.exit(1);
        }
    }
    
    public void run() {
        start();
        
        // Keep the main thread alive
        try {
            logger.info("Order Service is running. Press Ctrl+C to stop.");
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.info("Order Service interrupted");
        } finally {
            shutdown();
        }
    }
    
    private void startMockOrderGeneration() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                orderManager.generateMockOrder();
                logger.debug("Generated mock order");
            } catch (Exception e) {
                logger.error("Error generating mock order", e);
            }
        }, ServiceConfig.MOCK_ORDER_INITIAL_DELAY, ServiceConfig.MOCK_ORDER_INTERVAL, TimeUnit.SECONDS);
    }
    
    public void shutdown() {
        logger.info("Shutting down Order Service...");
        
        try {
            scheduler.shutdown();
            consulRegistration.deregister();
            adminServer.shutdown();
            fixServer.shutdown();
            logger.info("Order Service shutdown complete");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
    
    // Method to toggle the FIX server
    public void toggleFixServer(boolean enable) {
        try {
            if (enable && !fixServer.isRunning()) {
                fixServer.start();
                logger.info(">>> FIX server ENABLED via admin command <<<");
            } else if (!enable && fixServer.isRunning()) {
                fixServer.forceShutdown();
                logger.info(">>> FIX server DISABLED via admin command <<<");
            }
        } catch (Exception e) {
            logger.error("Error toggling FIX server", e);
        }
    }

    // Method to toggle the Admin server
    public void toggleAdminServer(boolean enable) {
        try {
            if (enable) {
                // Restarting a gRPC server is more complex, this is a simplified example
                logger.info(">>> Enabling Admin server is not dynamically supported in this example. <<<");
            } else {
                adminServer.forceShutdown();
                logger.info(">>> Admin server DISABLED via admin command <<<");
            }
        } catch (Exception e) {
            logger.error("Error toggling Admin server", e);
        }
    }
    
    // Method to check if TCP server is running
    public boolean isTcpServerRunning() {
        return fixServer.isRunning();
    }
    
    public static void main(String[] args) {
        OrderServiceApplication app = new OrderServiceApplication();
        app.run();
    }
} 