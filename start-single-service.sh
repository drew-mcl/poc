#!/bin/bash

# Script to start a single order service instance
# Usage: ./start-single-service.sh [ordinal]

set -e

ORDINAL=${1:-1}
TCP_PORT=$((8080 + ORDINAL - 1))
GRPC_PORT=$((9090 + ORDINAL - 1))
FIX_PORT=$((5001 + ORDINAL - 1))

echo "Starting order-service-$ORDINAL on TCP:$TCP_PORT, gRPC:$GRPC_PORT, FIX:$FIX_PORT"

# Set environment variables for this instance
export ORDINAL=$ORDINAL
export TCP_PORT=$TCP_PORT
export GRPC_PORT=$GRPC_PORT
export FIX_SOCKET_PORT=$FIX_PORT
export FIX_CLIENT_ID="ORDER_SERVICE_$(printf "%03d" $ORDINAL)"

# Create necessary directories
mkdir -p logs pids

# Clean up any existing PID for this instance
rm -f pids/order-service-$ORDINAL.pid

# Start the service in background
./gradlew :java-order-service:run > logs/order-service-$ORDINAL.log 2>&1 &
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