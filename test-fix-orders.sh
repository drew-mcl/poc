#!/bin/bash

# Test script for FIX Order Service
# This script demonstrates the new FIX order functionality

echo "🧪 Testing FIX Order Service"
echo "=============================="

# Check if Consul is running
echo "📋 Checking Consul status..."
if ! curl -s http://localhost:8500/v1/status/leader > /dev/null; then
    echo "❌ Consul is not running. Please start Consul first."
    exit 1
fi
echo "✅ Consul is running"

# Check if order service is registered
echo "📋 Checking order service registration..."
SERVICES=$(curl -s http://localhost:8500/v1/catalog/services | jq -r 'keys[]' | grep -v consul)
if echo "$SERVICES" | grep -q "order-service"; then
    echo "✅ Order service is registered"
else
    echo "❌ Order service not found. Please start the Java order service first."
    exit 1
fi

# Test FIX configuration retrieval
echo ""
echo "🔧 Testing FIX Configuration Retrieval..."
echo "----------------------------------------"

# Use the Go admin tool to get FIX config
echo "Getting FIX configuration for order-service:"
./go-asgard-admin/asgard-admin fix-config order-service

echo ""
echo "📊 Testing FIX Order Generation..."
echo "----------------------------------"

# Use the Go admin tool to interact with the service
echo "Connecting to order service and listing orders..."
./go-asgard-admin/asgard-admin admin --service order-service --auto-select << EOF
6
1
EOF

echo ""
echo "🎯 Testing FIX Order Creation..."
echo "--------------------------------"

# Create a new FIX order via gRPC
echo "Creating a new FIX order (BUY 100 AAPL @ $150.00)..."
./go-asgard-admin/asgard-admin admin --service order-service --auto-select << EOF
1
AAPL
AAPL.O
1
100
150.00
2
1
TRADER001
CS
USD
NASDAQ
EOF

echo ""
echo "🎯 Testing FIX Order Retrieval..."
echo "---------------------------------"

# Get a specific order
echo "Getting order ORDER-000001..."
./go-asgard-admin/asgard-admin admin --service order-service --auto-select << EOF
2
ORDER-000001
EOF

echo ""
echo "🎯 Testing FIX Order Cancellation..."
echo "------------------------------------"

# Cancel an order via gRPC
echo "Cancelling order ORDER-000001..."
./go-asgard-admin/asgard-admin admin --service order-service --auto-select << EOF
5
ORDER-000001
EOF

echo ""
echo "📈 Testing Service Info with FIX Config..."
echo "------------------------------------------"

# Get service info
echo "Getting service information..."
./go-asgard-admin/asgard-admin admin --service order-service --auto-select << EOF
3
EOF

echo ""
echo "✅ FIX Order Service Test Complete!"
echo "==================================="
echo ""
echo "Key Features Demonstrated:"
echo "• FIX message generation with proper tags"
echo "• Stock symbols (AAPL, MSFT, etc.) and RICs"
echo "• Buy/Sell sides (1=Buy, 2=Sell)"
echo "• Random quantities and prices"
echo "• FIX configuration via Consul discovery"
echo "• Order operations using simple order_id"
echo ""
echo "The service now generates realistic FIX stock orders and uses order_id for all operations." 