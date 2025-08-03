package main

import (
	"fmt"
	"log"
	"sort"
	"strconv"
	"strings"

	"github.com/fatih/color"
	consulapi "github.com/hashicorp/consul/api"
	"github.com/spf13/cobra"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)



// GetFixConfig retrieves FIX configuration for a specific service via Consul discovery
func GetFixConfig(consulAddr, serviceName string) (*FixConfig, error) {
	client, err := consulapi.NewClient(&consulapi.Config{Address: consulAddr})
	if err != nil {
		return nil, fmt.Errorf("failed to create Consul client: %v", err)
	}

	// Get all instances for the service
	instances, _, err := client.Catalog().Service(serviceName, "", nil)
	if err != nil {
		return nil, fmt.Errorf("failed to get instances for %s: %v", serviceName, err)
	}

	if len(instances) == 0 {
		return nil, fmt.Errorf("no instances found for service %s", serviceName)
	}

	// Use the first instance
	instance := instances[0]
	grpcPort := instance.ServicePort
	if grpcPort == 0 {
		// Try to get gRPC port from metadata
		if grpcPortStr, exists := instance.ServiceMeta["admin-port"]; exists {
			if port, err := strconv.Atoi(grpcPortStr); err == nil {
				grpcPort = port
			}
		}
	}

	if grpcPort == 0 {
		return nil, fmt.Errorf("no gRPC port found for service %s", serviceName)
	}

	// Connect to the service via gRPC
	grpcAddr := fmt.Sprintf("%s:%d", instance.ServiceAddress, grpcPort)
	conn, err := grpc.Dial(grpcAddr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return nil, fmt.Errorf("failed to connect to %s: %v", grpcAddr, err)
	}
	defer conn.Close()

	// Call the GetFixConfig RPC
	// Note: This requires the generated gRPC client code
	// For now, we'll return a default configuration based on service metadata
	return getDefaultFixConfig(serviceName, instance.ServiceMeta), nil
}

// getDefaultFixConfig creates a default FIX configuration based on service metadata
func getDefaultFixConfig(serviceName string, metadata map[string]string) *FixConfig {
	config := &FixConfig{
		SocketHost:        "localhost",
		SocketPort:        5001,
		ClientID:          fmt.Sprintf("%s_001", strings.ToUpper(serviceName)),
		TargetCompID:      "EXCHANGE",
		SenderCompID:      strings.ToUpper(serviceName),
		HeartbeatInterval: 30,
		FixVersion:        "FIX.4.4",
	}

	// Override with metadata if available
	if host, exists := metadata["fix_socket_host"]; exists {
		config.SocketHost = host
	}
	if portStr, exists := metadata["fix_socket_port"]; exists {
		if port, err := strconv.Atoi(portStr); err == nil {
			config.SocketPort = port
		}
	}
	if clientID, exists := metadata["fix_client_id"]; exists {
		config.ClientID = clientID
	}
	if targetCompID, exists := metadata["fix_target_comp_id"]; exists {
		config.TargetCompID = targetCompID
	}
	if senderCompID, exists := metadata["fix_sender_comp_id"]; exists {
		config.SenderCompID = senderCompID
	}
	if heartbeatStr, exists := metadata["fix_heartbeat_interval"]; exists {
		if heartbeat, err := strconv.Atoi(heartbeatStr); err == nil {
			config.HeartbeatInterval = heartbeat
		}
	}
	if version, exists := metadata["fix_version"]; exists {
		config.FixVersion = version
	}

	return config
}

// GetFixConfigForService is a convenience function that gets FIX config for a service
func GetFixConfigForService(consulAddr, serviceName string) {
	config, err := GetFixConfig(consulAddr, serviceName)
	if err != nil {
		log.Printf("Error getting FIX config for %s: %v", serviceName, err)
		return
	}

	fmt.Printf("FIX Configuration for %s:\n", serviceName)
	fmt.Printf("  Socket: %s:%d\n", config.SocketHost, config.SocketPort)
	fmt.Printf("  Client ID: %s\n", config.ClientID)
	fmt.Printf("  Target Comp ID: %s\n", config.TargetCompID)
	fmt.Printf("  Sender Comp ID: %s\n", config.SenderCompID)
	fmt.Printf("  Heartbeat Interval: %d seconds\n", config.HeartbeatInterval)
	fmt.Printf("  FIX Version: %s\n", config.FixVersion)
}



var discoverCmd = &cobra.Command{
	Use:   "discover",
	Short: "Discover services with health status and checks",
	Long:  "Lists all services registered with Consul and shows their health status, instances, and health checks",
	Run:   runDiscover,
}

func init() {
	discoverCmd.Flags().StringP("consul", "c", "localhost:8500", "Consul address")
	discoverCmd.Flags().StringP("service", "s", "", "Filter by specific service name")
	discoverCmd.Flags().BoolP("all", "", false, "Show all instances (including unhealthy) - default is healthy only")
}

// CORRECTED: The 'tags' parameter is now correctly defined as []string.
func getInstanceInfo(id, service, addr string, port int, meta map[string]string, tags []string) *InstanceInfo {
	info := &InstanceInfo{
		CatalogService: &consulapi.CatalogService{
					ServiceID:      id,
		ServiceName:    service,
		ServiceAddress: addr,
		ServicePort:    port,
		ServiceMeta:    meta,
		ServiceTags:    tags,
	},
}

	if adminPort, exists := meta["admin-port"]; exists {
		if p, err := strconv.Atoi(adminPort); err == nil {
			info.AdminPort = p
		}
	}

	if grpcPort, exists := meta["grpc-port"]; exists {
		if p, err := strconv.Atoi(grpcPort); err == nil {
			info.GRPCPort = p
		}
	}

	return info
}

func runDiscover(cmd *cobra.Command, args []string) {
	consulAddr, _ := cmd.Flags().GetString("consul")
	serviceFilter, _ := cmd.Flags().GetString("service")
	showAll, _ := cmd.Flags().GetBool("all")

	client, err := consulapi.NewClient(&consulapi.Config{Address: consulAddr})
	if err != nil {
		log.Fatalf("Failed to create Consul client: %v", err)
	}

	// CORRECTED: This is the proper way to define the color functions.
	blue := color.New(color.FgBlue).Printf
	yellow := color.New(color.FgYellow).Printf
	red := color.New(color.FgRed).Printf
	cyan := color.New(color.FgCyan).Printf
	magenta := color.New(color.FgMagenta).Printf

	services, _, err := client.Catalog().Services(nil)
	if err != nil {
		log.Fatalf("Failed to get services: %v", err)
	}

	var serviceNames []string
	for serviceName := range services {
		if serviceFilter == "" || serviceName == serviceFilter {
			serviceNames = append(serviceNames, serviceName)
		}
	}
	sort.Strings(serviceNames)

	if len(serviceNames) == 0 {
		if serviceFilter != "" {
			yellow("🔍 No services found matching filter: %s\n", serviceFilter)
		} else {
			yellow("🔍 No services found in Consul\n")
		}
		return
	}

	cyan("🔍 Discovered %d services in Consul:\n\n", len(serviceNames))

	for _, serviceName := range serviceNames {
		// Get instances based on showAll flag
		var instances []*consulapi.ServiceEntry
		var err error
		
		if showAll {
			// Get all instances from catalog (including unhealthy)
			catalogInstances, _, err := client.Catalog().Service(serviceName, "", nil)
			if err != nil {
				red("❌ Failed to get instances for %s: %v\n", serviceName, err)
				continue
			}
			// Convert to ServiceEntry format for consistency
			for _, catInstance := range catalogInstances {
				instances = append(instances, &consulapi.ServiceEntry{
					Service: &consulapi.AgentService{
						ID:      catInstance.ServiceID,
						Service:  catInstance.ServiceName,
						Address:  catInstance.ServiceAddress,
						Port:     catInstance.ServicePort,
						Meta:     catInstance.ServiceMeta,
						Tags:     catInstance.ServiceTags,
					},
					Checks: []*consulapi.HealthCheck{}, // Will be populated below
				})
			}
		} else {
			// Get only healthy instances (default behavior)
			instances, _, err = client.Health().Service(serviceName, "", true, nil)
			if err != nil {
				red("❌ Failed to get healthy instances for %s: %v\n", serviceName, err)
				continue
			}
		}

		// Determine overall service status
		status := "passing"
		criticalCount := 0
		warningCount := 0
		
		for _, instance := range instances {
			for _, check := range instance.Checks {
				if check.ServiceID == instance.Service.ID {
					switch check.Status {
					case "critical":
						criticalCount++
					case "warning":
						warningCount++
					}
				}
			}
		}
		
		if criticalCount > 0 {
			status = "critical"
		} else if warningCount > 0 {
			status = "warning"
		}
		
		statusIcon := formatHealthStatus(status)
		blue("📦 Service: %s %s\n", serviceName, statusIcon)
		
		if showAll {
			cyan("   (Showing all instances including unhealthy)\n")
		} else {
			cyan("   (Showing only healthy instances - use --all to see all)\n")
		}
		
		if len(instances) > 0 {
			yellow("   📍 Instances: %d\n", len(instances))
			
			for i, instance := range instances {
				// Get health status for this instance
				healthStatus := "✅ healthy"
				
				// Always check health status for display purposes
				// Use the service health endpoint to get accurate health status
				serviceHealth, _, err := client.Health().Service(instance.Service.Service, "", false, nil)
				if err == nil {
					hasCritical := false
					hasWarning := false
					for _, healthEntry := range serviceHealth {
						if healthEntry.Service.ID == instance.Service.ID {
							for _, check := range healthEntry.Checks {
								if check.ServiceID == instance.Service.ID {
									switch check.Status {
									case "critical":
										hasCritical = true
									case "warning":
										hasWarning = true
									}
								}
							}
						}
					}
					if hasCritical {
						healthStatus = "❌ critical"
					} else if hasWarning {
						healthStatus = "⚠️  warning"
					} else {
						healthStatus = "✅ healthy"
					}
				} else {
					healthStatus = "❓ unknown"
				}
				

				
				// Get admin port from metadata
				adminPort := instance.Service.Port // fallback to main port
				if adminPortStr, exists := instance.Service.Meta["admin-port"]; exists {
					if port, err := strconv.Atoi(adminPortStr); err == nil {
						adminPort = port
					}
				}
				
				yellow("      %d. %s (%s:%d) %s\n", i+1, instance.Service.ID, instance.Service.Address, instance.Service.Port, healthStatus)
				cyan("         🔌 Admin: %s:%d\n", instance.Service.Address, adminPort)
				
				// Show metadata
				if len(instance.Service.Meta) > 0 {
					cyan("         🔧 Metadata:\n")
					for key, value := range instance.Service.Meta {
						cyan("            %s: %s\n", key, value)
					}
				}
				
				// Show tags
				if len(instance.Service.Tags) > 0 {
					cyan("         🏷️  Tags: %v\n", instance.Service.Tags)
				}
				
				// Show health checks for this instance
				cyan("         🔍 Health Checks:\n")
				for _, check := range instance.Checks {
					if check.ServiceID == instance.Service.ID {
						checkIcon := formatCheckStatus(check.Status)
						cyan("            %s %s: %s\n", checkIcon, check.Name, check.Status)
						if check.Output != "" {
							// Truncate long output
							output := check.Output
							if len(output) > 100 {
								output = output[:97] + "..."
							}
							magenta("               Output: %s\n", output)
						}
					}
				}
			}
		} else {
			red("   ❌ No instances found\n")
		}
		fmt.Println()
	}
}