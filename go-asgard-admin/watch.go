package main

import (
	"fmt"
	"log"
	"time"

	"github.com/fatih/color"
	consulapi "github.com/hashicorp/consul/api"
	"github.com/spf13/cobra"
)

var watchCmd = &cobra.Command{
	Use:   "watch",
	Short: "Watch services in real-time with health status",
	Long:  "Continuously monitors Consul for service registration, deregistration, and health status changes.",
	Run:   runWatch,
}

func init() {
	watchCmd.Flags().StringP("consul", "c", "localhost:8500", "Consul address")
	watchCmd.Flags().StringP("service", "s", "", "Filter by specific service name")
}



func runWatch(cmd *cobra.Command, args []string) {
	consulAddr, _ := cmd.Flags().GetString("consul")
	serviceFilter, _ := cmd.Flags().GetString("service")

	// Create Consul client
	config := consulapi.DefaultConfig()
	config.Address = consulAddr
	client, err := consulapi.NewClient(config)
	if err != nil {
		log.Fatalf("Failed to create Consul client: %v", err)
	}

	green := color.New(color.FgGreen).PrintfFunc()
	red := color.New(color.FgRed).PrintfFunc()
	cyan := color.New(color.FgCyan).PrintfFunc()
	yellow := color.New(color.FgYellow).PrintfFunc()
	blue := color.New(color.FgBlue).PrintfFunc()

	fmt.Printf("🔍 Watching Consul at %s for service changes and health status...\n", consulAddr)
	if serviceFilter != "" {
		fmt.Printf("📋 Filtering for service: %s\n", serviceFilter)
	}
	fmt.Println("Press Ctrl+C to stop")

	// Track current services and their health status
	currentServices := make(map[string]*ServiceHealth)
	var lastIndex uint64

	for {
		options := &consulapi.QueryOptions{
			WaitIndex: lastIndex,
			WaitTime:  5 * time.Minute,
		}

		var services map[string][]string
		var meta *consulapi.QueryMeta

		// Get services
		if serviceFilter != "" {
			// For specific service, get health info directly
			health, err := getServiceHealth(client, serviceFilter)
			if err != nil {
				red("Error getting service '%s': %v\n", serviceFilter, err)
				time.Sleep(5 * time.Second)
				continue
			}
			services = make(map[string][]string)
			if len(health.Instances) > 0 {
				services[serviceFilter] = []string{}
			}
			currentServices[serviceFilter] = health
		} else {
			services, meta, err = client.Catalog().Services(options)
			if err != nil {
				red("Error getting services: %v\n", err)
				time.Sleep(5 * time.Second)
				continue
			}

			// Update health status for all services
			for serviceName := range services {
				health, err := getServiceHealth(client, serviceName)
				if err != nil {
					yellow("Warning: Could not get health for %s: %v\n", serviceName, err)
					continue
				}
				currentServices[serviceName] = health
			}
		}

		if meta != nil && meta.LastIndex == lastIndex {
			cyan("No changes detected (timeout), re-watching...\n")
			continue
		}

		if meta != nil {
			lastIndex = meta.LastIndex
		}
		cyan("\nChange detected at %s\n", time.Now().Format(time.Kitchen))

		// Display current service status
		for serviceName, health := range currentServices {
			statusIcon := formatHealthStatus(health.Status)
			blue("📦 Service: %s %s\n", serviceName, statusIcon)
			
			if len(health.Instances) > 0 {
				yellow("   📍 Instances: %d\n", len(health.Instances))
				
				for i, instance := range health.Instances {
					yellow("      %d. %s (%s:%d)\n", i+1, instance.Service.ID, instance.Service.Address, instance.Service.Port)
					
					// Show health checks for this instance
					for _, check := range instance.Checks {
						if check.ServiceID == instance.Service.ID {
							checkIcon := formatCheckStatus(check.Status)
							cyan("         %s %s: %s\n", checkIcon, check.Name, check.Status)
						}
					}
				}
			} else {
				red("   ❌ No healthy instances\n")
			}
			fmt.Println()
		}

		// Check for service registration/deregistration
		oldServices := make(map[string]*ServiceHealth)
		for k, v := range currentServices {
			oldServices[k] = v
		}

		for serviceName := range services {
			if _, exists := oldServices[serviceName]; !exists {
				green("✅ Service registered: %s\n", serviceName)
			}
		}

		for serviceName := range oldServices {
			if _, exists := services[serviceName]; !exists {
				red("❌ Service deregistered: %s\n", serviceName)
				delete(currentServices, serviceName)
			}
		}
	}
}