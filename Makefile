.PHONY: help build-java build-go run-java run-go watch demo clean

help: ## Show this help message
	@echo "Consul Demo - Available commands:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

build-java: ## Build the Java order service
	@echo "Building Java order service..."
	./gradlew :java-order-service:build

build-go: ## Build the Go CLI tool
	@echo "Building Go CLI tool..."
	cd go-asgard-admin && go build -o asgard-admin

build: build-java build-go ## Build both services

run-java: ## Run the Java order service
	@echo "Starting Java order service..."
	./gradlew :java-order-service:run

run-go: ## Run the Go CLI tool (interactive)
	@echo "Starting Go CLI tool..."
	cd go-asgard-admin && ./asgard-admin

watch: ## Watch services in Consul
	@echo "Watching services in Consul..."
	cd go-asgard-admin && ./asgard-admin watch

discover: ## Discover services and instances
	@echo "Discovering services and instances..."
	cd go-asgard-admin && ./asgard-admin discover

admin: ## Interactive admin tool
	@echo "Starting interactive admin tool..."
	cd go-asgard-admin && ./asgard-admin admin

demo: ## Run a complete demo
	@echo "Starting Consul demo..."
	@echo "1. Make sure Consul is running: consul agent -dev"
	@echo "2. In another terminal, run: make run-java"
	@echo "3. In another terminal, run: make discover"
	@echo "4. Try: make watch"
	@echo "5. Try: make admin (interactive)"

clean: ## Clean build artifacts
	@echo "Cleaning build artifacts..."
	./gradlew :java-order-service:clean
	cd go-asgard-admin && rm -f asgard-admin

install-deps: ## Install dependencies
	@echo "Installing dependencies..."
	@echo "Java 17 and Gradle should be installed"
	@echo "Go 1.21+ should be installed"
	@echo "Consul should be installed: brew install consul"

setup: install-deps build ## Setup the complete environment

test: ## Run tests
	@echo "Running tests..."
	./gradlew :java-order-service:test 