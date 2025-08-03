#!/bin/bash

# Distribution script for stopping Consul
# This script is called by Ansible deployment

set -e

echo "Stopping Consul agent..."

# Kill Consul processes
pkill -f "consul agent" || true
sleep 2

# Force kill if still running
pkill -9 -f "consul agent" || true

echo "Consul agent stopped" 