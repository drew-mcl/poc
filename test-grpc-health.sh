#!/bin/bash

echo "=== Testing gRPC Health Check ==="
echo ""

# Function to test gRPC health check
test_grpc_health() {
    local port=$1
    
    echo "Testing gRPC health check on port $port..."
    
    if command -v grpcurl &> /dev/null; then
        # Test the health check endpoint
        if grpcurl -plaintext -d '{"service": ""}' localhost:$port grpc.health.v1.Health/Check; then
            echo "✅ gRPC health check working on port $port"
            return 0
        else
            echo "❌ gRPC health check failed on port $port"
            return 1
        fi
    else
        echo "⚠️  grpcurl not available - install with: brew install grpcurl"
        return 1
    fi
}

# Function to test service discovery
test_service_discovery() {
    echo ""
    echo "Testing service discovery..."
    
    if command -v grpcurl &> /dev/null; then
        # Test listing services
        if grpcurl -plaintext localhost:9090 list; then
            echo "✅ gRPC reflection working"
        else
            echo "❌ gRPC reflection failed"
            return 1
        fi
        
        # Test our admin service
        if grpcurl -plaintext localhost:9090 order.OrderAdminService/getServiceInfo; then
            echo "✅ Admin service working"
        else
            echo "❌ Admin service failed"
            return 1
        fi
    else
        echo "⚠️  grpcurl not available - skipping service discovery test"
    fi
}

# Main test
echo "1. Testing gRPC health check..."
if test_grpc_health 9090; then
    echo ""
    echo "✅ gRPC health check is working!"
    
    test_service_discovery
    
    echo ""
    echo "=== Test Complete ==="
    echo ""
    echo "The gRPC health check should now work with Consul."
    echo "You can test it manually with:"
    echo "grpcurl -plaintext -d '{\"service\": \"\"}' localhost:9090 grpc.health.v1.Health/Check"
else
    echo ""
    echo "❌ gRPC health check is not working"
    echo "Make sure the service is running: ./start-single-service.sh"
fi 