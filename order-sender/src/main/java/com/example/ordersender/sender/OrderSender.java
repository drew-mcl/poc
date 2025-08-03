package com.example.ordersender.sender;

import com.example.ordersender.OrderManager;
import com.example.ordersender.SenderConfig;
import com.example.ordersender.consul.ConsulServiceDiscovery.ReceiverInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderSender {
    private static final Logger logger = LoggerFactory.getLogger(OrderSender.class);
    
    private final Random random = new Random();
    private final AtomicInteger orderCounter = new AtomicInteger(1);
    private final OrderManager orderManager;
    
    // Stock data for mock orders
    private static final String[] STOCK_SYMBOLS = {
        "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "ADBE", "CRM",
        "ORCL", "INTC", "AMD", "QCOM", "AVGO", "TXN", "MU", "ADI", "KLAC", "LRCX"
    };
    
    private static final String[] ACCOUNTS = {"TRADER001", "TRADER002", "TRADER003", "TRADER004", "TRADER005"};
    private static final String[] EXCHANGES = {"NASDAQ", "NYSE", "ARCA", "BATS", "EDGX"};
    
    public OrderSender(OrderManager orderManager) {
        this.orderManager = orderManager;
    }
    
    public OrderResponse sendOrder(ReceiverInfo receiver) {
        String orderId = "SENDER-" + String.format("%06d", orderCounter.getAndIncrement());
        
        // Generate mock order data
        String symbol = STOCK_SYMBOLS[random.nextInt(STOCK_SYMBOLS.length)];
        String side = random.nextBoolean() ? "BUY" : "SELL";
        int quantity = random.nextInt(1000) + 100; // 100-1100 shares
        double price = (random.nextDouble() * 500) + 50; // $50-$550
        String account = ACCOUNTS[random.nextInt(ACCOUNTS.length)];
        String exchange = EXCHANGES[random.nextInt(EXCHANGES.length)];
        
        // Create order message
        OrderMessage orderMessage = new OrderMessage(
            orderId,
            symbol,
            side,
            quantity,
            price,
            account,
            exchange,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"))
        );
        
        logger.info("Sending order to {}: {} {} {} shares @ ${:.2f}", 
                   receiver.getServiceId(), side, symbol, quantity, price);
        
        // Send via TCP
        try (Socket socket = new Socket(receiver.getAddress(), receiver.getPort())) {
            socket.setSoTimeout(SenderConfig.TCP_TIMEOUT_MS);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send order
            String orderString = orderMessage.toTcpString();
            out.println(orderString);
            
            // Read response
            String response = in.readLine();
            if (response == null) {
                throw new IOException("No response received from receiver");
            }
            
            // Parse response
            OrderResponse orderResponse = OrderResponse.fromTcpString(response);
            
            // Store response in OrderManager with receiver ID
            orderManager.addOrderResponse(orderId, orderResponse.toGrpcOrderResponse(receiver.getServiceId()));
            
            logger.info("Received response from {}: {}", receiver.getServiceId(), orderResponse);
            
            return orderResponse;
            
        } catch (IOException e) {
            logger.error("Failed to send order to {}: {}", receiver.getServiceId(), e.getMessage());
            OrderResponse errorResponse = new OrderResponse(orderId, "ERROR", "Failed to send order: " + e.getMessage());
            orderManager.addOrderResponse(orderId, errorResponse.toGrpcOrderResponse(receiver.getServiceId()));
            return errorResponse;
        }
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
        
        public String toTcpString() {
            // Simple pipe-delimited format for TCP communication
            return String.format("ORDER|%s|%s|%s|%d|%.2f|%s|%s|%s", 
                orderId, symbol, side, quantity, price, account, exchange, timestamp);
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
        
        public static OrderResponse fromTcpString(String tcpString) {
            String[] parts = tcpString.split("\\|");
            if (parts.length >= 3) {
                return new OrderResponse(parts[0], parts[1], parts[2]);
            } else {
                return new OrderResponse("UNKNOWN", "ERROR", "Invalid response format: " + tcpString);
            }
        }
        
        public com.example.ordersender.grpc.OrderResponse toGrpcOrderResponse() {
            return toGrpcOrderResponse("unknown");
        }
        
        public com.example.ordersender.grpc.OrderResponse toGrpcOrderResponse(String receiverId) {
            return com.example.ordersender.grpc.OrderResponse.newBuilder()
                    .setOrderId(orderId)
                    .setStatus(status)
                    .setMessage(message)
                    .setReceiverId(receiverId)
                    .build();
        }
        
        public String getOrderId() { return orderId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("OrderResponse{orderId='%s', status='%s', message='%s'}", 
                orderId, status, message);
        }
    }
} 