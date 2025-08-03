#!/bin/bash

# Test script for Order Sender/Receiver POC
# This script tests the basic functionality of the sender/receiver system

set -e

echo "=== Order Sender/Receiver POC Test ==="
echo

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if a service is running
check_service() {
    local service_name=$1
    if systemctl is-active --quiet $service_name; then
        print_status $GREEN "✓ $service_name is running"
        return 0
    else
        print_status $RED "✗ $service_name is not running"
        return 1
    fi
}

# Function to check if a port is listening
check_port() {
    local port=$1
    local service_name=$2
    if netstat -tuln | grep -q ":$port "; then
        print_status $GREEN "✓ Port $port is listening ($service_name)"
        return 0
    else
        print_status $RED "✗ Port $port is not listening ($service_name)"
        return 1
    fi
}

# Function to check Consul services
check_consul_services() {
    print_status $BLUE "Checking Consul services..."
    
    # Get list of services from Consul
    local services=$(curl -s http://localhost:8500/v1/catalog/services | jq -r 'keys[]' 2>/dev/null || echo "")
    
    if [[ -n "$services" ]]; then
        print_status $GREEN "✓ Consul is accessible"
        echo "Registered services:"
        echo "$services" | while read service; do
            if [[ -n "$service" ]]; then
                print_status $YELLOW "  - $service"
            fi
        done
    else
        print_status $RED "✗ Cannot access Consul or no services registered"
        return 1
    fi
}

# Function to test TCP communication
test_tcp_communication() {
    print_status $BLUE "Testing TCP communication..."
    
    # Test sending a simple order to the receiver
    local test_order="ORDER|TEST-001|AAPL|BUY|100|150.50|TRADER001|NASDAQ|$(date +%Y%m%d-%H:%M:%S.000)"
    
    echo "Sending test order: $test_order"
    
    # Send order via TCP
    local response=$(echo "$test_order" | nc localhost 9000 2>/dev/null || echo "ERROR|Connection failed")
    
    if [[ "$response" == *"FILLED"* ]] || [[ "$response" == *"REJECTED"* ]]; then
        print_status $GREEN "✓ TCP communication successful: $response"
        return 0
    else
        print_status $RED "✗ TCP communication failed: $response"
        return 1
    fi
}

# Function to check logs
check_logs() {
    print_status $BLUE "Checking service logs..."
    
    local sender_log="/opt/order-services/logs/order-sender.log"
    local receiver_log="/opt/order-services/logs/order-receiver.log"
    
    if [[ -f "$sender_log" ]]; then
        print_status $GREEN "✓ Sender log exists"
        echo "Last 5 lines of sender log:"
        tail -5 "$sender_log" | sed 's/^/  /'
    else
        print_status $YELLOW "⚠ Sender log not found"
    fi
    
    if [[ -f "$receiver_log" ]]; then
        print_status $GREEN "✓ Receiver log exists"
        echo "Last 5 lines of receiver log:"
        tail -5 "$receiver_log" | sed 's/^/  /'
    else
        print_status $YELLOW "⚠ Receiver log not found"
    fi
}

# Main test execution
echo "1. Checking service status..."
check_service "order-sender"
check_service "order-receiver"

echo
echo "2. Checking ports..."
check_port 9000 "Order Receiver TCP"

echo
echo "3. Checking Consul services..."
check_consul_services

echo
echo "4. Testing TCP communication..."
test_tcp_communication

echo
echo "5. Checking logs..."
check_logs

echo
print_status $BLUE "=== Test Summary ==="
echo "If you see green checkmarks above, the POC is working correctly!"
echo "The sender should be discovering receivers from Consul and sending orders every 15 seconds."
echo "Check the logs for detailed activity:"
echo "  - Sender logs: /opt/order-services/logs/order-sender.log"
echo "  - Receiver logs: /opt/order-services/logs/order-receiver.log"
echo "  - Consul UI: http://localhost:8500" 