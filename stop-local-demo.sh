#!/bin/bash

# Stop local demo script for Order Sender/Receiver POC

echo "=== Stopping Local Order Sender/Receiver Demo ==="
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

# Function to kill processes
kill_processes() {
    local pattern=$1
    local service_name=$2
    
    if pgrep -f "$pattern" > /dev/null; then
        print_status $YELLOW "Stopping $service_name..."
        pkill -f "$pattern"
        sleep 2
        
        # Force kill if still running
        if pgrep -f "$pattern" > /dev/null; then
            print_status $RED "Force killing $service_name..."
            pkill -9 -f "$pattern"
        fi
        
        print_status $GREEN "✓ $service_name stopped"
    else
        print_status $BLUE "✓ $service_name not running"
    fi
}

# Stop all services
print_status $BLUE "Stopping all services..."

kill_processes "order-sender" "Order Sender"
kill_processes "order-receiver" "Order Receiver"
kill_processes "consul agent" "Consul Agent"

# Check if any processes are still running
echo
print_status $BLUE "Checking for remaining processes..."

if pgrep -f "order-sender\|order-receiver\|consul agent" > /dev/null; then
    print_status $RED "Some processes are still running:"
    pgrep -f "order-sender\|order-receiver\|consul agent" | xargs ps -p
else
    print_status $GREEN "✓ All services stopped successfully"
fi

echo
print_status $GREEN "=== Demo Stopped Successfully ==="
echo
echo "All services have been stopped."
echo "To restart the demo, run: ./start-local-demo.sh" 