package main

import (
	consulapi "github.com/hashicorp/consul/api"
)

// ServiceHealth represents the health status of a service
type ServiceHealth struct {
	ServiceName string
	Instances   []*consulapi.ServiceEntry
	Status      string // "passing", "warning", "critical"
}

// InstanceInfo represents service instance information
type InstanceInfo struct {
	*consulapi.CatalogService
	AdminPort int
	GRPCPort  int
}

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