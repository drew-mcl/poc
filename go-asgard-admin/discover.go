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

// -----------------------------------------------------------------------------
// helper formatting
// -----------------------------------------------------------------------------
func formatHealthStatus(status string) string {
	switch status {
	case "passing":
		return color.HiGreenString("✔ PASS")
	case "warning":
		return color.HiYellowString("⚠ WARN")
	case "critical":
		return color.HiRedString("✖ FAIL")
	default:
		return color.HiWhiteString("? UNKNOWN")
	}
}

func formatCheckStatus(status string) string {
	switch status {
	case "passing":
		return color.GreenString("✔")
	case "warning":
		return color.YellowString("⚠")
	case "critical":
		return color.RedString("✖")
	default:
		return color.WhiteString("?")
	}
}

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

// -----------------------------------------------------------------------------
// cobra command definition
// -----------------------------------------------------------------------------
var discoverCmd = &cobra.Command{
	Use:   "discover",
	Short: "List Consul services and their health",
	Long:  "Lists all services registered with Consul and shows their health status, instances, and health checks",
	RunE:  runDiscover,
}

func init() {
	discoverCmd.Flags().StringP("consul", "c", "localhost:8500", "Consul HTTP address")
	discoverCmd.Flags().StringP("service", "s", "", "Filter by service name")
	discoverCmd.Flags().Bool("all", false, "Include unhealthy instances as well")
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

// -----------------------------------------------------------------------------
// command implementation
// -----------------------------------------------------------------------------
func runDiscover(cmd *cobra.Command, _ []string) error {
	consulAddr, _ := cmd.Flags().GetString("consul")
	filter, _ := cmd.Flags().GetString("service")
	showAll, _ := cmd.Flags().GetBool("all")

	client, err := consulapi.NewClient(&consulapi.Config{Address: consulAddr})
	if err != nil {
		return fmt.Errorf("create consul client: %w", err)
	}

	services, _, err := client.Catalog().Services(nil)
	if err != nil {
		return fmt.Errorf("list services: %w", err)
	}

	var names []string
	for name := range services {
		if name == "consul" {
			continue
		}
		if filter == "" || filter == name {
			names = append(names, name)
		}
	}
	sort.Strings(names)

	if len(names) == 0 {
		if filter != "" {
			color.Yellow("No services match %q", filter)
		} else {
			color.Yellow("No services found in Consul")
		}
		return nil
	}

	color.Cyan("Discovered %d service(s)\n", len(names))

	for _, name := range names {
		if err := renderService(client, name, showAll); err != nil {
			color.Red("   error: %v", err)
		}
	}
	return nil
}

// renderService prints one service block.
func renderService(client *consulapi.Client, service string, showAll bool) error {
	var entries []*consulapi.ServiceEntry
	var err error

	if showAll {
		cats, _, err := client.Catalog().Service(service, "", nil)
		if err != nil {
			return err
		}
		for _, c := range cats {
			entries = append(entries, &consulapi.ServiceEntry{Service: &consulapi.AgentService{
				ID:       c.ServiceID,
				Service:  c.ServiceName,
				Address:  c.ServiceAddress,
				Port:     c.ServicePort,
				Meta:     c.ServiceMeta,
				Tags:     c.ServiceTags,
			}})
		}
	} else {
		entries, _, err = client.Health().Service(service, "", true, nil)
		if err != nil {
			return err
		}
	}

	// aggregate health
	agg := "passing"
	for _, e := range entries {
		for _, ch := range e.Checks {
			if ch.ServiceID != e.Service.ID {
				continue
			}
			switch ch.Status {
			case "critical":
				agg = "critical"
			case "warning":
				if agg != "critical" {
					agg = "warning"
				}
			}
		}
	}

	fmt.Printf("\n%s %s\n", color.New(color.Bold, color.FgHiBlue).Sprint(service), formatHealthStatus(agg))

	if showAll {
		color.Cyan("   (Showing all instances including unhealthy)\n")
	} else {
		color.Cyan("   (Showing only healthy instances - use --all to see all)\n")
	}

	if len(entries) == 0 {
		color.Red("   no instances registered")
		return nil
	}

	for i, e := range entries {
		idx := color.HiMagentaString("#%d", i+1)
		addr := fmt.Sprintf("%s:%d", e.Service.Address, e.Service.Port)
		fmt.Printf("   %s %-30s %s\n", idx, addr, formatCheckStatus(instanceStatus(e)))

		// admin port
		adminPort := e.Service.Port
		if p, ok := e.Service.Meta["admin-port"]; ok {
			if n, err := strconv.Atoi(p); err == nil {
				adminPort = n
			}
		}
		fmt.Printf("      admin  : %s:%d\n", e.Service.Address, adminPort)

		// meta
		if len(e.Service.Meta) > 0 {
			keys := make([]string, 0, len(e.Service.Meta))
			for k := range e.Service.Meta {
				keys = append(keys, k)
			}
			sort.Strings(keys)
			var kv []string
			for _, k := range keys {
				kv = append(kv, fmt.Sprintf("%s=%s", k, e.Service.Meta[k]))
			}
			fmt.Printf("      meta   : %s\n", strings.Join(kv, ", "))
		}

		// tags
		if len(e.Service.Tags) > 0 {
			fmt.Printf("      tags   : %s\n", strings.Join(e.Service.Tags, ", "))
		}

		// checks
		for _, ch := range e.Checks {
			if ch.ServiceID != e.Service.ID {
				continue
			}
			glyph := formatCheckStatus(ch.Status)
			out := strings.TrimSpace(ch.Output)
			if len(out) > 60 {
				out = out[:57] + "..."
			}
			fmt.Printf("         %s %s (%s)\n", glyph, ch.Name, out)
		}
	}
	return nil
}

func instanceStatus(e *consulapi.ServiceEntry) string {
	crit, warn := false, false
	for _, ch := range e.Checks {
		if ch.ServiceID != e.Service.ID {
			continue
		}
		switch ch.Status {
		case "critical":
			crit = true
		case "warning":
			warn = true
		}
	}
	switch {
	case crit:
		return "critical"
	case warn:
		return "warning"
	default:
		return "passing"
	}
}