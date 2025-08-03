#!/bin/bash

# Distribution script for stopping Order Receiver
# This script is called by Ansible deployment

set -e

echo "Stopping Order Receiver..."

# Kill receiver processes
pkill -f "order-receiver" || true
sleep 2

# Force kill if still running
pkill -9 -f "order-receiver" || true

echo "Order Receiver stopped" 