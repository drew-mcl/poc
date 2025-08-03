package main

import (
	consulapi "github.com/hashicorp/consul/api"
)

// ServiceInfo holds enhanced service information
type ServiceInfo struct {
	Name      string
	Instances []*consulapi.CatalogService
	Online    int
	Total     int
}

// InstanceInfo holds enhanced instance information
type InstanceInfo struct {
	*consulapi.CatalogService
	AdminPort int
	GRPCPort  int
	Tags      []string
}

// AdminConfig holds configuration for admin operations
type AdminConfig struct {
	ConsulAddr    string
	TargetService string
	TargetInstance string
	AutoSelect    bool
	OnlineOnly    bool
}

// ServiceDiscovery provides methods for discovering and managing services
type ServiceDiscovery interface {
	GetServices() (map[string][]string, error)
	GetServiceInstances(serviceName string) ([]*InstanceInfo, error)
	GetInstanceInfo(instance *consulapi.CatalogService) *InstanceInfo
} 