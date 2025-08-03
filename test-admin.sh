#!/bin/bash

echo "Testing Admin functionality of Java Order Service..."
echo "Connecting to localhost:9090..."

# Test admin connection using grpcurl
if command -v grpcurl &> /dev/null; then
    echo "Testing service info..."
    grpcurl -plaintext localhost:9090 order.OrderAdminService/getServiceInfo
    
    echo ""
    echo "Testing reject all orders..."
    grpcurl -plaintext localhost:9090 order.OrderAdminService/rejectAllOrders
    
    echo ""
    echo "Testing toggle FIX server (disable)..."
    grpcurl -plaintext -d '{"enabled": false}' localhost:9090 order.OrderAdminService/toggleFix
    
    echo ""
    echo "Testing toggle FIX server (enable)..."
    grpcurl -plaintext -d '{"enabled": true}' localhost:9090 order.OrderAdminService/toggleFix
    
    echo ""
    echo "Testing accept all orders..."
    grpcurl -plaintext localhost:9090 order.OrderAdminService/acceptAllOrders
    
else
    echo "❌ grpcurl not found. Please install it to test admin functionality."
    echo "Install with: brew install grpcurl"
fi 