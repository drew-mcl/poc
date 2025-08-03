# Asgard Admin - Service Discovery & gRPC Admin Tool

A comprehensive service discovery and admin tool for Consul that allows you to discover services and interact with their gRPC methods dynamically.

## Features

- 🔍 **Service Discovery**: Discover all services registered with Consul
- 📊 **Service Discovery**: Discover and list service instances
- 🔌 **gRPC Integration**: Connect to services via gRPC reflection
- 🛠️ **Dynamic Method Execution**: Execute gRPC methods interactively
- 🏷️ **Metadata Support**: Extract admin ports, gRPC ports from service metadata
- 🤖 **Auto-selection**: Automatically select services/instances for scripting
- 📈 **Real-time Monitoring**: Watch services as they come online/offline

## Installation

```bash
cd go-asgard-admin
go build -o asgard-admin
```

## Usage

### Basic Commands

```bash
# Discover all services
./asgard-admin discover

# Watch services in real-time
./asgard-admin watch

# Interactive admin mode
./asgard-admin admin
```

### Service Discovery

```bash
# Discover all services
./asgard-admin discover

# Show only online instances
./asgard-admin discover --online-only

# Filter by specific service
./asgard-admin discover --service order-service

# Use custom Consul address
./asgard-admin discover --consul consul.example.com:8500
```

### Real-time Monitoring

```bash
# Watch all services
./asgard-admin watch

# Watch specific service
./asgard-admin watch --service order-service

# Use custom Consul address
./asgard-admin watch --consul consul.example.com:8500
```

### Interactive Admin Mode

```bash
# Full interactive mode
./asgard-admin admin

# Target specific service
./asgard-admin admin --service order-service

# Target specific instance
./asgard-admin admin --service order-service --instance order-service-1

# Auto-select first available service/instance
./asgard-admin admin --auto-select

# Use custom Consul address
./asgard-admin admin --consul consul.example.com:8500
```

## Service Metadata Integration

The admin tool automatically extracts useful information from service metadata:

### Admin Port Detection
Services can register with an `admin-port` metadata field:
```json
{
  "ServiceMeta": {
    "admin-port": "8080",
    "grpc-port": "9090"
  }
}
```

### gRPC Port Detection
Services can register with a `grpc-port` metadata field for gRPC connections:
```json
{
  "ServiceMeta": {
    "grpc-port": "9090"
  }
}
```

## Example Workflow

1. **Discover Services**:
   ```bash
   ./asgard-admin discover
   ```

2. **Watch for Changes**:
   ```bash
   ./asgard-admin watch --service order-service
   ```

3. **Interactive Admin**:
   ```bash
   ./asgard-admin admin --service order-service
   ```

4. **Auto-execution**:
   ```bash
   ./asgard-admin admin --service order-service --auto-select
   ```

## Service Registration Example

For services to work optimally with the admin tool, register them with appropriate metadata:

```go
// Example service registration with metadata
service := &api.AgentServiceRegistration{
    ID:      "order-service-1",
    Name:    "order-service",
    Address: "192.168.1.100",
    Port:    8080,
    Meta: map[string]string{
        "admin-port": "8080",
        "grpc-port":  "9090",
        "version":    "1.0.0",
    },
    Tags: []string{"api", "grpc", "production"},
}
```

## Command Line Options

### Global Options
- `--consul, -c`: Consul address (default: localhost:8500)

### Discover Command
- `--online-only, -o`: Show only online instances
- `--service, -s`: Filter by specific service name

### Watch Command
- `--service, -s`: Filter by specific service name

### Admin Command
- `--service, -s`: Target specific service (skip selection)
- `--instance, -i`: Target specific instance (skip selection)
- `--auto-select, -a`: Auto-select first available service/instance

## Integration with Existing Services

The admin tool integrates seamlessly with existing Consul-registered services:

1. **Automatic Discovery**: Discovers all services registered with Consul
2. **Service Discovery**: Shows all registered service instances
3. **Metadata Extraction**: Automatically extracts admin/gRPC ports from metadata
4. **gRPC Reflection**: Uses gRPC reflection to discover available methods
5. **Dynamic Execution**: Allows interactive execution of gRPC methods

## Building and Running

```bash
# Build the tool
go build -o asgard-admin

# Run with default settings
./asgard-admin discover

# Run with custom Consul
./asgard-admin admin --consul my-consul:8500
```

## Dependencies

- `github.com/hashicorp/consul/api` - Consul client
- `github.com/spf13/cobra` - CLI framework
- `google.golang.org/grpc` - gRPC client
- `github.com/jhump/protoreflect` - gRPC reflection
- `github.com/fatih/color` - Colored output 