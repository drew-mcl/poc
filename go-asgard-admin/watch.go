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
	Short: "Watch services in real-time using efficient blocking queries",
	Long:  "Continuously monitors Consul for service registration and deregistration events.",
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

	fmt.Printf("🔍 Watching Consul at %s for service changes...\n", consulAddr)
	if serviceFilter != "" {
		fmt.Printf("📋 Filtering for service: %s\n", serviceFilter)
	}
	fmt.Println("Press Ctrl+C to stop")

	// --- Blocking Query Implementation ---

	// lastIndex is the index of the last state we've seen from Consul.
	// It's the key to making blocking queries work.
	var lastIndex uint64
	
	// A map to keep track of the services we currently know about.
	currentServices := make(map[string]bool)

	for {
		// Prepare the query options for the blocking call.
		// WaitIndex tells Consul "don't return a result until the state has changed
		// since lastIndex or the WaitTime has passed".
		options := &consulapi.QueryOptions{
			WaitIndex: lastIndex,
			WaitTime:  5 * time.Minute, // How long to hold the connection open.
		}

		var services map[string][]string
		var meta *consulapi.QueryMeta

		// Make the blocking API call.
		// If a service filter is applied, we watch that specific service.
		// Otherwise, we watch all services.
		if serviceFilter != "" {
			var instances []*consulapi.ServiceEntry
			instances, meta, err = client.Health().Service(serviceFilter, "", true, options)
			if err != nil {
				red("Error getting service '%s': %v\n", serviceFilter, err)
				time.Sleep(5 * time.Second) // Wait before retrying on error
				continue
			}
			// Simulate the full service map for consistent logic below.
			services = make(map[string][]string)
			if len(instances) > 0 {
				services[serviceFilter] = []string{} // The value doesn't matter, just the key.
			}
		} else {
			services, meta, err = client.Catalog().Services(options)
			if err != nil {
				red("Error getting services: %v\n", err)
				time.Sleep(5 * time.Second) // Wait before retrying on error
				continue
			}
		}

		// If the index returned by Consul is the same as the one we sent,
		// it means our request timed out and there were no changes.
		// We simply loop and start another blocking query.
		if meta.LastIndex == lastIndex {
			cyan("No changes detected (timeout), re-watching...\n")
			continue
		}

		// A change occurred! Update our lastIndex to the new value.
		lastIndex = meta.LastIndex
		cyan("\nChange detected at index %d (%s)\n", lastIndex, time.Now().Format(time.Kitchen))

		// --- Compare the new state with the old state ---

		// Check for newly registered services.
		for serviceName := range services {
			if _, exists := currentServices[serviceName]; !exists {
				green("✅ Service registered: %s\n", serviceName)
				currentServices[serviceName] = true
			}
		}

		// Check for deregistered services.
		// We create a temporary map to safely iterate while deleting.
		oldServices := make(map[string]bool)
		for k, v := range currentServices {
			oldServices[k] = v
		}

		for serviceName := range oldServices {
			if _, exists := services[serviceName]; !exists {
				red("❌ Service deregistered: %s\n", serviceName)
				delete(currentServices, serviceName)
			}
		}
	}
}