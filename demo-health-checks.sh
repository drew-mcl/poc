#!/bin/bash

echo "=== Consul Health Check Demo ==="
echo ""
echo "This demo shows how Consul health checks work with our order service:"
echo "- FIX port (8080): Main business port with TCP health check"
echo "- Admin port (9090): Management port with gRPC health check"
echo ""
echo "Starting demo..."

# Function to show current status
show_status() {
    echo "=== Current Status ==="
    echo "FIX port (8080): $(nc -w 1 localhost 8080 < /dev/null 2>&1 && echo "✅ UP" || echo "❌ DOWN")"
    echo "Admin port (9090): $(grpcurl -plaintext -timeout 2s localhost:9090 order.OrderAdminService/getServiceInfo > /dev/null 2>&1 && echo "✅ UP" || echo "❌ DOWN")"
    echo ""
}

# Function to show Consul health
show_consul_health() {
    echo "=== Consul Health Status ==="
    curl -s http://localhost:8500/v1/health/service/order-service | jq -r '.[] | "Service: \(.Service.Service) - Status: \(.Checks[] | select(.ServiceID == .ServiceID) | .Status)"' 2>/dev/null || echo "Could not check Consul health"
    echo ""
}

# Initial status
show_status
show_consul_health

echo "=== Scenario 1: FIX Server Failure (Network Issue) ==="
echo "This simulates a network failure where the FIX port becomes unavailable."
echo "This should make the service appear unhealthy in Consul."
echo ""
read -p "Press Enter to disable FIX server..."

if command -v grpcurl &> /dev/null; then
    grpcurl -plaintext -d '{"enabled": false}' localhost:9090 order.OrderAdminService/toggleFix
    echo "FIX server disabled."
    sleep 3
    show_status
    show_consul_health
else
    echo "⚠️  grpcurl not available - skipping demo"
fi

echo ""
read -p "Press Enter to re-enable FIX server..."

if command -v grpcurl &> /dev/null; then
    grpcurl -plaintext -d '{"enabled": true}' localhost:9090 order.OrderAdminService/toggleFix
    echo "FIX server enabled."
    sleep 3
    show_status
    show_consul_health
fi

echo ""
echo "=== Scenario 2: Admin Server Failure ==="
echo "This simulates the admin interface becoming unavailable."
echo "This should also make the service appear unhealthy in Consul."
echo ""
read -p "Press Enter to disable admin server..."

if command -v grpcurl &> /dev/null; then
    grpcurl -plaintext -d '{"enabled": false}' localhost:9090 order.OrderAdminService/toggleAdmin
    echo "Admin server disabled."
    sleep 3
    show_status
    show_consul_health
else
    echo "⚠️  grpcurl not available - skipping demo"
fi

echo ""
read -p "Press Enter to re-enable admin server..."

if command -v grpcurl &> /dev/null; then
    grpcurl -plaintext -d '{"enabled": true}' localhost:9090 order.OrderAdminService/toggleAdmin
    echo "Admin server enabled."
    sleep 3
    show_status
    show_consul_health
fi

echo ""
echo "=== Scenario 3: Business Logic Failure (Order Rejection) ==="
echo "This simulates a business logic failure where orders are rejected."
echo "This does NOT affect Consul health checks - the service is still healthy."
echo ""
read -p "Press Enter to enable order rejection..."

if command -v grpcurl &> /dev/null; then
    grpcurl -plaintext localhost:9090 order.OrderAdminService/rejectAllOrders
    echo "Order rejection enabled."
    show_consul_health
else
    echo "⚠️  grpcurl not available - skipping demo"
fi

echo ""
read -p "Press Enter to disable order rejection..."

if command -v grpcurl &> /dev/null; then
    grpcurl -plaintext localhost:9090 order.OrderAdminService/acceptAllOrders
    echo "Order rejection disabled."
    show_consul_health
fi

echo ""
echo "=== Demo Complete ==="
echo ""
echo "Summary:"
echo "✅ FIX port (8080) - Main business port with TCP health check"
echo "✅ Admin port (9090) - Management port with gRPC health check"
echo "✅ Both ports can be toggled to simulate infrastructure failures"
echo "✅ Order rejection affects business logic but not health checks"
echo ""
echo "In Consul UI, you should see:"
echo "- Service marked as 'critical' when either port is down"
echo "- Service marked as 'passing' when both ports are up"
echo "- Health checks update within 10 seconds" 