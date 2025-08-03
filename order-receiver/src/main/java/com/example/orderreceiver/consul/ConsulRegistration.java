package com.example.orderreceiver.consul;

import com.example.orderreceiver.ReceiverConfig;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsulRegistration {
    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistration.class);
    
    private final Consul consul;
    private final AgentClient agentClient;
    private final String serviceName;
    private final String serviceId;
    private final int mainPort; // Primary service port (TCP for orders)
    private final Map<String, Integer> additionalPorts; // Additional ports (admin, metrics, etc.)
    
    public ConsulRegistration(String serviceName, String serviceId, int mainPort, Map<String, Integer> additionalPorts) {
        this.consul = Consul.builder().build();
        this.agentClient = consul.agentClient();
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.mainPort = mainPort;
        this.additionalPorts = additionalPorts != null ? additionalPorts : new HashMap<>();
    }
    
    // Convenience constructor for receiver (TCP port as main port, admin port as additional)
    public ConsulRegistration(String serviceName, String serviceId, int tcpPort, int adminPort) {
        Map<String, Integer> additionalPorts = new HashMap<>();
        additionalPorts.put("admin", adminPort);
        this.consul = Consul.builder().build();
        this.agentClient = consul.agentClient();
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.mainPort = tcpPort;
        this.additionalPorts = additionalPorts;
    }
    
    public void register() {
        try {
            logger.info("Registering service with Consul: {} (ID: {})", serviceName, serviceId);
            
            // Create metadata with all ports
            Map<String, String> metadata = new HashMap<>();
            metadata.put("main_port", String.valueOf(mainPort));
            metadata.put("version", "1.0.0");
            metadata.put("service_type", "order-receiver");
            metadata.put("ordinal", String.valueOf(ReceiverConfig.ORDINAL));
            
            // Add additional ports to metadata - use hyphen format for admin-port
            for (Map.Entry<String, Integer> entry : additionalPorts.entrySet()) {
                String key = entry.getKey();
                if ("admin".equals(key)) {
                    metadata.put("admin-port", String.valueOf(entry.getValue())); // Use hyphen for admin-port
                } else {
                    metadata.put(key + "_port", String.valueOf(entry.getValue()));
                }
            }
            
            // Create health checks
            List<Registration.RegCheck> checks = Arrays.asList(
                Registration.RegCheck.tcp("localhost:" + mainPort, 10L)
            );
            
            // Add health checks for additional ports if they support health checks
            if (additionalPorts.containsKey("admin")) {
                checks = Arrays.asList(
                    Registration.RegCheck.tcp("localhost:" + mainPort, 10L),
                    Registration.RegCheck.grpc("localhost:" + additionalPorts.get("admin") + "/grpc.health.v1.Health/Check", 10L)
                );
            }
            
            // Create service registration
            Registration registration = ImmutableRegistration.builder()
                    .id(serviceId)
                    .name(serviceName)
                    .port(mainPort)
                    .address("localhost")
                    .addTags(ReceiverConfig.SERVICE_TAGS)
                    .meta(metadata)
                    .checks(checks)
                    .build();
            
            agentClient.register(registration);
            logger.info("Successfully registered service with Consul. Main port: {}, Additional ports: {}", 
                       mainPort, additionalPorts);
            
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