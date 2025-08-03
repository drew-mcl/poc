#!/bin/bash

# Test script to verify rejected orders functionality
# Usage: ./test-rejected-orders.sh [service_ordinal]

set -e

SERVICE_ORDINAL=${1:-1}
GRPC_PORT=$((9090 + SERVICE_ORDINAL - 1))

echo "=== Testing Rejected Orders Functionality ==="
echo "Testing order-service-$SERVICE_ORDINAL on gRPC port $GRPC_PORT"
echo ""

# Function to call gRPC service
call_grpc() {
    local method=$1
    local request=$2
    local port=$3
    
    echo "Calling $method..."
    echo "Request: $request"
    
    # Use grpcurl to call the service
    response=$(grpcurl -plaintext -d "$request" localhost:$port OrderAdminService/$method 2>/dev/null || echo "ERROR")
    
    echo "Response: $response"
    echo ""
}

# Test 1: Check initial state
echo "Test 1: Check initial state"
call_grpc "ListCancelledOrRejectedOrders" '{}' $GRPC_PORT

# Test 2: Enable reject all mode
echo "Test 2: Enable reject all mode"
call_grpc "RejectAllOrders" '{}' $GRPC_PORT

# Test 3: Add some orders (should be rejected)
echo "Test 3: Add orders (should be rejected)"
call_grpc "AddOrder" '{"symbol": "AAPL", "side": "1", "orderQty": 100, "price": 150.0, "ordType": "2", "timeInForce": "0", "account": "TRADER001", "exchange": "NASDAQ", "currency": "USD"}' $GRPC_PORT
call_grpc "AddOrder" '{"symbol": "MSFT", "side": "2", "orderQty": 200, "price": 300.0, "ordType": "2", "timeInForce": "0", "account": "TRADER002", "exchange": "NASDAQ", "currency": "USD"}' $GRPC_PORT

# Test 4: Check rejected orders
echo "Test 4: Check rejected orders"
call_grpc "ListCancelledOrRejectedOrders" '{}' $GRPC_PORT

# Test 5: Check all orders
echo "Test 5: Check all orders"
call_grpc "ListAllOrders" '{}' $GRPC_PORT

# Test 6: Disable reject all mode
echo "Test 6: Disable reject all mode"
call_grpc "AcceptAllOrders" '{}' $GRPC_PORT

# Test 7: Add an order (should be accepted)
echo "Test 7: Add order (should be accepted)"
call_grpc "AddOrder" '{"symbol": "GOOGL", "side": "1", "orderQty": 150, "price": 250.0, "ordType": "2", "timeInForce": "0", "account": "TRADER003", "exchange": "NASDAQ", "currency": "USD"}' $GRPC_PORT

# Test 8: Final check of all orders
echo "Test 8: Final check of all orders"
call_grpc "ListAllOrders" '{}' $GRPC_PORT

echo "=== Test Complete ===" 