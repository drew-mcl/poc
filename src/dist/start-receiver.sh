#!/bin/bash

# Distribution script for Order Receiver
# This script is called by Ansible deployment

set -e

# Configuration
SERVICE_NAME=${SERVICE_NAME:-"order-receiver"}
ORDINAL=${ORDINAL:-1}
SERVICE_ID=${SERVICE_ID:-"order-receiver-1"}
TCP_PORT=${TCP_PORT:-9000}
CONSUL_HOST=${CONSUL_HOST:-"localhost"}
CONSUL_PORT=${CONSUL_PORT:-8500}
JAVA_HOME=${JAVA_HOME:-"/usr/lib/jvm/java-11-openjdk"}
JAVA_OPTS=${JAVA_OPTS:-"-Xmx512m -Xms256m"}

# Service directories
SERVICE_HOME=${SERVICE_HOME:-"/opt/order-services"}
LOGS_DIR=${LOGS_DIR:-"$SERVICE_HOME/logs"}

echo "Starting Order Receiver..."
echo "Service ID: $SERVICE_ID"
echo "TCP Port: $TCP_PORT"
echo "Consul: $CONSUL_HOST:$CONSUL_PORT"

# Kill any existing receiver processes
pkill -f "order-receiver" || true

# Set environment variables
export SERVICE_NAME
export ORDINAL
export SERVICE_ID
export TCP_PORT
export CONSUL_HOST
export CONSUL_PORT
export JAVA_HOME
export JAVA_OPTS

# Start Order Receiver
cd "$SERVICE_HOME"
nohup java $JAVA_OPTS -cp "order-receiver/build/libs/*:order-receiver/build/classes/java/main:order-receiver/build/resources/main" com.example.orderreceiver.OrderReceiverApplication > "$LOGS_DIR/order-receiver.log" 2>&1 &

echo "Order Receiver started. PID: $!"
echo "Logs: $LOGS_DIR/order-receiver.log" 