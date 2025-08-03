#!/bin/bash

# Script to check the status of order service instances
# Usage: ./check-services.sh

set -e

echo "=== Order Service Status Check ==="

# Check for running processes
echo "Checking for running order service processes..."
ps aux | grep -E "(gradle|java.*order-service)" | grep -v grep || echo "No order service processes found"

echo ""

# Check for PID files
if [ -d "pids" ]; then
    echo "PID files found:"
    for pid_file in pids/order-service-*.pid; do
        if [ -f "$pid_file" ]; then
            service_name=$(basename "$pid_file" .pid)
            pid=$(cat "$pid_file")
            if kill -0 "$pid" 2>/dev/null; then
                echo "  $service_name: PID $pid (RUNNING)"
            else
                echo "  $service_name: PID $pid (STOPPED)"
            fi
        fi
    done
else
    echo "No PID files found"
fi

echo ""

# Check port usage
echo "Checking port usage for order services:"
for port in 8080 8081 8082 9090 9091 9092 5001 5002 5003; do
    if lsof -i :$port >/dev/null 2>&1; then
        echo "  Port $port: IN USE"
        lsof -i :$port | grep LISTEN
    else
        echo "  Port $port: AVAILABLE"
    fi
done

echo ""

# Check logs directory
if [ -d "logs" ]; then
    echo "Log files found:"
    ls -la logs/order-service-*.log 2>/dev/null || echo "No log files found"
else
    echo "No logs directory found"
fi

echo "=== End Status Check ===" 