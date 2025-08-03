#!/bin/bash

echo "=== Testing Consul Health Check Scenarios ==="
echo ""

# Function to check service health in Consul
check_consul_health() {
    echo "Checking Consul health status..."
    curl -s http://localhost:8500/v1/health/service/order-service | jq -r '.[] | "Service: \(.Service.Service) - Status: \(.Checks[] | select(.ServiceID == .ServiceID) | .Status)"' 2>/dev/null || echo "Could not check Consul health"
    echo ""
}

# Function to test FIX connection
test_fix_connection() {
    echo "Testing FIX connection on port 8080..."
    nc -w 5 localhost 8080 < /dev/null
    if [ $? -eq 0 ]; then
        echo "✅ FIX connection successful!"
    else
        echo "❌ FIX connection failed!"
    fi
    echo ""
}

# Function to test admin connection
test_admin_connection() {
    echo "Testing admin connection on port 9090..."
    if command -v grpcurl &> /dev/null; then
        grpcurl -plaintext -timeout 5s localhost:9090 order.OrderAdminService/getServiceInfo > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo "✅ Admin connection successful!"
        else
            echo "❌ Admin connection failed!"
        fi
    else
        echo "⚠️  grpcurl not available - skipping admin test"
    fi
    echo ""
}

# Function to toggle FIX server
toggle_fix() {
    local enabled=$1
    echo "Toggling FIX server: $enabled"
    if command -v grpcurl &> /dev/null; then
        grpcurl -plaintext -d "{\"enabled\": $enabled}" localhost:9090 order.OrderAdminService/toggleFix
    else
        echo "⚠️  grpcurl not available - cannot toggle FIX server"
    fi
    echo ""
}

# Function to toggle admin server
toggle_admin() {
    local enabled=$1
    echo "Toggling admin server: $enabled"
    if command -v grpcurl &> /dev/null; then
        grpcurl -plaintext -d "{\"enabled\": $enabled}" localhost:9090 order.OrderAdminService/toggleAdmin
    else
        echo "⚠️  grpcurl not available - cannot toggle admin server"
    fi
    echo ""
}

# Function to reject all orders
reject_orders() {
    echo "Rejecting all new orders..."
    if command -v grpcurl &> /dev/null; then
        grpcurl -plaintext localhost:9090 order.OrderAdminService/rejectAllOrders
    else
        echo "⚠️  grpcurl not available - cannot reject orders"
    fi
    echo ""
}

# Function to accept all orders
accept_orders() {
    echo "Accepting all new orders..."
    if command -v grpcurl &> /dev/null; then
        grpcurl -plaintext localhost:9090 order.OrderAdminService/acceptAllOrders
    else
        echo "⚠️  grpcurl not available - cannot accept orders"
    fi
    echo ""
}

# Main test scenarios
echo "1. Initial health check..."
check_consul_health

echo "2. Testing initial connections..."
test_fix_connection
test_admin_connection

echo "3. Testing FIX server shutdown (network failure simulation)..."
toggle_fix false
sleep 3
test_fix_connection
check_consul_health

echo "4. Testing FIX server restart..."
toggle_fix true
sleep 3
test_fix_connection
check_consul_health

echo "5. Testing admin server shutdown..."
toggle_admin false
sleep 3
test_admin_connection
check_consul_health

echo "6. Testing admin server restart..."
toggle_admin true
sleep 3
test_admin_connection
check_consul_health

echo "7. Testing order rejection..."
reject_orders
sleep 2
check_consul_health

echo "8. Testing order acceptance..."
accept_orders
sleep 2
check_consul_health

echo "=== Test Complete ==="
echo ""
echo "Summary:"
echo "- FIX port (8080) is the main Consul health check port with TCP check"
echo "- Admin port (9090) has gRPC health check"
echo "- Both can be toggled to simulate failures"
echo "- Order rejection affects business logic but not health checks" 