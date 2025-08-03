#!/bin/bash

# Distribution script for starting Consul
# This script is called by Ansible deployment

set -e

# Configuration
CONSUL_CONFIG_DIR=${CONSUL_CONFIG_DIR:-"/etc/consul.d"}
LOGS_DIR=${LOGS_DIR:-"/opt/order-services/logs"}

echo "Starting Consul agent..."

# Kill any existing Consul processes
pkill -f "consul agent" || true

# Start Consul agent
nohup consul agent -config-dir="$CONSUL_CONFIG_DIR" > "$LOGS_DIR/consul.log" 2>&1 &

echo "Consul agent started. PID: $!"
echo "Logs: $LOGS_DIR/consul.log"
echo "Consul UI: http://localhost:8500" 