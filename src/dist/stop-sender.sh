#!/bin/bash

# Distribution script for stopping Order Sender
# This script is called by Ansible deployment

set -e

echo "Stopping Order Sender..."

# Kill sender processes
pkill -f "order-sender" || true
sleep 2

# Force kill if still running
pkill -9 -f "order-sender" || true

echo "Order Sender stopped" 