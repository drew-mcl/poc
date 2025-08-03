package com.example.orderreceiver;

import com.example.orderreceiver.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderManager {
    private static final Logger logger = LoggerFactory.getLogger(OrderManager.class);
    
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, OrderResponse> responses = new ConcurrentHashMap<>();
    
    public void addOrder(Order order) {
        orders.put(order.getOrderId(), order);
        logger.info("Added order: {}", order.getOrderId());
    }
    
    public void addResponse(String orderId, OrderResponse response) {
        responses.put(orderId, response);
        logger.info("Added response for order {}: {}", orderId, response.getStatus());
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
                    .setMessage("Order retrieved successfully")
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error getting order", e);
            return GetOrderResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to get order: " + e.getMessage())
                    .build();
        }
    }
    
    public ListAllOrderIdsResponse listAllOrderIds(ListAllOrderIdsRequest request) {
        try {
            List<String> orderIds = new ArrayList<>(orders.keySet());
            
            return ListAllOrderIdsResponse.newBuilder()
                    .addAllOrderIds(orderIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing all order IDs", e);
            return ListAllOrderIdsResponse.newBuilder().build();
        }
    }
    
    public ListRejectedOrderIdsResponse listRejectedOrderIds(ListRejectedOrderIdsRequest request) {
        try {
            List<String> rejectedOrderIds = responses.values().stream()
                    .filter(response -> response.getStatus().equals("REJECTED"))
                    .map(OrderResponse::getOrderId)
                    .collect(Collectors.toList());
            
            return ListRejectedOrderIdsResponse.newBuilder()
                    .addAllOrderIds(rejectedOrderIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing rejected order IDs", e);
            return ListRejectedOrderIdsResponse.newBuilder().build();
        }
    }
    
    public ListFilledOrderIdsResponse listFilledOrderIds(ListFilledOrderIdsRequest request) {
        try {
            List<String> filledOrderIds = responses.values().stream()
                    .filter(response -> response.getStatus().equals("FILLED"))
                    .map(OrderResponse::getOrderId)
                    .collect(Collectors.toList());
            
            return ListFilledOrderIdsResponse.newBuilder()
                    .addAllOrderIds(filledOrderIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing filled order IDs", e);
            return ListFilledOrderIdsResponse.newBuilder().build();
        }
    }
    
    public ServiceInfoResponse getServiceInfo(ServiceInfoRequest request) {
        return ServiceInfoResponse.newBuilder()
                .setServiceName("Order Receiver")
                .setVersion("1.0.0")
                .setStatus("RUNNING")
                .setOrderCount(orders.size())
                .setResponseCount(responses.size())
                .addAllAvailableMethods(Arrays.asList(
                    "GetServiceInfo",
                    "RejectAllOrders",
                    "AcceptAllOrders",
                    "GetOrder",
                    "ListAllOrderIds",
                    "ListRejectedOrderIds",
                    "ListFilledOrderIds"
                ))
                .build();
    }
    
    public AdminActionResponse rejectAllOrders() {
        logger.warn(">>> Order processing is now set to REJECT ALL NEW ORDERS <<<");
        return AdminActionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Service will now reject all new incoming orders.")
                .build();
    }

    public AdminActionResponse acceptAllOrders() {
        logger.info(">>> Order processing is now set to ACCEPT ALL NEW ORDERS <<<");
        return AdminActionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Service will now accept new incoming orders.")
                .build();
    }
    
    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }
    
    public List<OrderResponse> getAllResponses() {
        return new ArrayList<>(responses.values());
    }
    
    public int getOrderCount() {
        return orders.size();
    }
    
    public int getResponseCount() {
        return responses.size();
    }
} 