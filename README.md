# Consul Demo - Order Service & Asgard Admin

This project demonstrates a microservices architecture with Consul service discovery, featuring a Java order service and a Go CLI admin tool.

## Project Structure

```
consul-demo/
├── java-order-service/          # Java 17 service with gRPC and TCP
│   ├── build.gradle            # Gradle build configuration
│   ├── src/main/
│   │   ├── java/com/example/orderservice/
│   │   │   ├── OrderServiceApplication.java
│   │   │   ├── OrderManager.java
│   │   │   ├── grpc/GrpcServer.java
│   │   │   ├── tcp/TcpServer.java
│   │   │   └── consul/ConsulRegistration.java
│   │   ├── proto/order_service.proto
│   │   └── resources/logback.xml
│   └── README.md
├── go-asgard-admin/            # Go CLI tool for service management
│   ├── go.mod
│   ├── main.go
│   ├── cmd/
│   │   ├── watch.go
│   │   ├── list.go
│   │   └── execute.go
│   └── proto/order_service.proto
└── README.md
```

## Prerequisites

- Java 17
- Go 1.21+
- Consul (running locally on localhost:8500)
- Gradle

## Quick Start

### 1. Start Consul

First, ensure Consul is running locally:

```bash
# Install Consul (if not already installed)
brew install consul

# Start Consul in dev mode
consul agent -dev
```

### 2. Build and Run Java Order Service

```bash
# Build the project
./gradlew :java-order-service:build

# Run the service
./gradlew :java-order-service:run
```

The Java service will:
- Start on TCP port 8080
- Start gRPC server on port 9090
- Register with Consul automatically
- Generate mock orders every 10 seconds
- Expose health checks for both TCP and gRPC

### 3. Build and Run Go CLI Tool

```bash
cd go-asgard-admin

# Build the CLI tool
go build -o asgard-admin

# Run the CLI tool
./asgard-admin --help
```

## CLI Commands

### Watch Services

Monitor services in real-time as they come in and out of Consul:

```bash
# Watch all services
./asgard-admin watch

# Watch specific service
./asgard-admin watch --service order-service

# Watch with custom Consul address
./asgard-admin watch --consul localhost:8500
```

### Discover Services

Discover all services and their online instances from Consul:

```bash
# Discover all services
./asgard-admin discover

# Discover only online instances
./asgard-admin discover --online-only

# Discover with custom Consul address
./asgard-admin discover --consul localhost:8500
```

### Interactive Admin Tool

Use the interactive admin tool to discover services, select instances, and execute gRPC functions:

```bash
# Start interactive admin
./asgard-admin admin

# With custom Consul address
./asgard-admin admin --consul localhost:8500
```

The interactive admin tool will:
1. Show all available services
2. Let you select a service
3. Show online instances for that service
4. Let you select an instance
5. Show available gRPC methods
6. Let you select and execute a method with arguments

## Service Features

### Java Order Service

- **TCP Server**: Exposes order data on port 8080
- **gRPC Server**: Admin interface on port 9090
- **Consul Integration**: Automatic registration with health checks
- **Mock Data**: Generates random orders automatically
- **Health Checks**: TCP and gRPC health monitoring

### Go Asgard Admin CLI

- **Real-time Monitoring**: Watch services coming in/out of Consul
- **Service Discovery**: Find and list service instances
- **gRPC Function Listing**: Discover available methods
- **Remote Execution**: Execute gRPC functions via CLI
- **Colored Output**: Beautiful terminal interface

## API Endpoints

### gRPC Methods

- `AddOrder(customer_id, product_id, quantity, price)` - Create new order
- `CancelOrder(order_id)` - Cancel existing order
- `GetOrder(order_id)` - Retrieve order details
- `ListOrders(limit, offset)` - List orders with pagination
- `GetServiceInfo()` - Get service status and info

### TCP Interface

Connect to `localhost:8080` to get:
- Service connection confirmation
- Total order count
- All orders as JSON

## Development

### Java Service Development

```bash
# Run with hot reload
./gradlew :java-order-service:run

# Build JAR
./gradlew :java-order-service:jar

# Run tests
./gradlew :java-order-service:test
```

### Go CLI Development

```bash
cd go-asgard-admin

# Run directly
go run main.go watch

# Build for different platforms
GOOS=linux GOARCH=amd64 go build -o asgard-admin-linux
GOOS=darwin GOARCH=amd64 go build -o asgard-admin-mac
```

## Demo Scenarios

### 1. Service Discovery Demo

1. Start Consul: `consul agent -dev`
2. Start Java service: `make run-java`
3. Discover services: `make discover`
4. Watch services: `make watch`

### 2. Interactive Admin Demo

1. Start Consul and Java service
2. Run interactive admin: `make admin`
3. Follow the prompts to:
   - Select a service
   - Choose an online instance
   - Pick a gRPC method
   - Execute with arguments

### 3. Service Health Demo

1. Start multiple Java service instances
2. Use `./asgard-admin discover --online-only` to see only healthy instances
3. Use interactive admin to execute functions on specific instances

## Troubleshooting

### Common Issues

1. **Consul Connection Failed**
   - Ensure Consul is running: `consul agent -dev`
   - Check Consul address: `./asgard-admin watch --consul localhost:8500`

2. **gRPC Connection Failed**
   - Verify Java service is running
   - Check gRPC port in Consul metadata
   - Ensure firewall allows connections

3. **Build Issues**
   - Java: Ensure Java 17 is installed
   - Go: Ensure Go 1.21+ is installed
   - Gradle: Run `./gradlew clean build`

### Logs

- Java service logs are in console with logback configuration
- Go CLI provides colored output for different operations
- Consul logs can be viewed in Consul UI at `http://localhost:8500`

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Consul        │    │  Java Service   │    │  Go CLI Tool    │
│   (Discovery)   │◄──►│  (Order Mgmt)   │◄──►│  (Admin)        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
   Service Catalog         TCP:8080, gRPC:9090    Remote Execution
   Health Monitoring       Mock Order Generation    Service Discovery
```

This demo showcases:
- Service registration and discovery with Consul
- Health monitoring and failover
- gRPC communication between services
- CLI-based service management
- Real-time service monitoring 