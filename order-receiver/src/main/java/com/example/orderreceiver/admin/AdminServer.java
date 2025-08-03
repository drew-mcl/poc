package com.example.orderreceiver.admin;

import com.example.orderreceiver.OrderReceiverApplication;
import com.example.orderreceiver.grpc.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AdminServer {
    private static final Logger logger = LoggerFactory.getLogger(AdminServer.class);
    
    private final int port;
    private final OrderReceiverApplication application;
    private Server server;
    
    public AdminServer(int port, OrderReceiverApplication application) {
        this.port = port;
        this.application = application;
    }
    
    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new OrderReceiverAdminServiceGrpcImpl())
                .addService(ProtoReflectionService.newInstance())
                .addService(new HealthCheckService())
                .build()
                .start();
        
        logger.info("Admin server started on port {}", port);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down admin server...");
            try {
                AdminServer.this.shutdown();
            } catch (InterruptedException e) {
                logger.error("Error during admin server shutdown", e);
            }
        }));
    }
    
    public void shutdown() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            logger.info("Admin server shutdown complete");
        }
    }
    
    public void forceShutdown() {
        if (server != null) {
            server.shutdownNow();
            logger.info("Admin server force shutdown");
        }
    }
    
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }
    
    /**
     * Simple health check service implementation
     */
    private static class HealthCheckService extends HealthGrpc.HealthImplBase {
        @Override
        public void check(io.grpc.health.v1.HealthCheckRequest request, 
                         StreamObserver<HealthCheckResponse> responseObserver) {
            HealthCheckResponse response = HealthCheckResponse.newBuilder()
                    .setStatus(HealthCheckResponse.ServingStatus.SERVING)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    private class OrderReceiverAdminServiceGrpcImpl extends OrderReceiverAdminServiceGrpc.OrderReceiverAdminServiceImplBase {
        
        @Override
        public void getServiceInfo(ServiceInfoRequest request, StreamObserver<ServiceInfoResponse> responseObserver) {
            try {
                ServiceInfoResponse response = application.getOrderManager().getServiceInfo(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error getting service info", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void rejectAllOrders(RejectAllOrdersRequest request, StreamObserver<AdminActionResponse> responseObserver) {
            try {
                application.setRejectMode(true);
                AdminActionResponse response = application.getOrderManager().rejectAllOrders();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error rejecting all orders", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void acceptAllOrders(AcceptAllOrdersRequest request, StreamObserver<AdminActionResponse> responseObserver) {
            try {
                application.setRejectMode(false);
                AdminActionResponse response = application.getOrderManager().acceptAllOrders();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error accepting all orders", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void getOrder(GetOrderRequest request, StreamObserver<GetOrderResponse> responseObserver) {
            try {
                GetOrderResponse response = application.getOrderManager().getOrder(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error getting order", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void listAllOrderIds(ListAllOrderIdsRequest request, StreamObserver<ListAllOrderIdsResponse> responseObserver) {
            try {
                ListAllOrderIdsResponse response = application.getOrderManager().listAllOrderIds(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error listing all order IDs", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void listRejectedOrderIds(ListRejectedOrderIdsRequest request, StreamObserver<ListRejectedOrderIdsResponse> responseObserver) {
            try {
                ListRejectedOrderIdsResponse response = application.getOrderManager().listRejectedOrderIds(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error listing rejected order IDs", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void listFilledOrderIds(ListFilledOrderIdsRequest request, StreamObserver<ListFilledOrderIdsResponse> responseObserver) {
            try {
                ListFilledOrderIdsResponse response = application.getOrderManager().listFilledOrderIds(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error listing filled order IDs", e);
                responseObserver.onError(e);
            }
        }
    }
} 