package com.example.orderservice.grpc;

import com.example.orderservice.OrderManager;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrpcServer {
    private static final Logger logger = LoggerFactory.getLogger(GrpcServer.class);
    
    private final Server server;
    private final OrderManager orderManager;
    
    public GrpcServer(int port, OrderManager orderManager) {
        this.orderManager = orderManager;
        this.server = ServerBuilder.forPort(port)
                .addService(new OrderAdminServiceImpl(orderManager))
                .addService(ProtoReflectionService.newInstance())
                .build();
    }
    
    public void start() throws IOException {
        server.start();
        logger.info("gRPC server started on port {}", server.getPort());
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server...");
            try {
                GrpcServer.this.shutdown();
            } catch (InterruptedException e) {
                logger.error("Error shutting down gRPC server", e);
            }
        }));
    }
    
    public void shutdown() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }
    
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
    
    private static class OrderAdminServiceImpl extends OrderAdminServiceGrpc.OrderAdminServiceImplBase {
        private final OrderManager orderManager;
        
        public OrderAdminServiceImpl(OrderManager orderManager) {
            this.orderManager = orderManager;
        }
        
        @Override
        public void addOrder(AddOrderRequest request, 
                           io.grpc.stub.StreamObserver<AddOrderResponse> responseObserver) {
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
                              io.grpc.stub.StreamObserver<CancelOrderResponse> responseObserver) {
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
                           io.grpc.stub.StreamObserver<GetOrderResponse> responseObserver) {
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
                                io.grpc.stub.StreamObserver<ListAllOrdersResponse> responseObserver) {
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
                                 io.grpc.stub.StreamObserver<ListOpenOrdersResponse> responseObserver) {
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
                                                io.grpc.stub.StreamObserver<ListCancelledOrRejectedOrdersResponse> responseObserver) {
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
                                 io.grpc.stub.StreamObserver<ServiceInfoResponse> responseObserver) {
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
                                  io.grpc.stub.StreamObserver<AdminActionResponse> responseObserver) {
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
                                  io.grpc.stub.StreamObserver<AdminActionResponse> responseObserver) {
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
                               io.grpc.stub.StreamObserver<FixConfigResponse> responseObserver) {
            try {
                FixConfigResponse response = orderManager.getFixConfig(request);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                logger.error("Error in getFixConfig", e);
                responseObserver.onError(e);
            }
        }
    }
} 