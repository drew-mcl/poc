#!/bin/bash

echo "=== Testing Admin Port Usage ==="
echo ""

# Function to check Consul metadata
check_consul_metadata() {
    echo "1. Checking Consul service metadata..."
    
    if command -v curl &> /dev/null && command -v jq &> /dev/null; then
        # Get service metadata from Consul
        response=$(curl -s http://localhost:8500/v1/catalog/service/order-service)
        
        if [ $? -eq 0 ]; then
            echo "✅ Consul service data retrieved"
            
            # Check if admin-port metadata exists
            admin_port=$(echo "$response" | jq -r '.[0].ServiceMeta["admin-port"] // empty')
            if [ -n "$admin_port" ]; then
                echo "✅ Admin port metadata found: $admin_port"
            else
                echo "❌ Admin port metadata not found"
                return 1
            fi
            
            # Check main service port
            service_port=$(echo "$response" | jq -r '.[0].ServicePort // empty')
            if [ -n "$service_port" ]; then
                echo "✅ Service port: $service_port"
            else
                echo "❌ Service port not found"
                return 1
            fi
            
            echo ""
            echo "Port Configuration:"
            echo "  FIX Port (main): $service_port"
            echo "  Admin Port: $admin_port"
            
        else
            echo "❌ Failed to get service data from Consul"
            return 1
        fi
    else
        echo "⚠️  curl or jq not available - skipping metadata check"
    fi
}

# Function to test Go CLI admin
test_go_admin() {
    echo ""
    echo "2. Testing Go CLI admin..."
    
    if [ -f "asgard-admin" ]; then
        echo "Testing discover command..."
        ./asgard-admin discover
        
        echo ""
        echo "Testing admin command (should show correct admin ports)..."
        echo "Run manually: ./asgard-admin admin"
        echo "Expected: Should connect to admin port (9090, 9091, etc.) not FIX port (8080, 8081, etc.)"
        
    else
        echo "❌ Go CLI not built"
        return 1
    fi
}

# Function to test direct gRPC connection
test_grpc_connection() {
    echo ""
    echo "3. Testing direct gRPC connection..."
    
    if command -v grpcurl &> /dev/null; then
        echo "Testing admin port 9090..."
        if grpcurl -plaintext -timeout 5s localhost:9090 list > /dev/null 2>&1; then
            echo "✅ Admin port 9090 is working"
        else
            echo "❌ Admin port 9090 is not working"
            return 1
        fi
        
        echo "Testing FIX port 8080 (should not have gRPC)..."
        if grpcurl -plaintext -timeout 5s localhost:8080 list > /dev/null 2>&1; then
            echo "⚠️  FIX port 8080 has gRPC (unexpected)"
        else
            echo "✅ FIX port 8080 does not have gRPC (expected)"
        fi
        
    else
        echo "⚠️  grpcurl not available - install with: brew install grpcurl"
    fi
}

# Main test
echo "Starting admin port test..."
check_consul_metadata

if [ $? -eq 0 ]; then
    test_grpc_connection
    test_go_admin
    
    echo ""
    echo "=== Test Complete ==="
    echo ""
    echo "Summary:"
    echo "- Consul should store admin-port in metadata"
    echo "- Go CLI should use admin-port metadata for gRPC connections"
    echo "- FIX port should not have gRPC services"
    echo "- Admin port should have gRPC services"
else
    echo ""
    echo "❌ Metadata check failed"
    echo "Make sure the service is running: cd .. && ./start-single-service.sh"
fi 