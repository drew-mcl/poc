package com.example.orderservice.admin;

import com.example.orderservice.OrderManager;
import com.example.orderservice.grpc.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderServiceGrpcImpl extends OrderAdminServiceGrpc.OrderAdminServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceGrpcImpl.class);
    
    private final OrderManager orderManager;
    
    public OrderServiceGrpcImpl(OrderManager orderManager) {
        this.orderManager = orderManager;
    }
    
    @Override
    public void addOrder(AddOrderRequest request, 
                       StreamObserver<AddOrderResponse> responseObserver) {
        try {
            AddOrderResponse response = orderManager.addOrder(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in addOrder", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void cancelOrder(CancelOrderRequest request, 
                          StreamObserver<CancelOrderResponse> responseObserver) {
        try {
            CancelOrderResponse response = orderManager.cancelOrder(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in cancelOrder", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getOrder(GetOrderRequest request, 
                       StreamObserver<GetOrderResponse> responseObserver) {
        try {
            GetOrderResponse response = orderManager.getOrder(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getOrder", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void listAllOrders(ListAllOrdersRequest request, 
                            StreamObserver<ListAllOrdersResponse> responseObserver) {
        try {
            ListAllOrdersResponse response = orderManager.listAllOrders(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in listAllOrders", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void listOpenOrders(ListOpenOrdersRequest request, 
                             StreamObserver<ListOpenOrdersResponse> responseObserver) {
        try {
            ListOpenOrdersResponse response = orderManager.listOpenOrders(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in listOpenOrders", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void listCancelledOrRejectedOrders(ListCancelledOrRejectedOrdersRequest request, 
                                            StreamObserver<ListCancelledOrRejectedOrdersResponse> responseObserver) {
        try {
            ListCancelledOrRejectedOrdersResponse response = orderManager.listCancelledOrRejectedOrders(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in listCancelledOrRejectedOrders", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getServiceInfo(ServiceInfoRequest request, 
                             StreamObserver<ServiceInfoResponse> responseObserver) {
        try {
            ServiceInfoResponse response = orderManager.getServiceInfo(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getServiceInfo", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void rejectAllOrders(RejectAllOrdersRequest request, 
                              StreamObserver<AdminActionResponse> responseObserver) {
        try {
            AdminActionResponse response = orderManager.rejectAllOrders();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in rejectAllOrders", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void acceptAllOrders(AcceptAllOrdersRequest request, 
                              StreamObserver<AdminActionResponse> responseObserver) {
        try {
            AdminActionResponse response = orderManager.acceptAllOrders();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in acceptAllOrders", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getFixConfig(FixConfigRequest request, 
                           StreamObserver<FixConfigResponse> responseObserver) {
        try {
            FixConfigResponse response = orderManager.getFixConfig(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getFixConfig", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void toggleFix(ToggleFixRequest request, 
                        StreamObserver<ToggleFixResponse> responseObserver) {
        try {
            ToggleFixResponse response = orderManager.toggleFix(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in toggleFix", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void toggleAdmin(ToggleAdminRequest request, 
                          StreamObserver<ToggleAdminResponse> responseObserver) {
        try {
            ToggleAdminResponse response = orderManager.toggleAdmin(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in toggleAdmin", e);
            responseObserver.onError(e);
        }
    }
} 