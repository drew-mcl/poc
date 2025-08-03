package com.example.orderservice;

import com.example.orderservice.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class OrderManager {
    private static final Logger logger = LoggerFactory.getLogger(OrderManager.class);
    
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger orderCounter = new AtomicInteger(1);
    private final Random random = new Random();
    private volatile boolean rejectAllNewOrders = false;
    
    // FIX Stock Order Data
    private static final String[] STOCK_SYMBOLS = {
        "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "ADBE", "CRM",
        "ORCL", "INTC", "AMD", "QCOM", "AVGO", "TXN", "MU", "ADI", "KLAC", "LRCX",
        "ASML", "TSM", "AMAT", "QCOM", "MRVL", "SWKS", "MCHP", "TER", "ENTG", "COHR"
    };
    
    private static final String[] RICS = {
        "AAPL.O", "MSFT.O", "GOOGL.O", "AMZN.O", "TSLA.O", "META.O", "NVDA.O", "NFLX.O", "ADBE.O", "CRM.O",
        "ORCL.O", "INTC.O", "AMD.O", "QCOM.O", "AVGO.O", "TXN.O", "MU.O", "ADI.O", "KLAC.O", "LRCX.O",
        "ASML.O", "TSM.O", "AMAT.O", "QCOM.O", "MRVL.O", "SWKS.O", "MCHP.O", "TER.O", "ENTG.O", "COHR.O"
    };
    
    private static final String[] ACCOUNTS = {"TRADER001", "TRADER002", "TRADER003", "TRADER004", "TRADER005"};
    private static final String[] EXCHANGES = {"NASDAQ", "NYSE", "ARCA", "BATS", "EDGX"};
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "JPY", "CAD"};
    
    // FIX Configuration
    private final FixConfig fixConfig;
    
    public OrderManager() {
        // Initialize FIX configuration
        this.fixConfig = FixConfig.newBuilder()
                .setSocketHost("localhost")
                .setSocketPort(5001)
                .setClientId("ORDER_SERVICE_001")
                .setTargetCompId("EXCHANGE")
                .setSenderCompId("ORDER_SERVICE")
                .setHeartbeatInterval(30)
                .setFixVersion("FIX.4.4")
                .build();
        
        // Generate 10 initial mock FIX orders
        for (int i = 0; i < 10; i++) {
            generateMockFixOrder();
        }
    }
    
    public void generateMockOrder() {
        // Legacy method - now calls generateMockFixOrder
        generateMockFixOrder();
    }
    
    public void generateMockFixOrder() {
        if (this.rejectAllNewOrders) {
            logger.error("REJECTED mock FIX order generation because 'reject all' mode is active.");
            return;
        }
        
        String orderId = "ORDER-" + String.format("%06d", orderCounter.getAndIncrement());
        
        // Random stock data
        int symbolIndex = random.nextInt(STOCK_SYMBOLS.length);
        String symbol = STOCK_SYMBOLS[symbolIndex];
        String ric = RICS[symbolIndex];
        String side = random.nextBoolean() ? "1" : "2"; // 1=Buy, 2=Sell
        int orderQty = random.nextInt(1000) + 100; // 100-1100 shares
        double price = (random.nextDouble() * 500) + 50; // $50-$550
        String ordType = random.nextBoolean() ? "1" : "2"; // 1=Market, 2=Limit
        String timeInForce = random.nextBoolean() ? "0" : "1"; // 0=Day, 1=GTC
        String account = ACCOUNTS[random.nextInt(ACCOUNTS.length)];
        String exchange = EXCHANGES[random.nextInt(EXCHANGES.length)];
        String currency = CURRENCIES[random.nextInt(CURRENCIES.length)];
        
        // Generate FIX message using orderId as ClOrdID
        String fixMessage = generateFixMessage(orderId, symbol, side, orderQty, price, ordType, timeInForce, account, exchange, currency);
        
        Order order = Order.newBuilder()
                .setOrderId(orderId)
                .setSymbol(symbol)
                .setRic(ric)
                .setSide(side)
                .setOrderQty(orderQty)
                .setPrice(price)
                .setOrdType(ordType)
                .setTimeInForce(timeInForce)
                .setAccount(account)
                .setSecurityType("CS") // Common Stock
                .setCurrency(currency)
                .setExchange(exchange)
                .setTransactTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS")))
                .setStatus(OrderStatus.PENDING)
                .setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .setFixMessage(fixMessage)
                .build();
        
        orders.put(orderId, order);
        logger.info("Generated mock FIX order: {} - {} {} {} shares @ ${:.2f}", 
                   orderId, side.equals("1") ? "BUY" : "SELL", symbol, orderQty, price);
    }
    
    private String generateFixMessage(String clOrdId, String symbol, String side, int orderQty, 
                                   double price, String ordType, String timeInForce, 
                                   String account, String exchange, String currency) {
        // Simple FIX message generation (8=FIX.4.4|9=length|35=D|...|10=checksum)
        StringBuilder fix = new StringBuilder();
        fix.append("8=FIX.4.4|");
        fix.append("35=D|"); // New Order Single
        fix.append("11=").append(clOrdId).append("|");
        fix.append("21=1|"); // HandlInst (1=Automated)
        fix.append("55=").append(symbol).append("|");
        fix.append("54=").append(side).append("|");
        fix.append("60=").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"))).append("|");
        fix.append("38=").append(orderQty).append("|");
        fix.append("40=").append(ordType).append("|");
        fix.append("59=").append(timeInForce).append("|");
        fix.append("1=").append(account).append("|");
        fix.append("15=").append(currency).append("|");
        fix.append("207=").append(exchange).append("|");
        
        if (ordType.equals("2")) { // Limit order
            fix.append("44=").append(String.format("%.2f", price)).append("|");
        }
        
        // Calculate message length and checksum (simplified)
        String body = fix.toString();
        int length = body.length();
        fix.insert(0, "9=" + length + "|");
        
        // Simple checksum calculation
        int checksum = 0;
        for (char c : body.toCharArray()) {
            checksum += c;
        }
        checksum = checksum % 256;
        fix.append("10=").append(String.format("%03d", checksum)).append("|");
        
        return fix.toString();
    }
    
    public AddOrderResponse addOrder(AddOrderRequest request) {
        try {
            String orderId = "ORDER-" + String.format("%06d", orderCounter.getAndIncrement());
            
            // Generate FIX message using orderId as ClOrdID
            String fixMessage = generateFixMessage(
                orderId, 
                request.getSymbol(), 
                request.getSide(), 
                request.getOrderQty(), 
                request.getPrice(), 
                request.getOrdType(), 
                request.getTimeInForce(), 
                request.getAccount(), 
                request.getExchange(), 
                request.getCurrency()
            );
            
            Order order = Order.newBuilder()
                    .setOrderId(orderId)
                    .setSymbol(request.getSymbol())
                    .setRic(request.getRic())
                    .setSide(request.getSide())
                    .setOrderQty(request.getOrderQty())
                    .setPrice(request.getPrice())
                    .setOrdType(request.getOrdType())
                    .setTimeInForce(request.getTimeInForce())
                    .setAccount(request.getAccount())
                    .setSecurityType(request.getSecurityType().isEmpty() ? "CS" : request.getSecurityType())
                    .setCurrency(request.getCurrency().isEmpty() ? "USD" : request.getCurrency())
                    .setExchange(request.getExchange().isEmpty() ? "NASDAQ" : request.getExchange())
                    .setTransactTime(request.getTransactTime().isEmpty() ? 
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS")) : request.getTransactTime())
                    .setStatus(OrderStatus.PENDING)
                    .setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .setFixMessage(fixMessage)
                    .build();
            
            orders.put(orderId, order);
            logger.info("Added new FIX order: {} - {} {} {} shares @ ${:.2f}", 
                       orderId, request.getSide().equals("1") ? "BUY" : "SELL", 
                       request.getSymbol(), request.getOrderQty(), request.getPrice());
            
            return AddOrderResponse.newBuilder()
                    .setSuccess(true)
                    .setOrderId(orderId)
                    .setMessage("FIX order created successfully")
                    .setFixMessage(fixMessage)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error adding FIX order", e);
            return AddOrderResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to create FIX order: " + e.getMessage())
                    .build();
        }
    }
    
    public CancelOrderResponse cancelOrder(CancelOrderRequest request) {
        try {
            Order order = orders.get(request.getOrderId());
            
            if (order == null) {
                return CancelOrderResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Order not found: " + request.getOrderId())
                        .build();
            }
            
            // Generate FIX cancellation message using orderId as ClOrdID
            String cancelFixMessage = generateCancelFixMessage(
                order.getOrderId(),
                order.getSymbol(),
                order.getSide(),
                order.getOrderQty()
            );
            
            Order updatedOrder = order.toBuilder()
                    .setStatus(OrderStatus.CANCELLED)
                    .setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
            
            orders.put(order.getOrderId(), updatedOrder);
            logger.info("Cancelled FIX order: {}", order.getOrderId());
            
            return CancelOrderResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("FIX order cancelled successfully")
                    .setFixMessage(cancelFixMessage)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error cancelling FIX order", e);
            return CancelOrderResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to cancel FIX order: " + e.getMessage())
                    .build();
        }
    }
    
    private String generateCancelFixMessage(String clOrdId, String symbol, String side, int orderQty) {
        StringBuilder fix = new StringBuilder();
        fix.append("8=FIX.4.4|");
        fix.append("35=F|"); // Order Cancel Request
        fix.append("11=").append(clOrdId).append("|");
        fix.append("41=").append(clOrdId).append("|"); // Original ClOrdID (same as new for simplicity)
        fix.append("55=").append(symbol).append("|");
        fix.append("54=").append(side).append("|");
        fix.append("60=").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS"))).append("|");
        fix.append("38=").append(orderQty).append("|");
        
        // Calculate message length and checksum (simplified)
        String body = fix.toString();
        int length = body.length();
        fix.insert(0, "9=" + length + "|");
        
        int checksum = 0;
        for (char c : body.toCharArray()) {
            checksum += c;
        }
        checksum = checksum % 256;
        fix.append("10=").append(String.format("%03d", checksum)).append("|");
        
        return fix.toString();
    }
    
    public GetOrderResponse getOrder(GetOrderRequest request) {
        try {
            Order order = orders.get(request.getOrderId());
            
            if (order == null) {
                return GetOrderResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Order not found: " + request.getOrderId())
                        .build();
            }
            
            return GetOrderResponse.newBuilder()
                    .setSuccess(true)
                    .setOrder(order)
                    .setMessage("FIX order retrieved successfully")
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error getting FIX order", e);
            return GetOrderResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to get FIX order: " + e.getMessage())
                    .build();
        }
    }
    
    public ListAllOrdersResponse listAllOrders(ListAllOrdersRequest request) {
        try {
            List<String> orderIds = new ArrayList<>(orders.keySet());
            
            return ListAllOrdersResponse.newBuilder()
                    .addAllOrderIds(orderIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing all FIX orders", e);
            return ListAllOrdersResponse.newBuilder().build();
        }
    }
    
    public ListOpenOrdersResponse listOpenOrders(ListOpenOrdersRequest request) {
        try {
            List<String> openOrderIds = orders.values().stream()
                    .filter(order -> order.getStatus() == OrderStatus.PENDING)
                    .map(Order::getOrderId)
                    .collect(Collectors.toList());
            
            return ListOpenOrdersResponse.newBuilder()
                    .addAllOrderIds(openOrderIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing open FIX orders", e);
            return ListOpenOrdersResponse.newBuilder().build();
        }
    }
    
    public ListCancelledOrRejectedOrdersResponse listCancelledOrRejectedOrders(ListCancelledOrRejectedOrdersRequest request) {
        try {
            List<String> cancelledOrRejectedOrderIds = orders.values().stream()
                    .filter(order -> order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED)
                    .map(Order::getOrderId)
                    .collect(Collectors.toList());
            
            return ListCancelledOrRejectedOrdersResponse.newBuilder()
                    .addAllOrderIds(cancelledOrRejectedOrderIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing cancelled or rejected FIX orders", e);
            return ListCancelledOrRejectedOrdersResponse.newBuilder().build();
        }
    }
    
    public ServiceInfoResponse getServiceInfo(ServiceInfoRequest request) {
        return ServiceInfoResponse.newBuilder()
                .setServiceName("FIX Order Service")
                .setVersion("2.0.0")
                .setStatus("RUNNING")
                .setOrderCount(orders.size())
                .addAllAvailableMethods(Arrays.asList(
                    "AddOrder",
                    "CancelOrder", 
                    "GetOrder",
                    "ListAllOrders",
                    "ListOpenOrders",
                    "ListCancelledOrRejectedOrders",
                    "GetServiceInfo",
                    "GetFixConfig"
                ))
                .setFixConfig(fixConfig)
                .build();
    }
    
    public FixConfigResponse getFixConfig(FixConfigRequest request) {
        return FixConfigResponse.newBuilder()
                .setSuccess(true)
                .setConfig(fixConfig)
                .setMessage("FIX configuration retrieved successfully")
                .build();
    }
    
    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }
    
    public int getOrderCount() {
        return orders.size();
    }
    
    // Method to enable "reject all" mode
    public AdminActionResponse rejectAllOrders() {
        this.rejectAllNewOrders = true;
        logger.warn(">>> FIX order processing is now set to REJECT ALL NEW ORDERS <<<");
        return AdminActionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Service will now reject all new incoming FIX orders.")
                .build();
    }

    // Method to disable "reject all" mode
    public AdminActionResponse acceptAllOrders() {
        this.rejectAllNewOrders = false;
        logger.info(">>> FIX order processing is now set to ACCEPT ALL NEW ORDERS <<<");
        return AdminActionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Service will now accept new incoming FIX orders.")
                .build();
    }
}