#!/bin/bash

# Script to start multiple order service instances
# Usage: ./start-order-services.sh [num_instances] [start_ordinal]

set -e

# Default values
NUM_INSTANCES=${1:-3}
START_ORDINAL=${2:-1}

echo "Starting $NUM_INSTANCES order service instances starting from ordinal $START_ORDINAL..."

# Function to start a single instance
start_instance() {
    local ordinal=$1
    local tcp_port=$((8080 + ordinal - 1))
    local grpc_port=$((9090 + ordinal - 1))
    local fix_port=$((5001 + ordinal - 1))
    
    echo "Starting order-service-$ordinal on TCP:$tcp_port, gRPC:$grpc_port, FIX:$fix_port"
    
    # Set environment variables for this instance
    export ORDINAL=$ordinal
    export TCP_PORT=$tcp_port
    export GRPC_PORT=$grpc_port
    export FIX_SOCKET_PORT=$fix_port
    export FIX_CLIENT_ID="ORDER_SERVICE_$(printf "%03d" $ordinal)"
    
    # Start the service in background
    cd java-order-service
    ./gradlew bootRun > ../logs/order-service-$ordinal.log 2>&1 &
    cd ..
    
    # Store the PID
    echo $! > pids/order-service-$ordinal.pid
    echo "Started order-service-$ordinal with PID $(cat pids/order-service-$ordinal.pid)"
}

# Create necessary directories
mkdir -p logs pids

# Clean up any existing PIDs
rm -f pids/order-service-*.pid

# Start instances
for ((i=0; i<NUM_INSTANCES; i++)); do
    ordinal=$((START_ORDINAL + i))
    start_instance $ordinal
    
    # Small delay between starts
    sleep 2
done

echo ""
echo "Started $NUM_INSTANCES order service instances:"
for ((i=0; i<NUM_INSTANCES; i++)); do
    ordinal=$((START_ORDINAL + i))
    if [ -f "pids/order-service-$ordinal.pid" ]; then
        pid=$(cat pids/order-service-$ordinal.pid)
        echo "  order-service-$ordinal: PID $pid"
    fi
done

echo ""
echo "Logs are available in the logs/ directory"
echo "To stop all instances: ./stop-order-services.sh" 