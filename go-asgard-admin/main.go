package main

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"
)

var rootCmd = &cobra.Command{
	Use:   "asgard-admin",
	Short: "Service Discovery Admin Tool for Consul",
	Long: `Asgard Admin is a service discovery and admin tool for Consul.
It discovers services, shows online instances, lists gRPC functions,
and allows interactive execution of admin functions.`,
}

func init() {
	rootCmd.AddCommand(watchCmd)
	rootCmd.AddCommand(discoverCmd)
	rootCmd.AddCommand(adminCmd)
	rootCmd.AddCommand(fixConfigCmd)
}

func main() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
} 