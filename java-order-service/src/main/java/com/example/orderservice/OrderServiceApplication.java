package com.example.orderservice;

import com.example.orderservice.consul.ConsulRegistration;
import com.example.orderservice.grpc.GrpcServer;
import com.example.orderservice.tcp.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderServiceApplication {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceApplication.class);
    
    private final GrpcServer grpcServer;
    private final TcpServer tcpServer;
    private final ConsulRegistration consulRegistration;
    private final OrderManager orderManager;
    private final ScheduledExecutorService scheduler;
    
    public OrderServiceApplication() {
        // Print configuration at startup
        ServiceConfig.printConfiguration();
        
        this.orderManager = new OrderManager();
        this.grpcServer = new GrpcServer(ServiceConfig.GRPC_PORT, orderManager);
        this.tcpServer = new TcpServer(ServiceConfig.TCP_PORT, orderManager);
        this.consulRegistration = new ConsulRegistration(
            ServiceConfig.SERVICE_NAME, 
            ServiceConfig.SERVICE_ID, 
            ServiceConfig.TCP_PORT, 
            ServiceConfig.GRPC_PORT
        );
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public void start() {
        try {
            logger.info("Starting Order Service (ID: {})...", ServiceConfig.SERVICE_ID);
            
            // Start TCP server
            tcpServer.start();
            logger.info("TCP server started on port {}", ServiceConfig.TCP_PORT);
            
            // Start gRPC server
            grpcServer.start();
            logger.info("gRPC server started on port {}", ServiceConfig.GRPC_PORT);
            
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
            grpcServer.shutdown();
            tcpServer.shutdown();
            logger.info("Order Service shutdown complete");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
    
    public static void main(String[] args) {
        OrderServiceApplication app = new OrderServiceApplication();
        app.run();
    }
} 