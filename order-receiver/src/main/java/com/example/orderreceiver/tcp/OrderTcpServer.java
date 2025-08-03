package com.example.orderreceiver.tcp;

import com.example.orderreceiver.OrderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderTcpServer {
    private static final Logger logger = LoggerFactory.getLogger(OrderTcpServer.class);

    private final int port;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final OrderManager orderManager;

    private volatile ServerSocket serverSocket;
    private volatile boolean running = false;
    private volatile boolean rejectMode = false; // Default to FILL mode

    public OrderTcpServer(int port) {
        this.port = port;
        this.orderManager = new OrderManager();
    }

    public synchronized void start() throws IOException {
        if (running) {
            logger.info("TCP server already running on {}", port);
            return;
        }

        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));
        running = true;

        logger.info("TCP server STARTED on port {}", port);

        new Thread(this::acceptLoop, "tcp-listener-" + port) {{
            setDaemon(true);
            start();
        }};
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                executorService.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (running) logger.error("Accept error", e);
            }
        }
    }

    private void handleClient(Socket client) {
        try (PrintWriter out = new PrintWriter(client.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            
            logger.info("Order client connected: {}", client.getInetAddress());
            
            // Read order message
            String orderMessage = in.readLine();
            if (orderMessage == null) {
                logger.warn("No order message received from client");
                return;
            }
            
            // Parse and process order
            OrderMessage order = OrderMessage.fromTcpString(orderMessage);
            logger.info("Received order: {} {} {} shares @ ${:.2f}", 
                       order.getSide(), order.getSymbol(), order.getQuantity(), order.getPrice());
            
            // Store the order
            orderManager.addOrder(order.toGrpcOrder());
            
            // Process order based on mode
            String status = rejectMode ? "REJECTED" : "FILLED";
            String message = rejectMode ? "Order rejected by receiver" : "Order filled successfully";
            
            OrderResponse response = new OrderResponse(order.getOrderId(), status, message);
            String responseString = response.toTcpString();
            
            // Store the response
            orderManager.addResponse(order.getOrderId(), response.toGrpcOrderResponse());
            
            // Send response
            out.println(responseString);
            logger.info("Sent response: {} - {}", order.getOrderId(), status);
            
        } catch (Exception ex) {
            logger.error("TCP client handler error", ex);
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    public synchronized void shutdown() {
        if (!running) {
            logger.info("TCP server already stopped on port {}", port);
            return;
        }

        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                logger.info("TCP server socket CLOSED on port {}", port);
            }
        } catch (IOException e) {
            logger.warn("Error closing server socket", e);
        }
        
        executorService.shutdown();
        logger.info("TCP server STOPPED on port {}", port);
    }

    public boolean isRunning() { 
        return running; 
    }
    
    public void setRejectMode(boolean rejectMode) {
        this.rejectMode = rejectMode;
        String mode = rejectMode ? "REJECT ALL" : "FILL ALL";
        logger.info("TCP server mode changed to: {}", mode);
    }
    
    public OrderManager getOrderManager() {
        return orderManager;
    }
    
    public static class OrderMessage {
        private final String orderId;
        private final String symbol;
        private final String side;
        private final int quantity;
        private final double price;
        private final String account;
        private final String exchange;
        private final String timestamp;
        
        public OrderMessage(String orderId, String symbol, String side, int quantity, 
                          double price, String account, String exchange, String timestamp) {
            this.orderId = orderId;
            this.symbol = symbol;
            this.side = side;
            this.quantity = quantity;
            this.price = price;
            this.account = account;
            this.exchange = exchange;
            this.timestamp = timestamp;
        }
        
        public static OrderMessage fromTcpString(String tcpString) {
            String[] parts = tcpString.split("\\|");
            if (parts.length >= 8 && parts[0].equals("ORDER")) {
                return new OrderMessage(
                    parts[1], // orderId
                    parts[2], // symbol
                    parts[3], // side
                    Integer.parseInt(parts[4]), // quantity
                    Double.parseDouble(parts[5]), // price
                    parts[6], // account
                    parts[7], // exchange
                    parts[8]  // timestamp
                );
            } else {
                throw new IllegalArgumentException("Invalid order message format: " + tcpString);
            }
        }
        
        public com.example.orderreceiver.grpc.Order toGrpcOrder() {
            return com.example.orderreceiver.grpc.Order.newBuilder()
                    .setOrderId(orderId)
                    .setSymbol(symbol)
                    .setSide(side)
                    .setOrderQty(quantity)
                    .setPrice(price)
                    .setAccount(account)
                    .setExchange(exchange)
                    .setTransactTime(timestamp)
                    .build();
        }
        
        // Getters
        public String getOrderId() { return orderId; }
        public String getSymbol() { return symbol; }
        public String getSide() { return side; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public String getAccount() { return account; }
        public String getExchange() { return exchange; }
        public String getTimestamp() { return timestamp; }
    }
    
    public static class OrderResponse {
        private final String orderId;
        private final String status;
        private final String message;
        
        public OrderResponse(String orderId, String status, String message) {
            this.orderId = orderId;
            this.status = status;
            this.message = message;
        }
        
        public String toTcpString() {
            return String.format("%s|%s|%s", orderId, status, message);
        }
        
        public com.example.orderreceiver.grpc.OrderResponse toGrpcOrderResponse() {
            return com.example.orderreceiver.grpc.OrderResponse.newBuilder()
                    .setOrderId(orderId)
                    .setStatus(status)
                    .setMessage(message)
                    .build();
        }
        
        public String getOrderId() { return orderId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }
} 