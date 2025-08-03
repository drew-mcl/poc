#!/bin/bash

echo "Testing FIX connection to Java Order Service..."
echo "Connecting to localhost:8080..."

# Use netcat to connect and read response
nc -w 5 localhost 8080 << EOF
EOF

if [ $? -eq 0 ]; then
    echo "✅ FIX connection successful!"
else
    echo "❌ FIX connection failed. Make sure the Java service is running."
    echo "Run: make run-java"
fi 