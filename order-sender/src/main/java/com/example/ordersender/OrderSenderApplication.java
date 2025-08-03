package com.example.ordersender;

import com.example.ordersender.admin.AdminServer;
import com.example.ordersender.consul.ConsulRegistration;
import com.example.ordersender.consul.ConsulServiceDiscovery;
import com.example.ordersender.sender.OrderSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderSenderApplication {
    private static final Logger logger = LoggerFactory.getLogger(OrderSenderApplication.class);
    
    private final ConsulServiceDiscovery serviceDiscovery;
    private final ConsulRegistration consulRegistration;
    private final OrderSender orderSender;
    private final AdminServer adminServer;
    private final OrderManager orderManager;
    private final ScheduledExecutorService scheduler;
    
    public OrderSenderApplication() {
        // Print configuration at startup
        SenderConfig.printConfiguration();
        
        this.orderManager = new OrderManager();
        this.serviceDiscovery = new ConsulServiceDiscovery(
            SenderConfig.CONSUL_HOST, 
            SenderConfig.CONSUL_PORT,
            SenderConfig.RECEIVER_SERVICE_NAME
        );
        
        this.consulRegistration = new ConsulRegistration(
            SenderConfig.CONSUL_HOST,
            SenderConfig.CONSUL_PORT,
            SenderConfig.SENDER_ID,
            SenderConfig.SERVICE_NAME,
            SenderConfig.SERVICE_ADDRESS,
            SenderConfig.ADMIN_PORT
        );
        
        this.orderSender = new OrderSender(orderManager);
        this.adminServer = new AdminServer(SenderConfig.ADMIN_PORT, this);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public void start() {
        try {
            logger.info("Starting Order Sender (ID: {})...", SenderConfig.SENDER_ID);
            
            // Register with Consul
            consulRegistration.register();
            logger.info("Registered with Consul");
            
            // Start admin server
            adminServer.start();
            logger.info("Admin server started on port {}", SenderConfig.ADMIN_PORT);
            
            // Start mock order sending if enabled
            if (SenderConfig.MOCK_ORDER_ENABLED) {
                startMockOrderSending();
            }
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            
            logger.info("Order Sender started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start Order Sender", e);
            System.exit(1);
        }
    }
    
    public void run() {
        start();
        
        // Keep the main thread alive
        try {
            logger.info("Order Sender is running. Press Ctrl+C to stop.");
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.info("Order Sender interrupted");
        } finally {
            shutdown();
        }
    }
    
    private void startMockOrderSending() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Discover available receivers
                var receivers = serviceDiscovery.discoverReceivers();
                
                if (receivers.isEmpty()) {
                    logger.warn("No receivers found in Consul - skipping order send");
                    return;
                }
                
                logger.info("Found {} receivers, sending mock order", receivers.size());
                
                // Send order to each receiver (excluding excluded ones)
                for (var receiver : receivers) {
                    if (orderManager.isReceiverExcluded(receiver.getServiceId())) {
                        logger.info("Skipping excluded receiver: {}", receiver.getServiceId());
                        continue;
                    }
                    
                    try {
                        var response = orderSender.sendOrder(receiver);
                        logger.info("Order sent to {}: {}", receiver.getServiceId(), response);
                    } catch (Exception e) {
                        logger.error("Failed to send order to {}: {}", receiver.getServiceId(), e.getMessage());
                    }
                }
                
            } catch (Exception e) {
                logger.error("Error in mock order sending cycle", e);
            }
        }, SenderConfig.MOCK_ORDER_INITIAL_DELAY, SenderConfig.MOCK_ORDER_INTERVAL, TimeUnit.SECONDS);
    }
    
    public void shutdown() {
        logger.info("Shutting down Order Sender...");
        
        try {
            scheduler.shutdown();
            adminServer.shutdown();
            consulRegistration.deregister();
            consulRegistration.close();
            logger.info("Order Sender shutdown complete");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
    
    // Method to get OrderManager
    public OrderManager getOrderManager() {
        return orderManager;
    }
    
    public static void main(String[] args) {
        OrderSenderApplication app = new OrderSenderApplication();
        app.run();
    }
} 