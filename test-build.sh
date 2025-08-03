#!/bin/bash

echo "=== Testing Build and Scripts ==="
echo ""

echo "1. Testing Java build..."
if ./gradlew :java-order-service:build; then
    echo "✅ Java build successful"
else
    echo "❌ Java build failed"
    exit 1
fi

echo ""
echo "2. Testing Go build..."
if cd go-asgard-admin && go build -o asgard-admin && cd ..; then
    echo "✅ Go build successful"
else
    echo "❌ Go build failed"
    exit 1
fi

echo ""
echo "3. Testing script syntax..."
for script in start-single-service.sh start-order-services.sh stop-order-services.sh test-fix.sh test-admin.sh test-health-checks.sh demo-health-checks.sh; do
    if bash -n "$script"; then
        echo "✅ $script syntax OK"
    else
        echo "❌ $script syntax error"
        exit 1
    fi
done

echo ""
echo "4. Testing Go CLI help..."
if cd go-asgard-admin && ./asgard-admin --help > /dev/null 2>&1; then
    echo "✅ Go CLI help works"
else
    echo "❌ Go CLI help failed"
    exit 1
fi

echo ""
echo "=== All tests passed! ==="
echo ""
echo "You can now:"
echo "- Start services: ./start-single-service.sh"
echo "- Watch services: cd go-asgard-admin && ./asgard-admin watch"
echo "- Discover services: cd go-asgard-admin && ./asgard-admin discover"
echo "- Run demo: ./demo-health-checks.sh" 