package com.example.ordersender.consul;

import com.orbitz.consul.Consul;
import com.orbitz.consul.model.health.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ConsulServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ConsulServiceDiscovery.class);
    
    private final Consul consul;
    private final String receiverServiceName;
    
    public ConsulServiceDiscovery(String consulHost, int consulPort, String receiverServiceName) {
        this.consul = Consul.builder()
            .withUrl("http://" + consulHost + ":" + consulPort)
            .build();
        this.receiverServiceName = receiverServiceName;
    }
    
    public List<ReceiverInfo> discoverReceivers() {
        List<ReceiverInfo> receivers = new ArrayList<>();
        
        try {
            // Get healthy instances of the receiver service
            var healthyServices = consul.healthClient().getHealthyServiceInstances(receiverServiceName);
            
            logger.info("Found {} healthy receiver instances", healthyServices.getResponse().size());
            
            for (var serviceEntry : healthyServices.getResponse()) {
                Service service = serviceEntry.getService();
                
                // Extract port from metadata or use default
                int port = service.getPort();
                if (service.getMeta() != null && service.getMeta().containsKey("tcp-port")) {
                    try {
                        port = Integer.parseInt(service.getMeta().get("tcp-port"));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid tcp-port in metadata for {}: {}", service.getId(), service.getMeta().get("tcp-port"));
                    }
                }
                
                ReceiverInfo receiver = new ReceiverInfo(
                    service.getId(),
                    service.getService(),
                    service.getAddress(),
                    port,
                    service.getMeta()
                );
                
                receivers.add(receiver);
                logger.debug("Discovered receiver: {} at {}:{}", receiver.getServiceId(), receiver.getAddress(), receiver.getPort());
            }
            
        } catch (Exception e) {
            logger.error("Error discovering receivers from Consul", e);
        }
        
        return receivers;
    }
    
    public static class ReceiverInfo {
        private final String serviceId;
        private final String serviceName;
        private final String address;
        private final int port;
        private final java.util.Map<String, String> metadata;
        
        public ReceiverInfo(String serviceId, String serviceName, String address, int port, java.util.Map<String, String> metadata) {
            this.serviceId = serviceId;
            this.serviceName = serviceName;
            this.address = address;
            this.port = port;
            this.metadata = metadata != null ? metadata : new java.util.HashMap<>();
        }
        
        public String getServiceId() { return serviceId; }
        public String getServiceName() { return serviceName; }
        public String getAddress() { return address; }
        public int getPort() { return port; }
        public java.util.Map<String, String> getMetadata() { return metadata; }
        
        @Override
        public String toString() {
            return String.format("ReceiverInfo{serviceId='%s', address='%s', port=%d}", serviceId, address, port);
        }
    }
} 