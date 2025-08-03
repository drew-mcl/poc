# Order Sender/Receiver POC

A proof-of-concept demonstrating service discovery and TCP-based order communication between microservices using Consul.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Order Sender  │    │   Consul        │    │ Order Receiver  │
│                 │    │   Service       │    │                 │
│ • Discovers     │◄──►│   Discovery     │◄──►│ • Accepts       │
│   receivers     │    │                 │    │   orders        │
│ • Sends orders  │    │ • Health checks │    │ • Processes     │
│ • Handles       │    │ • Load balancing│    │ • Accept/Reject │
│   responses     │    │                 │    │ • Reports back  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Components

### Order Sender
- **Purpose**: Discovers receivers from Consul and sends mock orders
- **Features**:
  - Dynamic service discovery via Consul
  - Mock order generation with realistic trading data
  - TCP communication with receivers
  - Response handling and logging
  - Configurable intervals and timeouts

### Order Receiver
- **Purpose**: Accepts orders via TCP and responds with FILLED/REJECTED
- **Features**:
  - TCP server for order acceptance
  - Consul registration with health checks
  - Configurable accept/reject mode
  - Order processing and response generation
  - Service metadata for discovery

## Quick Start

### Prerequisites
- Java 11+
- Consul (will be installed automatically)
- Ansible (for deployment)

### Option 1: Local Development (Recommended for Demo)

**Start the demo with 1 sender and 2 receivers:**
```bash
./start-local-demo.sh
```

**Stop the demo:**
```bash
./stop-local-demo.sh
```

This will:
- Start Consul agent in dev mode
- Start 2 Order Receivers (ports 9000, 9001)
- Start 1 Order Sender (discovers both receivers)
- Test the setup automatically

### Option 2: Manual Local Development

1. **Build the services**:
   ```bash
   cd order-sender && ./gradlew build
   cd ../order-receiver && ./gradlew build
   ```

2. **Start Consul** (if not already running):
   ```bash
   consul agent -dev
   ```

3. **Start the receiver**:
   ```bash
   cd order-receiver
   ./gradlew run
   ```

4. **Start the sender**:
   ```bash
   cd order-sender
   ./gradlew run
   ```

### Option 3: Ansible Deployment

**Deploy using Ansible:**
```bash
ansible-playbook ansible/playbook.yml -i ansible/inventory.ini
```

**Shutdown using Ansible:**
```bash
ansible-playbook ansible/shutdown.yml -i ansible/inventory.ini
```

The Ansible playbook will:
- Install Java and required packages
- Download and install Consul
- Build and deploy both services
- Start all services automatically
- Test the deployment

## Configuration

### Order Sender Environment Variables
```bash
SENDER_ID=order-sender-1                    # Unique sender identifier
CONSUL_HOST=localhost                       # Consul host
CONSUL_PORT=8500                           # Consul port
RECEIVER_SERVICE_NAME=order-receiver        # Service name to discover
MOCK_ORDER_ENABLED=true                    # Enable mock order generation
MOCK_ORDER_INITIAL_DELAY=5                 # Initial delay before sending
MOCK_ORDER_INTERVAL=15                     # Interval between orders (seconds)
TCP_TIMEOUT_MS=5000                        # TCP connection timeout
TCP_RETRY_ATTEMPTS=3                       # Number of retry attempts
```

### Order Receiver Environment Variables
```bash
SERVICE_NAME=order-receiver                 # Service name for Consul
ORDINAL=1                                  # Instance ordinal
SERVICE_ID=order-receiver-1                # Unique service identifier
TCP_PORT=9000                              # TCP port for order acceptance
CONSUL_HOST=localhost                      # Consul host
CONSUL_PORT=8500                           # Consul port
```

## TCP Protocol

### Order Message Format
```
ORDER|orderId|symbol|side|quantity|price|account|exchange|timestamp
```

Example:
```
ORDER|SENDER-000001|AAPL|BUY|100|150.50|TRADER001|NASDAQ|20241201-14:30:45.123
```

### Response Format
```
orderId|status|message
```

Examples:
```
SENDER-000001|FILLED|Order filled successfully
SENDER-000002|REJECTED|Order rejected by receiver
```

## Service Discovery

The sender discovers receivers using Consul's health check API:

1. **Query healthy services**: `GET /v1/health/service/order-receiver`
2. **Extract connection info**: Address, port from service metadata
3. **Connect and send**: TCP connection to each discovered receiver

## Monitoring

### Logs
- **Local demo logs**: `logs/` directory
  - `consul.log` - Consul agent logs
  - `order-sender.log` - Sender logs
  - `order-receiver-1.log` - Receiver 1 logs
  - `order-receiver-2.log` - Receiver 2 logs
- **Ansible deployment logs**: `/opt/order-services/logs/`

### Consul UI
- **URL**: http://localhost:8500
- **Services**: View registered services and health status
- **Health checks**: Monitor service health

### Process Management
```bash
# Check running processes
ps aux | grep -E "(order-sender|order-receiver|consul)"

# View logs in real-time
tail -f logs/order-sender.log
tail -f logs/order-receiver-1.log
tail -f logs/order-receiver-2.log
```

## Testing

### Manual Testing
```bash
# Test TCP communication directly
echo "ORDER|TEST-001|AAPL|BUY|100|150.50|TRADER001|NASDAQ|$(date +%Y%m%d-%H:%M:%S.000)" | nc localhost 9000
```

### Automated Testing
```bash
# Run the test script
./test-sender-receiver.sh
```

## Scaling

### Multiple Receivers (Local)
The local demo script starts 2 receivers by default. To add more:

```bash
# Edit start-local-demo.sh to add more receivers
# Copy the receiver startup section and change ports
```

### Multiple Receivers (Ansible)
To deploy multiple receivers, modify the Ansible playbook:

```yaml
# In ansible/playbook.yml
vars:
  receiver_instances:
    - ordinal: 1
      tcp_port: 9000
    - ordinal: 2
      tcp_port: 9001
    - ordinal: 3
      tcp_port: 9002
```

### Multiple Senders
To deploy multiple senders:

```yaml
# In ansible/playbook.yml
vars:
  sender_instances:
    - ordinal: 1
      mock_order_interval: 15
    - ordinal: 2
      mock_order_interval: 20
```

## Troubleshooting

### Common Issues

1. **Services not discovering each other**:
   - Check Consul is running: `curl http://localhost:8500/v1/status/leader`
   - Verify service registration: `curl http://localhost:8500/v1/catalog/services`

2. **TCP connection failures**:
   - Check port is listening: `netstat -tuln | grep 9000`
   - Verify firewall settings
   - Check service logs for errors

3. **Build failures**:
   - Ensure Java 11+ is installed: `java -version`
   - Check Gradle wrapper: `./gradlew --version`

4. **Process not starting**:
   - Check logs for errors
   - Verify environment variables
   - Check port availability

### Debug Mode
Enable debug logging by modifying `logback.xml`:
```xml
<root level="DEBUG">
```

### Clean Restart
If services get stuck:
```bash
# Stop everything
./stop-local-demo.sh

# Clean up any remaining processes
pkill -9 -f "order-sender\|order-receiver\|consul"

# Restart
./start-local-demo.sh
```

## Development

### Adding New Features

1. **New order types**: Extend `OrderMessage` class
2. **Additional metadata**: Modify Consul registration
3. **Custom health checks**: Add new health check types
4. **Load balancing**: Implement round-robin or weighted selection

### Performance Tuning

1. **Connection pooling**: Implement TCP connection pooling
2. **Batch processing**: Send orders in batches
3. **Async processing**: Use async/await patterns
4. **Caching**: Cache discovered services

## Security Considerations

- **Network security**: Use TLS for TCP communication
- **Authentication**: Implement service-to-service authentication
- **Authorization**: Add role-based access control
- **Encryption**: Encrypt sensitive order data

## Future Enhancements

1. **Message queuing**: Integrate with RabbitMQ/Kafka
2. **Database persistence**: Store orders in PostgreSQL
3. **API gateway**: Add REST API endpoints
4. **Metrics**: Integrate with Prometheus/Grafana
5. **Distributed tracing**: Add OpenTelemetry support 