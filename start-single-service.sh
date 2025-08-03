#!/bin/bash

# Script to start a single order service instance
# Usage: ./start-single-service.sh [ordinal]

set -e

ORDINAL=${1:-1}
FIX_PORT=$((8080 + ORDINAL - 1))
ADMIN_PORT=$((9090 + ORDINAL - 1))

echo "Starting order-service-$ORDINAL on FIX:$FIX_PORT, Admin:$ADMIN_PORT"

# Set environment variables for this instance
export ORDINAL=$ORDINAL
export FIX_PORT=$FIX_PORT
export ADMIN_PORT=$ADMIN_PORT
export FIX_CLIENT_ID="ORDER_SERVICE_$(printf "%03d" $ORDINAL)"

# Create necessary directories
mkdir -p logs pids

# Clean up any existing PID for this instance
rm -f pids/order-service-$ORDINAL.pid

# Start the service in background
cd java-order-service
../gradlew bootRun > ../logs/order-service-$ORDINAL.log 2>&1 &
cd ..
PID=$!

# Store the PID
echo $PID > pids/order-service-$ORDINAL.pid

# Wait a moment and check if the process is still running
sleep 5
if kill -0 $PID 2>/dev/null; then
    echo "Started order-service-$ORDINAL with PID $PID"
    echo "Logs: logs/order-service-$ORDINAL.log"
    echo "To stop: kill $PID"
else
    echo "Error: order-service-$ORDINAL failed to start. Check logs/order-service-$ORDINAL.log"
    rm -f pids/order-service-$ORDINAL.pid
    exit 1
fi 