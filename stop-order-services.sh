#!/bin/bash

# Script to stop all order service instances
# Usage: ./stop-order-services.sh

set -e

echo "Stopping all order service instances..."

# Check if pids directory exists
if [ ! -d "pids" ]; then
    echo "No PID files found. No instances to stop."
    exit 0
fi

# Stop each instance
for pid_file in pids/order-service-*.pid; do
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        service_name=$(basename "$pid_file" .pid)
        
        echo "Stopping $service_name (PID: $pid)..."
        
        # Try graceful shutdown first
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid"
            
            # Wait for graceful shutdown
            for i in {1..10}; do
                if ! kill -0 "$pid" 2>/dev/null; then
                    echo "  $service_name stopped gracefully"
                    break
                fi
                sleep 1
            done
            
            # Force kill if still running
            if kill -0 "$pid" 2>/dev/null; then
                echo "  Force killing $service_name..."
                kill -9 "$pid"
                echo "  $service_name force stopped"
            fi
        else
            echo "  $service_name already stopped"
        fi
        
        # Remove PID file
        rm -f "$pid_file"
    fi
done

echo ""
echo "All order service instances stopped."
echo "Logs are available in the logs/ directory" 