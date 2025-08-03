#!/bin/bash

# Distribution script for Order Sender
# This script is called by Ansible deployment

set -e

# Configuration
SENDER_ID=${SENDER_ID:-"order-sender-1"}
SERVICE_NAME=${SERVICE_NAME:-"order-sender"}
SERVICE_ADDRESS=${SERVICE_ADDRESS:-"localhost"}
CONSUL_HOST=${CONSUL_HOST:-"localhost"}
CONSUL_PORT=${CONSUL_PORT:-8500}
RECEIVER_SERVICE_NAME=${RECEIVER_SERVICE_NAME:-"order-receiver"}
MOCK_ORDER_ENABLED=${MOCK_ORDER_ENABLED:-"true"}
MOCK_ORDER_INITIAL_DELAY=${MOCK_ORDER_INITIAL_DELAY:-2}
MOCK_ORDER_INTERVAL=${MOCK_ORDER_INTERVAL:-3}
TCP_TIMEOUT_MS=${TCP_TIMEOUT_MS:-5000}
TCP_RETRY_ATTEMPTS=${TCP_RETRY_ATTEMPTS:-3}
JAVA_HOME=${JAVA_HOME:-"/usr/lib/jvm/java-11-openjdk"}
JAVA_OPTS=${JAVA_OPTS:-"-Xmx512m -Xms256m"}

# Service directories
SERVICE_HOME=${SERVICE_HOME:-"/opt/order-services"}
LOGS_DIR=${LOGS_DIR:-"$SERVICE_HOME/logs"}

echo "Starting Order Sender..."
echo "Sender ID: $SENDER_ID"
echo "Service Name: $SERVICE_NAME"
echo "Service Address: $SERVICE_ADDRESS"
echo "Consul: $CONSUL_HOST:$CONSUL_PORT"
echo "Receiver Service: $RECEIVER_SERVICE_NAME"

# Kill any existing sender processes
pkill -f "order-sender" || true

# Set environment variables
export SENDER_ID
export SERVICE_NAME
export SERVICE_ADDRESS
export CONSUL_HOST
export CONSUL_PORT
export RECEIVER_SERVICE_NAME
export MOCK_ORDER_ENABLED
export MOCK_ORDER_INITIAL_DELAY
export MOCK_ORDER_INTERVAL
export TCP_TIMEOUT_MS
export TCP_RETRY_ATTEMPTS
export JAVA_HOME
export JAVA_OPTS

# Start Order Sender
cd "$SERVICE_HOME"
nohup java $JAVA_OPTS -cp "order-sender/build/libs/*:order-sender/build/classes/java/main:order-sender/build/resources/main" com.example.ordersender.OrderSenderApplication > "$LOGS_DIR/order-sender.log" 2>&1 &

echo "Order Sender started. PID: $!"
echo "Logs: $LOGS_DIR/order-sender.log" 