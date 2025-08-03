package com.example.ordersender.admin;

import com.example.ordersender.OrderSenderApplication;
import com.example.ordersender.grpc.*;
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
    private final OrderSenderApplication application;
    private Server server;
    
    public AdminServer(int port, OrderSenderApplication application) {
        this.port = port;
        this.application = application;
    }
    
    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new OrderSenderAdminServiceGrpcImpl())
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
    
    private class OrderSenderAdminServiceGrpcImpl extends OrderSenderAdminServiceGrpc.OrderSenderAdminServiceImplBase {
        
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
        public void excludeReceiver(ExcludeReceiverRequest request, StreamObserver<AdminActionResponse> responseObserver) {
            try {
                AdminActionResponse response = application.getOrderManager().excludeReceiver(request.getReceiverId());
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error excluding receiver", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void includeReceiver(IncludeReceiverRequest request, StreamObserver<AdminActionResponse> responseObserver) {
            try {
                AdminActionResponse response = application.getOrderManager().includeReceiver(request.getReceiverId());
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error including receiver", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void listExcludedReceivers(ListExcludedReceiversRequest request, StreamObserver<ListExcludedReceiversResponse> responseObserver) {
            try {
                ListExcludedReceiversResponse response = application.getOrderManager().listExcludedReceivers(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error listing excluded receivers", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void getOrderResponse(GetOrderResponseRequest request, StreamObserver<GetOrderResponseResponse> responseObserver) {
            try {
                GetOrderResponseResponse response = application.getOrderManager().getOrderResponse(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error getting order response", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void listAllOrderResponses(ListAllOrderResponsesRequest request, StreamObserver<ListAllOrderResponsesResponse> responseObserver) {
            try {
                ListAllOrderResponsesResponse response = application.getOrderManager().listAllOrderResponses(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error listing all order responses", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void listFilledOrderResponses(ListFilledOrderResponsesRequest request, StreamObserver<ListFilledOrderResponsesResponse> responseObserver) {
            try {
                ListFilledOrderResponsesResponse response = application.getOrderManager().listFilledOrderResponses(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error listing filled order responses", e);
                responseObserver.onError(e);
            }
        }
        
        @Override
        public void listRejectedOrderResponses(ListRejectedOrderResponsesRequest request, StreamObserver<ListRejectedOrderResponsesResponse> responseObserver) {
            try {
                ListRejectedOrderResponsesResponse response = application.getOrderManager().listRejectedOrderResponses(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error listing rejected order responses", e);
                responseObserver.onError(e);
            }
        }
    }
} 