#!/bin/bash

echo "=== Testing gRPC Admin Functionality ==="
echo ""

# Function to check if service is running
check_service() {
    local service_name=$1
    local port=$2
    
    echo "Checking $service_name on port $port..."
    
    # Check if port is listening
    if nc -z localhost $port 2>/dev/null; then
        echo "✅ Port $port is listening"
    else
        echo "❌ Port $port is not listening"
        return 1
    fi
    
    # Test gRPC connection if it's admin port
    if [[ $port == 909* ]]; then
        echo "Testing gRPC connection..."
        if command -v grpcurl &> /dev/null; then
            if grpcurl -plaintext -timeout 5s localhost:$port list > /dev/null 2>&1; then
                echo "✅ gRPC reflection working"
            else
                echo "❌ gRPC reflection failed"
                return 1
            fi
        else
            echo "⚠️  grpcurl not available - skipping gRPC test"
        fi
    fi
    
    return 0
}

# Function to test admin commands
test_admin_commands() {
    echo ""
    echo "Testing admin commands..."
    
    if command -v grpcurl &> /dev/null; then
        echo "1. Testing getServiceInfo..."
        grpcurl -plaintext localhost:9090 order.OrderAdminService/getServiceInfo
        
        echo ""
        echo "2. Testing rejectAllOrders..."
        grpcurl -plaintext localhost:9090 order.OrderAdminService/rejectAllOrders
        
        echo ""
        echo "3. Testing toggleFix (disable)..."
        grpcurl -plaintext -d '{"enabled": false}' localhost:9090 order.OrderAdminService/toggleFix
        
        echo ""
        echo "4. Testing toggleFix (enable)..."
        grpcurl -plaintext -d '{"enabled": true}' localhost:9090 order.OrderAdminService/toggleFix
        
        echo ""
        echo "5. Testing toggleAdmin (disable)..."
        grpcurl -plaintext -d '{"enabled": false}' localhost:9090 order.OrderAdminService/toggleAdmin
        
        echo ""
        echo "6. Testing toggleAdmin (enable)..."
        grpcurl -plaintext -d '{"enabled": true}' localhost:9090 order.OrderAdminService/toggleAdmin
        
        echo ""
        echo "7. Testing acceptAllOrders..."
        grpcurl -plaintext localhost:9090 order.OrderAdminService/acceptAllOrders
        
    else
        echo "⚠️  grpcurl not available - install with: brew install grpcurl"
    fi
}

# Function to test Go CLI admin
test_go_admin() {
    echo ""
    echo "Testing Go CLI admin..."
    
    if [ -f "go-asgard-admin/asgard-admin" ]; then
        echo "1. Testing discover command..."
        cd go-asgard-admin && ./asgard-admin discover && cd ..
        
        echo ""
        echo "2. Testing admin command with --all flag..."
        echo "This will show interactive admin - you can test it manually"
        echo "Run: cd go-asgard-admin && ./asgard-admin admin --all"
        
    else
        echo "❌ Go CLI not built - run: cd go-asgard-admin && go build -o asgard-admin"
    fi
}

# Main test
echo "1. Checking service ports..."
check_service "FIX" 8080
check_service "Admin" 9090

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All ports are working"
    
    test_admin_commands
    test_go_admin
    
    echo ""
    echo "=== Test Complete ==="
    echo ""
    echo "If all tests passed, you can:"
    echo "- Use grpcurl directly: grpcurl -plaintext localhost:9090 list"
    echo "- Use Go CLI: cd go-asgard-admin && ./asgard-admin admin"
    echo "- Use Go CLI with all instances: cd go-asgard-admin && ./asgard-admin admin --all"
else
    echo ""
    echo "❌ Some ports are not working"
    echo "Make sure the service is running: ./start-single-service.sh"
fi 