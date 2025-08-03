package com.example.ordersender;

import com.example.ordersender.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderManager {
    private static final Logger logger = LoggerFactory.getLogger(OrderManager.class);
    
    private final Map<String, OrderResponse> orderResponses = new ConcurrentHashMap<>();
    private final Set<String> excludedReceivers = ConcurrentHashMap.newKeySet();
    
    public void addOrderResponse(String orderId, OrderResponse response) {
        orderResponses.put(orderId, response);
        logger.info("Added response for order {}: {}", orderId, response.getStatus());
    }
    
    public GetOrderResponseResponse getOrderResponse(GetOrderResponseRequest request) {
        try {
            OrderResponse response = orderResponses.get(request.getOrderId());
            
            if (response == null) {
                return GetOrderResponseResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Order response not found: " + request.getOrderId())
                        .build();
            }
            
            return GetOrderResponseResponse.newBuilder()
                    .setSuccess(true)
                    .setOrderResponse(response)
                    .setMessage("Order response retrieved successfully")
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error getting order response", e);
            return GetOrderResponseResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to get order response: " + e.getMessage())
                    .build();
        }
    }
    
    public ListAllOrderResponsesResponse listAllOrderResponses(ListAllOrderResponsesRequest request) {
        try {
            List<String> orderIds = new ArrayList<>(orderResponses.keySet());
            
            return ListAllOrderResponsesResponse.newBuilder()
                    .addAllOrderIds(orderIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing all order responses", e);
            return ListAllOrderResponsesResponse.newBuilder().build();
        }
    }
    
    public ListFilledOrderResponsesResponse listFilledOrderResponses(ListFilledOrderResponsesRequest request) {
        try {
            List<String> filledOrderIds = orderResponses.values().stream()
                    .filter(response -> response.getStatus().equals("FILLED"))
                    .map(OrderResponse::getOrderId)
                    .collect(Collectors.toList());
            
            return ListFilledOrderResponsesResponse.newBuilder()
                    .addAllOrderIds(filledOrderIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing filled order responses", e);
            return ListFilledOrderResponsesResponse.newBuilder().build();
        }
    }
    
    public ListRejectedOrderResponsesResponse listRejectedOrderResponses(ListRejectedOrderResponsesRequest request) {
        try {
            List<String> rejectedOrderIds = orderResponses.values().stream()
                    .filter(response -> response.getStatus().equals("REJECTED"))
                    .map(OrderResponse::getOrderId)
                    .collect(Collectors.toList());
            
            return ListRejectedOrderResponsesResponse.newBuilder()
                    .addAllOrderIds(rejectedOrderIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing rejected order responses", e);
            return ListRejectedOrderResponsesResponse.newBuilder().build();
        }
    }
    
    public AdminActionResponse excludeReceiver(String receiverId) {
        excludedReceivers.add(receiverId);
        logger.warn(">>> Receiver {} EXCLUDED from order sending <<<", receiverId);
        return AdminActionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Receiver " + receiverId + " excluded from order sending.")
                .build();
    }
    
    public AdminActionResponse includeReceiver(String receiverId) {
        excludedReceivers.remove(receiverId);
        logger.info(">>> Receiver {} INCLUDED in order sending <<<", receiverId);
        return AdminActionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Receiver " + receiverId + " included in order sending.")
                .build();
    }
    
    public ListExcludedReceiversResponse listExcludedReceivers(ListExcludedReceiversRequest request) {
        try {
            List<String> excludedIds = new ArrayList<>(excludedReceivers);
            
            return ListExcludedReceiversResponse.newBuilder()
                    .addAllReceiverIds(excludedIds)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error listing excluded receivers", e);
            return ListExcludedReceiversResponse.newBuilder().build();
        }
    }
    
    public ServiceInfoResponse getServiceInfo(ServiceInfoRequest request) {
        return ServiceInfoResponse.newBuilder()
                .setServiceName("Order Sender")
                .setVersion("1.0.0")
                .setStatus("RUNNING")
                .setResponseCount(orderResponses.size())
                .setExcludedReceiverCount(excludedReceivers.size())
                .addAllAvailableMethods(Arrays.asList(
                    "GetServiceInfo",
                    "ExcludeReceiver",
                    "IncludeReceiver",
                    "ListExcludedReceivers",
                    "GetOrderResponse",
                    "ListAllOrderResponses",
                    "ListFilledOrderResponses",
                    "ListRejectedOrderResponses"
                ))
                .build();
    }
    
    public boolean isReceiverExcluded(String receiverId) {
        return excludedReceivers.contains(receiverId);
    }
    
    public List<OrderResponse> getAllOrderResponses() {
        return new ArrayList<>(orderResponses.values());
    }
    
    public int getResponseCount() {
        return orderResponses.size();
    }
    
    public int getExcludedReceiverCount() {
        return excludedReceivers.size();
    }
} 