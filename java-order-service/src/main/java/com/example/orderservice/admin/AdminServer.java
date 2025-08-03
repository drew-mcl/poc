package com.example.orderservice.admin;

import com.example.orderservice.OrderManager;
import com.example.orderservice.grpc.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AdminServer {
    private static final Logger logger = LoggerFactory.getLogger(AdminServer.class);

    private final int port;
    private final OrderManager orderManager;
    private Server server;

    public AdminServer(int port, OrderManager orderManager) {
        this.port = port;
        this.orderManager = orderManager;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new OrderServiceGrpcImpl(orderManager))
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
    
    /**
     * Force shutdown the admin server to make Consul gRPC health check fail
     */
    public void forceShutdown() {
        logger.warn("FORCE SHUTDOWN of admin server on port {}", port);
        if (server != null) {
            server.shutdownNow(); // Force shutdown immediately
            logger.info("Admin server FORCE STOPPED on port {}", port);
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
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
} 