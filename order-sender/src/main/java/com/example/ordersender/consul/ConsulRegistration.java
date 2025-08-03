package com.example.ordersender.consul;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ConsulRegistration {
    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistration.class);
    
    private final Consul consul;
    private final AgentClient agentClient;
    private final String serviceId;
    private final String serviceName;
    private final String serviceAddress;
    private final int adminPort;
    
    public ConsulRegistration(String consulHost, int consulPort, String serviceId, String serviceName, 
                           String serviceAddress, int adminPort) {
        this.consul = Consul.builder().build();
        this.agentClient = consul.agentClient();
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.serviceAddress = serviceAddress;
        this.adminPort = adminPort;
    }
    
    public void register() {
        try {
            logger.info("Registering sender service with Consul: {} (ID: {})", serviceName, serviceId);
            
            // Create metadata - admin port in metadata only
            Map<String, String> metadata = new HashMap<>();
            metadata.put("admin-port", String.valueOf(adminPort)); // Use hyphen format
            metadata.put("service_type", "sender");
            metadata.put("version", "1.0.0");
            
            // Create health check for admin gRPC port
            Registration.RegCheck adminCheck = Registration.RegCheck.grpc(serviceAddress + ":" + adminPort + "/grpc.health.v1.Health/Check", 10L);
            
            // Create registration - no main port, just admin port in metadata
            Registration registration = ImmutableRegistration.builder()
                .id(serviceId)
                .name(serviceName)
                .address(serviceAddress)
                .port(adminPort)  // Use admin port as the registered port for health checks
                .addTags("sender", "admin", "grpc")
                .meta(metadata)
                .check(adminCheck)
                .build();
            
            agentClient.register(registration);
            logger.info("Successfully registered sender service with Consul. Admin port: {}", adminPort);
            
        } catch (Exception e) {
            logger.error("Failed to register sender service with Consul", e);
            throw new RuntimeException("Failed to register with Consul", e);
        }
    }
    
    public void deregister() {
        try {
            logger.info("Deregistering sender service from Consul: {}", serviceId);
            agentClient.deregister(serviceId);
            logger.info("Successfully deregistered sender service from Consul");
        } catch (Exception e) {
            logger.error("Failed to deregister sender service from Consul", e);
        }
    }
    
    public void close() {
        if (consul != null) {
            consul.destroy();
        }
    }
} 