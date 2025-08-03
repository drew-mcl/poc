package com.example.orderservice.consul;

import com.example.orderservice.ServiceConfig;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class ConsulRegistration {
    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistration.class);
    
    private final Consul consul;
    private final AgentClient agentClient;
    private final String serviceName;
    private final String serviceId;
    private final int fixPort;
    private final int adminPort;
    
    public ConsulRegistration(String serviceName, String serviceId, int fixPort, int adminPort) {
        this.consul = Consul.builder().build();
        this.agentClient = consul.agentClient();
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.fixPort = fixPort;
        this.adminPort = adminPort;
    }
    

    
    public void register() {
        try {

            Registration.RegCheck fixCheck = Registration.RegCheck.tcp("localhost:" + fixPort, 10L);  
            Registration.RegCheck adminCheck = Registration.RegCheck.grpc("localhost:" + adminPort + "/grpc.health.v1.Health/Check", 10L);

            // Create service registration with FIX configuration metadata
            Registration registration = ImmutableRegistration.builder()
                    .id(serviceId)
                    .name(serviceName)
                    .port(fixPort)
                    .address("localhost")
                    .addTags(ServiceConfig.SERVICE_TAGS)
                    .putMeta("admin-port", String.valueOf(adminPort))
                    .putMeta("version", "2.0.0")
                    .putMeta("fix-client-id", ServiceConfig.FIX_CLIENT_ID)
                    .putMeta("fix-target-comp-id", ServiceConfig.FIX_TARGET_COMP_ID)
                    .putMeta("fix-sender-comp-id", ServiceConfig.FIX_SENDER_COMP_ID)
                    .putMeta("fix-heartbeat-interval", String.valueOf(ServiceConfig.FIX_HEARTBEAT_INTERVAL))
                    .putMeta("fix-version", ServiceConfig.FIX_VERSION)
                    .putMeta("service-type", "fix-order-service")
                    .putMeta("ordinal", String.valueOf(ServiceConfig.ORDINAL))
                    .checks(Arrays.asList(fixCheck, adminCheck))
                    .build();
            
            agentClient.register(registration);
            logger.info("Successfully registered FIX order service '{}' with Consul", serviceName);
            
        } catch (Exception e) {
            logger.error("Failed to register with Consul", e);
            throw new RuntimeException("Failed to register with Consul", e);
        }
    }
    
    public void deregister() {
        try {
            agentClient.deregister(serviceId);
            logger.info("Successfully deregistered service '{}' from Consul", serviceName);
        } catch (Exception e) {
            logger.error("Failed to deregister from Consul", e);
        }
    }
    
    public boolean isRegistered() {
        try {
            return agentClient.getServices().containsKey(serviceId);
        } catch (Exception e) {
            logger.error("Error checking registration status", e);
            return false;
        }
    }
} 