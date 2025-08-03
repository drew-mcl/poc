# Consul Demo - Order Service

This demo shows a simplified order service with Consul health checks and failure simulation capabilities.

## Architecture

The service has been simplified to two ports:

- **FIX Port (8080)**: Main business port for FIX order processing
  - TCP health check in Consul
  - Can be toggled to simulate network failures
  - This is the primary health check port

- **Admin Port (9090)**: Management interface for service control
  - gRPC health check in Consul
  - Can be toggled to simulate admin interface failures
  - Used for service management and monitoring

## Key Features

### Health Check Scenarios

1. **FIX Server Failure (Network Issue)**
   - Disable FIX port to simulate network failure
   - Consul marks service as "critical"
   - Service becomes unavailable for order processing

2. **Admin Server Failure**
   - Disable admin port to simulate management interface failure
   - Consul marks service as "critical"
   - Service is still functional but management interface is down

3. **Business Logic Failure (Order Rejection)**
   - Enable order rejection mode
   - **Does NOT affect Consul health checks**
   - Service remains "healthy" but rejects all orders
   - Demonstrates difference between infrastructure and business health

### Demo Scripts

- `demo-health-checks.sh`: Interactive demo showing all scenarios
- `test-health-checks.sh`: Automated test of all scenarios
- `test-admin.sh`: Test admin functionality
- `test-fix.sh`: Test FIX port connectivity

## Running the Demo

1. Start Consul:
   ```bash
   consul agent -dev
   ```

2. Start the order service:
   ```bash
   ./start-order-services.sh
   ```

3. Run the demo:
   ```bash
   ./demo-health-checks.sh
   ```

## Health Check Configuration

The service registers with Consul with three health checks:

1. **TCP Check**: `localhost:8080` (FIX port)
2. **gRPC Check**: `localhost:9090/order.OrderAdminService/getServiceInfo` (Admin port)
3. **TTL Check**: 15-second TTL for additional monitoring

## Service Management

### Via gRPC Admin Interface

```bash
# Toggle FIX server (affects TCP health check)
grpcurl -plaintext -d '{"enabled": false}' localhost:9090 order.OrderAdminService/toggleFix

# Toggle admin server (affects gRPC health check)
grpcurl -plaintext -d '{"enabled": false}' localhost:9090 order.OrderAdminService/toggleAdmin

# Reject all orders (business logic - doesn't affect health)
grpcurl -plaintext localhost:9090 order.OrderAdminService/rejectAllOrders

# Accept all orders
grpcurl -plaintext localhost:9090 order.OrderAdminService/acceptAllOrders
```

## Consul UI

In the Consul UI, you'll see:

- **Service Status**: 
  - "Passing" when both ports are healthy
  - "Critical" when either port is down
  - "Warning" for TTL check issues

- **Health Checks**:
  - TCP check for FIX port
  - gRPC check for admin port
  - TTL check for additional monitoring

## Port Configuration

Default ports (can be overridden with environment variables):

- `FIX_PORT`: 8080 + (ORDINAL - 1)
- `ADMIN_PORT`: 9090 + (ORDINAL - 1)

For multiple instances, each gets unique ports based on ORDINAL. 