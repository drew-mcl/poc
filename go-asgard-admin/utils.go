package main

import (
	consulapi "github.com/hashicorp/consul/api"
)

func getServiceHealth(client *consulapi.Client, serviceName string) (*ServiceHealth, error) {
	instances, _, err := client.Health().Service(serviceName, "", true, nil)
	if err != nil {
		return nil, err
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

	return &ServiceHealth{
		ServiceName: serviceName,
		Instances:   instances,
		Status:      status,
	}, nil
}

func formatHealthStatus(status string) string {
	switch status {
	case "passing":
		return "✅ passing"
	case "warning":
		return "⚠️  warning"
	case "critical":
		return "❌ critical"
	default:
		return "❓ unknown"
	}
}

func formatCheckStatus(status string) string {
	switch status {
	case "passing":
		return "✅"
	case "warning":
		return "⚠️"
	case "critical":
		return "❌"
	default:
		return "❓"
	}
} 