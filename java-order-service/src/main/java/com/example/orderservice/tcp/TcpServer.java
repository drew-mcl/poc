package com.example.orderservice.tcp;

import com.example.orderservice.OrderManager;
import com.example.orderservice.grpc.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {
    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
    
    private final int port;
    private final OrderManager orderManager;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    
    public TcpServer(int port, OrderManager orderManager) {
        this.port = port;
        this.orderManager = orderManager;
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newCachedThreadPool();
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        logger.info("TCP server started on port {}", port);
        
        // Start server in a separate thread
        Thread serverThread = new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        logger.error("Error accepting client connection", e);
                    }
                }
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }
    
    private void handleClient(Socket clientSocket) {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            logger.info("Client connected: {}", clientSocket.getInetAddress());
            
            // Send initial order count
            out.println("Connected to Order Service");
            out.println("Total orders: " + orderManager.getOrderCount());
            
            // Send all orders as JSON
            List<Order> orders = orderManager.getAllOrders();
            String ordersJson = objectMapper.writeValueAsString(orders);
            out.println("Orders: " + ordersJson);
            
            // Keep connection alive for a short time to demonstrate
            Thread.sleep(2000);
            
        } catch (Exception e) {
            logger.error("Error handling client", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Error closing client socket", e);
            }
        }
    }
    
    public void shutdown() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing server socket", e);
            }
        }
        executorService.shutdown();
        logger.info("TCP server shutdown complete");
    }
} 