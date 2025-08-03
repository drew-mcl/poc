#!/bin/bash

# Local demo script to start 1 sender and 2 receivers with Consul

set -e

echo "Starting local demo with 1 sender and 2 receivers..."

# Check if Consul is running, start if not
if ! pgrep -f "consul agent" > /dev/null; then
    echo "Starting Consul agent..."
    nohup consul agent -dev -ui -client=0.0.0.0 > logs/consul.log 2>&1 &
    sleep 3
else
    echo "Consul is already running"
fi

# Kill any existing processes
echo "Stopping any existing services..."
pkill -f "order-sender" || true
pkill -f "order-receiver" || true
sleep 2

# Create logs directory
mkdir -p logs

# Set environment variables
export CONSUL_HOST="localhost"
export CONSUL_PORT=8500
export RECEIVER_SERVICE_NAME="order-receiver"
export MOCK_ORDER_ENABLED="true"
export MOCK_ORDER_INITIAL_DELAY=2
export MOCK_ORDER_INTERVAL=3
export TCP_TIMEOUT_MS=5000
export TCP_RETRY_ATTEMPTS=3

# Start Order Sender
echo "Starting Order Sender..."
export SENDER_ID="order-sender-1"
export SERVICE_NAME="order-sender"
export SERVICE_ADDRESS="localhost"
cd order-sender
nohup ../gradlew :order-sender:run > ../logs/order-sender.log 2>&1 &
cd ..

# Start Order Receiver 1
echo "Starting Order Receiver 1..."
export SERVICE_NAME="order-receiver"
export ORDINAL=1
export SERVICE_ID="order-receiver-1"
export TCP_PORT=9001
cd order-receiver
nohup ../gradlew :order-receiver:run > ../logs/order-receiver-1.log 2>&1 &
cd ..

# Start Order Receiver 2
echo "Starting Order Receiver 2..."
export SERVICE_NAME="order-receiver"
export ORDINAL=2
export SERVICE_ID="order-receiver-2"
export TCP_PORT=9002
cd order-receiver
nohup ../gradlew :order-receiver:run > ../logs/order-receiver-2.log 2>&1 &
cd ..

echo "Local demo started!"
echo "Services:"
echo "  - Consul: http://localhost:8500"
echo "  - Order Sender: Admin port 9200"
echo "  - Order Receiver 1: TCP port 9001, Admin port 9201"
echo "  - Order Receiver 2: TCP port 9002, Admin port 9202"
echo ""
echo "Logs:"
echo "  - Consul: logs/consul.log"
echo "  - Sender: logs/order-sender.log"
echo "  - Receiver 1: logs/order-receiver-1.log"
echo "  - Receiver 2: logs/order-receiver-2.log"
echo ""
echo "To stop all services: ./stop-local-demo.sh" 