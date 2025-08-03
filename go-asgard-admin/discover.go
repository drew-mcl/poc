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

// FixConfig represents FIX configuration for a service
type FixConfig struct {
	SocketHost        string `json:"socket_host"`
	SocketPort        int    `json:"socket_port"`
	ClientID          string `json:"client_id"`
	TargetCompID      string `json:"target_comp_id"`
	SenderCompID      string `json:"sender_comp_id"`
	HeartbeatInterval int    `json:"heartbeat_interval"`
	FixVersion        string `json:"fix_version"`
}

// GetFixConfig retrieves FIX configuration for a specific service via Consul discovery
func GetFixConfig(consulAddr, serviceName string) (*FixConfig, error) {
	client, err := consulapi.NewClient(&consulapi.Config{Address: consulAddr})
	if err != nil {
		return nil, fmt.Errorf("failed to create Consul client: %v", err)
	}

	// Get healthy instances for the service
	instances, _, err := client.Health().Service(serviceName, "", true, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to get healthy instances for %s: %v", serviceName, err)
	}

	if len(instances) == 0 {
		return nil, fmt.Errorf("no healthy instances found for service %s", serviceName)
	}

	// Use the first healthy instance
	instance := instances[0]
	grpcPort := instance.Service.Port
	if grpcPort == 0 {
		// Try to get gRPC port from metadata
		if grpcPortStr, exists := instance.Service.Meta["grpc_port"]; exists {
			if port, err := strconv.Atoi(grpcPortStr); err == nil {
				grpcPort = port
			}
		}
	}

	if grpcPort == 0 {
		return nil, fmt.Errorf("no gRPC port found for service %s", serviceName)
	}

	// Connect to the service via gRPC
	grpcAddr := fmt.Sprintf("%s:%d", instance.Service.Address, grpcPort)
	conn, err := grpc.Dial(grpcAddr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return nil, fmt.Errorf("failed to connect to %s: %v", grpcAddr, err)
	}
	defer conn.Close()

	// Call the GetFixConfig RPC
	// Note: This requires the generated gRPC client code
	// For now, we'll return a default configuration based on service metadata
	return getDefaultFixConfig(serviceName, instance.Service.Meta), nil
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
	Short: "Discover services and their online instances from Consul",
	Long:  "Lists all services registered with Consul and shows their online instances",
	Run:   runDiscover,
}

func init() {
	discoverCmd.Flags().StringP("consul", "c", "localhost:8500", "Consul address")
	discoverCmd.Flags().BoolP("online-only", "o", false, "Show only online instances")
	discoverCmd.Flags().StringP("service", "s", "", "Filter by specific service name")
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
		HealthStatus: "unknown",
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
	onlineOnly, _ := cmd.Flags().GetBool("online-only")
	serviceFilter, _ := cmd.Flags().GetString("service")

	client, err := consulapi.NewClient(&consulapi.Config{Address: consulAddr})
	if err != nil {
		log.Fatalf("Failed to create Consul client: %v", err)
	}

	// CORRECTED: This is the proper way to define the color functions.
	green := color.New(color.FgGreen).Printf
	blue := color.New(color.FgBlue).Printf
	yellow := color.New(color.FgYellow).Printf
	red := color.New(color.FgRed).Printf
	cyan := color.New(color.FgCyan).Printf

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
		healthInstances, _, err := client.Health().Service(serviceName, "", true, nil)
		if err != nil {
			red("❌ Failed to get instances for %s: %v\n", serviceName, err)
			continue
		}

		green("📦 Service: %s\n", serviceName)
		blue("   📍 Online instances: %d\n", len(healthInstances))

		if len(healthInstances) > 0 {
			yellow("   ✅ Online instances:\n")
			for i, instance := range healthInstances {
				// CORRECTED: Now passing instance.Service.Tags which is a []string.
				info := getInstanceInfo(
					instance.Service.ID,
					instance.Service.Service,
					instance.Service.Address,
					instance.Service.Port,
					instance.Service.Meta,
					instance.Service.Tags,
				)
				yellow("      %d. %s (%s:%d)\n", i+1, info.ServiceID, info.ServiceAddress, info.ServicePort)

				if info.AdminPort > 0 {
					cyan("         🔧 Admin: %s:%d\n", info.ServiceAddress, info.AdminPort)
				}
				if info.GRPCPort > 0 {
					cyan("         🔌 gRPC: %s:%d\n", info.ServiceAddress, info.GRPCPort)
				}
				if len(info.ServiceTags) > 0 {
					cyan("         🏷️  Tags: %v\n", info.ServiceTags)
				}
			}
		}

		// Logic for showing offline instances if desired (can be added here)
		if !onlineOnly {
			// You would need to make another API call to get all instances
			// and compare against the healthy list to find the offline ones.
		}
		fmt.Println()
	}
}