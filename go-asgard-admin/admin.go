package main

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"log"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/fatih/color"
	consulapi "github.com/hashicorp/consul/api"
	"github.com/spf13/cobra"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	reflectionpb "google.golang.org/grpc/reflection/grpc_reflection_v1alpha"

	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"
	"google.golang.org/protobuf/reflect/protodesc"
	"google.golang.org/protobuf/reflect/protoreflect"
	"google.golang.org/protobuf/reflect/protoregistry"
	"google.golang.org/protobuf/types/descriptorpb"
	"google.golang.org/protobuf/types/dynamicpb"
)

var adminCmd = &cobra.Command{
	Use:   "admin",
	Short: "Interactive admin tool for dynamic gRPC execution",
	Long:  "Discovers services via Consul and allows dynamic interaction with their gRPC methods. By default shows only healthy instances.",
	Run:   runAdmin,
}

var fixConfigCmd = &cobra.Command{
	Use:   "fix-config [service-name]",
	Short: "Get FIX configuration for a service",
	Long:  "Retrieves FIX configuration (socket port, client ID, etc.) for a specific service via Consul discovery.",
	Args:  cobra.ExactArgs(1),
	Run: func(cmd *cobra.Command, args []string) {
		consulAddr, _ := cmd.Flags().GetString("consul")
		serviceName := args[0]
		
		GetFixConfigForService(consulAddr, serviceName)
	},
}

func init() {
	adminCmd.Flags().StringP("consul", "c", "localhost:8500", "Consul address")
	adminCmd.Flags().StringP("service", "s", "", "Target specific service (skip service selection)")
	adminCmd.Flags().StringP("instance", "i", "", "Target specific instance (skip instance selection)")
	adminCmd.Flags().BoolP("auto-select", "a", false, "Auto-select first available service/instance")
	adminCmd.Flags().BoolP("all", "", false, "Show all instances (including unhealthy) - default is healthy only")
	
	fixConfigCmd.Flags().StringP("consul", "c", "localhost:8500", "Consul address")
}

var (
	green  = color.New(color.FgGreen).Printf
	blue   = color.New(color.FgBlue).Printf
	yellow = color.New(color.FgYellow).Printf
	red    = color.New(color.FgRed).Printf
	cyan   = color.New(color.FgCyan).Printf
)

func runAdmin(cmd *cobra.Command, args []string) {
	consulAddr, _ := cmd.Flags().GetString("consul")
	targetService, _ := cmd.Flags().GetString("service")
	targetInstance, _ := cmd.Flags().GetString("instance")
	autoSelect, _ := cmd.Flags().GetBool("auto-select")
	showAll, _ := cmd.Flags().GetBool("all")

	client, err := consulapi.NewClient(&consulapi.Config{Address: consulAddr})
	if err != nil {
		log.Fatalf("Failed to create Consul client: %v", err)
	}

	reader := bufio.NewReader(os.Stdin)

	for {
		selectedService, err := selectService(client, reader, targetService, autoSelect)
		if err != nil {
			red("Error selecting service: %v\n", err)
			continue
		}
		if selectedService == "" {
			cyan("👋 Goodbye!\n")
			return
		}

		selectedInstance, err := selectInstance(client, reader, selectedService, targetInstance, autoSelect, showAll)
		if err != nil {
			red("Error selecting instance: %v\n", err)
			continue
		}
		if selectedInstance == nil {
			continue
		}

		err = connectAndExecute(selectedInstance, reader)
		if err != nil {
			if err.Error() == "return_to_service_selection" {
				continue // Go back to service selection
			}
			red("Error during gRPC execution: %v\n", err)
		}

		if autoSelect || targetInstance != "" {
			return
		}

		cyan("\nPress Enter to return to the service list or 'q' to quit: ")
		choice, _ := reader.ReadString('\n')
		if strings.TrimSpace(choice) == "q" {
			cyan("👋 Goodbye!\n")
			return
		}
	}
}

func selectService(client *consulapi.Client, reader *bufio.Reader, targetService string, autoSelect bool) (string, error) {
	if targetService != "" {
		services, _, err := client.Catalog().Services(nil)
		if err != nil {
			return "", fmt.Errorf("failed to get services: %v", err)
		}
		if _, exists := services[targetService]; !exists {
			return "", fmt.Errorf("target service '%s' not found in Consul", targetService)
		}
		green("🎯 Using target service: %s\n", targetService)
		return targetService, nil
	}

	services, _, err := client.Catalog().Services(nil)
	if err != nil {
		return "", fmt.Errorf("failed to get services: %v", err)
	}

	var serviceNames []string
	for name := range services {
		if name == "consul" {
			continue
		}
		serviceNames = append(serviceNames, name)
	}
	sort.Strings(serviceNames)

	if autoSelect && len(serviceNames) > 0 {
		selected := serviceNames[0]
		green("🤖 Auto-selected service: %s\n", selected)
		return selected, nil
	}

	cyan("\n🔍 Available services:\n")
	for i, name := range serviceNames {
		green("  %d. %s\n", i+1, name)
	}

	for {
		cyan("Select a service (1-%d) or 'q' to quit: ", len(serviceNames))
		choiceStr, _ := reader.ReadString('\n')
		choiceStr = strings.TrimSpace(choiceStr)
		if choiceStr == "q" {
			return "", nil
		}
		choice, err := strconv.Atoi(choiceStr)
		if err != nil || choice < 1 || choice > len(serviceNames) {
			red("❌ Invalid choice. Please try again.\n")
			continue
		}
		return serviceNames[choice-1], nil
	}
}

func selectInstance(client *consulapi.Client, reader *bufio.Reader, serviceName string, targetInstance string, autoSelect bool, showAll bool) (*InstanceInfo, error) {
	// Get instances based on showAll flag
	var instances []*consulapi.CatalogService
	var err error
	
	if showAll {
		// Get all instances from catalog (including unhealthy)
		instances, _, err = client.Catalog().Service(serviceName, "", nil)
		if err != nil {
			return nil, fmt.Errorf("failed to get instances for %s: %v", serviceName, err)
		}
	} else {
		// Get only healthy instances (default behavior)
		healthInstances, _, err := client.Health().Service(serviceName, "", true, nil)
		if err != nil {
			return nil, fmt.Errorf("failed to get healthy instances for %s: %v", serviceName, err)
		}
		// Convert health instances to catalog instances
		for _, healthInstance := range healthInstances {
			instances = append(instances, &consulapi.CatalogService{
				ServiceID:      healthInstance.Service.ID,
				ServiceName:    healthInstance.Service.Service,
				ServiceAddress: healthInstance.Service.Address,
				ServicePort:    healthInstance.Service.Port,
				ServiceMeta:    healthInstance.Service.Meta,
				ServiceTags:    healthInstance.Service.Tags,
			})
		}
	}
	if err != nil {
		return nil, fmt.Errorf("failed to get instances for %s: %v", serviceName, err)
	}
	if len(instances) == 0 {
		red("❌ No instances found for %s\n", serviceName)
		return nil, nil
	}

	var instanceInfos []*InstanceInfo
	for _, instance := range instances {
		info := getInstanceInfo(
			instance.ServiceID,
			instance.ServiceName,
			instance.ServiceAddress,
			instance.ServicePort,
			instance.ServiceMeta,
			instance.ServiceTags,
		)
		instanceInfos = append(instanceInfos, info)
	}

	if autoSelect && len(instanceInfos) > 0 {
		selected := instanceInfos[0]
		green("🤖 Auto-selected instance: %s (%s:%d)\n", selected.ServiceID, selected.ServiceAddress, selected.ServicePort)
		return selected, nil
	}

	if targetInstance != "" {
		for _, instance := range instanceInfos {
			if instance.ServiceID == targetInstance {
				green("🎯 Using target instance: %s (%s:%d)\n", instance.ServiceID, instance.ServiceAddress, instance.ServicePort)
				return instance, nil
			}
		}
		return nil, fmt.Errorf("target instance '%s' not found for service '%s'", targetInstance, serviceName)
	}

	cyan("\n📦 Instances for %s:\n", serviceName)
	if showAll {
		cyan("(Showing all instances including unhealthy)\n")
	} else {
		cyan("(Showing only healthy instances - use --all to see all)\n")
	}
	
	for i, instance := range instanceInfos {
		// Get health status for this instance
		healthStatus := "✅ healthy"
		
		// Always check health status for display purposes
		healthChecks, _, err := client.Health().Checks(instance.ServiceID, nil)
		if err == nil {
			hasCritical := false
			hasWarning := false
			for _, check := range healthChecks {
				if check.ServiceID == instance.ServiceID {
					switch check.Status {
					case "critical":
						hasCritical = true
					case "warning":
						hasWarning = true
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
		
		yellow("  %d. %s (%s:%d) %s\n", i+1, instance.ServiceID, instance.ServiceAddress, instance.ServicePort, healthStatus)
		//cyan("     🔌 Admin: %s:%d\n", instance.ServiceAddress, adminPort)
	}

	for {
		cyan("Select an instance (1-%d) or 'b' to go back: ", len(instanceInfos))
		choiceStr, _ := reader.ReadString('\n')
		choiceStr = strings.TrimSpace(choiceStr)
		if choiceStr == "b" {
			return nil, nil
		}
		choice, err := strconv.Atoi(choiceStr)
		if err != nil || choice < 1 || choice > len(instanceInfos) {
			red("❌ Invalid choice. Please try again.\n")
			continue
		}
		return instanceInfos[choice-1], nil
	}
}

func connectAndExecute(instance *InstanceInfo, reader *bufio.Reader) error {
	// Get admin port from metadata
	adminPort := instance.ServicePort // fallback to main port
	if adminPortStr, exists := instance.ServiceMeta["admin-port"]; exists {
		if port, err := strconv.Atoi(adminPortStr); err == nil {
			adminPort = port
		}
	}
	
	grpcAddr := fmt.Sprintf("%s:%d", instance.ServiceAddress, adminPort)
	blue("🔌 Connecting to gRPC server at %s...\n", grpcAddr)

	// Try to establish connection with timeout and retry logic
	conn, err := establishConnection(grpcAddr)
	if err != nil {
		return fmt.Errorf("failed to connect: %v", err)
	}
	defer conn.Close()

	files, serviceNames, err := discoverServicesViaReflection(context.Background(), conn)
	if err != nil {
		return fmt.Errorf("failed to discover services via reflection: %v", err)
	}

	// This loop allows the user to execute multiple methods within the same session.
	for {
		var allMethods []protoreflect.MethodDescriptor
		var simpleMethodNames []string

		cyan("\n🛠️  Available gRPC methods on %s:\n", instance.ServiceID)
		methodIndex := 1
		for _, srvName := range serviceNames {
			sd, err := files.FindDescriptorByName(protoreflect.FullName(srvName))
			if err != nil {
				continue
			}
			serviceDesc, ok := sd.(protoreflect.ServiceDescriptor)
			if !ok {
				continue
			}
			methods := serviceDesc.Methods()
			for i := 0; i < methods.Len(); i++ {
				method := methods.Get(i)
				allMethods = append(allMethods, method)

				// Simplify the method name for cleaner display
				fullName := method.FullName()
				parts := strings.Split(string(fullName), ".")
				simpleName := parts[len(parts)-1]
				simpleMethodNames = append(simpleMethodNames, simpleName)
				green("  %d. %s\n", methodIndex, simpleName)
				methodIndex++
			}
		}

		if len(allMethods) == 0 {
			red("No gRPC methods discovered. Ensure reflection is enabled on the server.\n")
			return nil
		}

		cyan("Select a method (1-%d), 'b' to go back to instance selection, or 's' to go back to service selection: ", len(allMethods))
		choiceStr, _ := reader.ReadString('\n')
		choiceStr = strings.TrimSpace(choiceStr)
		if choiceStr == "b" || choiceStr == "back" {
			return nil // Exit this function to go back to the main loop
		}
		if choiceStr == "s" || choiceStr == "service" {
			return fmt.Errorf("return_to_service_selection") // Special error to indicate service selection return
		}
		choice, err := strconv.Atoi(choiceStr)
		if err != nil || choice < 1 || choice > len(allMethods) {
			red("❌ Invalid method choice.\n")
			continue // Ask for method selection again
		}
		selectedMethod := allMethods[choice-1]

		req := dynamicpb.NewMessage(selectedMethod.Input())
		if err = getMessageInput(req, reader); err != nil {
			red("Error getting message input: %v\n", err)
			continue
		}

		cyan("\n🚀 Executing %s...\n", selectedMethod.Name())
		resp := dynamicpb.NewMessage(selectedMethod.Output())
		methodName := fmt.Sprintf("/%s/%s", selectedMethod.Parent().FullName(), selectedMethod.Name())

		err = conn.Invoke(context.Background(), methodName, req, resp)
		if err != nil {
			red("❌ gRPC Invoke failed: %v\n", err)
			
			// Check if it's a connection error and offer to reconnect
			if isConnectionError(err) {
				yellow("🔄 Connection appears to be lost. Attempting to reconnect...\n")
				newConn, reconnectErr := establishConnection(grpcAddr)
				if reconnectErr != nil {
					red("❌ Failed to reconnect: %v\n", reconnectErr)
					return fmt.Errorf("connection lost and reconnection failed: %v", reconnectErr)
				}
				conn.Close()
				conn = newConn
				yellow("✅ Reconnected successfully!\n")
			}
			continue // Allow user to try again
		}

		// REMOVED: All hard-coded logic.
		// This block now simply prints the result for any successful method call.
		jsonMarshaler := &protojson.MarshalOptions{Multiline: true, Indent: "  ", UseProtoNames: true, EmitUnpopulated: true}
		jsonBytes, err := jsonMarshaler.Marshal(resp)
		if err != nil {
			red("❌ Failed to marshal response to JSON: %v\n", err)
			continue
		}
		green("✅ Result:\n")
		fmt.Println(string(jsonBytes))

		cyan("\nPress Enter to continue to method selection, 'b' to go back to instance selection, or 's' to go back to service selection: ")
		continueChoice, _ := reader.ReadString('\n')
		continueChoice = strings.TrimSpace(continueChoice)
		if continueChoice == "b" || continueChoice == "back" {
			return nil // Go back to instance selection
		}
		if continueChoice == "s" || continueChoice == "service" {
			return fmt.Errorf("return_to_service_selection") // Go back to service selection
		}
		// If Enter or any other key, continue to method selection
	}
}

// establishConnection creates a gRPC connection with timeout and retry logic
func establishConnection(grpcAddr string) (*grpc.ClientConn, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	
	conn, err := grpc.DialContext(ctx, grpcAddr, 
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithBlock())
	
	if err != nil {
		return nil, fmt.Errorf("failed to connect to %s: %v", grpcAddr, err)
	}
	
	return conn, nil
}

// isConnectionError checks if the error is likely a connection issue
func isConnectionError(err error) bool {
	if err == nil {
		return false
	}
	
	errStr := err.Error()
	connectionErrors := []string{
		"connection refused",
		"connection reset",
		"broken pipe",
		"unavailable",
		"deadline exceeded",
		"context deadline exceeded",
		"transport is closing",
	}
	
	for _, connErr := range connectionErrors {
		if strings.Contains(strings.ToLower(errStr), connErr) {
			return true
		}
	}
	
	return false
}

func discoverServicesViaReflection(ctx context.Context, conn *grpc.ClientConn) (*protoregistry.Files, []string, error) {
	client := reflectionpb.NewServerReflectionClient(conn)
	stream, err := client.ServerReflectionInfo(ctx, grpc.WaitForReady(true))
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create reflection stream: %w", err)
	}
	defer stream.CloseSend()

	if err := stream.Send(&reflectionpb.ServerReflectionRequest{MessageRequest: &reflectionpb.ServerReflectionRequest_ListServices{}}); err != nil {
		return nil, nil, fmt.Errorf("failed to send list services request: %w", err)
	}
	resp, err := stream.Recv()
	if err != nil {
		return nil, nil, fmt.Errorf("failed to receive list services response: %w", err)
	}
	listResp := resp.GetListServicesResponse()
	if listResp == nil {
		return nil, nil, fmt.Errorf("invalid list services response")
	}

	var serviceNames []string
	for _, s := range listResp.GetService() {
		if !strings.HasPrefix(s.Name, "grpc.") {
			serviceNames = append(serviceNames, s.Name)
		}
	}

	fds := make(map[string]*descriptorpb.FileDescriptorProto)
	for _, srv := range serviceNames {
		err = getFileDescriptorsForSymbol(srv, stream, fds)
		if err != nil {
			yellow("Warning: could not resolve service %s: %v\n", srv, err)
		}
	}

	fdProtos := make([]*descriptorpb.FileDescriptorProto, 0, len(fds))
	for _, fd := range fds {
		fdProtos = append(fdProtos, fd)
	}

	files, err := protodesc.NewFiles(&descriptorpb.FileDescriptorSet{File: fdProtos})
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create file registry: %w", err)
	}

	return files, serviceNames, nil
}

func getFileDescriptorsForSymbol(symbol string, stream reflectionpb.ServerReflection_ServerReflectionInfoClient, fds map[string]*descriptorpb.FileDescriptorProto) error {
	if err := stream.Send(&reflectionpb.ServerReflectionRequest{
		MessageRequest: &reflectionpb.ServerReflectionRequest_FileContainingSymbol{FileContainingSymbol: symbol},
	}); err != nil {
		return fmt.Errorf("failed to send file containing symbol request: %w", err)
	}
	resp, err := stream.Recv()
	if err == io.EOF {
		return io.EOF
	}
	if err != nil {
		return fmt.Errorf("failed to receive file containing symbol response: %w", err)
	}

	fileDescResp := resp.GetFileDescriptorResponse()
	if fileDescResp == nil {
		return fmt.Errorf("invalid file descriptor response for symbol %s", symbol)
	}

	for _, b := range fileDescResp.FileDescriptorProto {
		fd := &descriptorpb.FileDescriptorProto{}
		if err := proto.Unmarshal(b, fd); err != nil {
			return fmt.Errorf("failed to unmarshal file descriptor: %w", err)
		}
		if _, ok := fds[fd.GetName()]; !ok {
			fds[fd.GetName()] = fd
			for _, dep := range fd.GetDependency() {
				if _, ok := fds[dep]; !ok {
					if err := getFileDescriptorsForSymbol(dep, stream, fds); err != nil && err != io.EOF {
						return err
					}
				}
			}
		}
	}
	return nil
}

func getMessageInput(msg *dynamicpb.Message, reader *bufio.Reader) error {
	fields := msg.Descriptor().Fields()
	if fields.Len() == 0 {
		return nil
	}

	cyan("\nPlease provide input for method %s:\n", msg.Descriptor().Name())
	for i := 0; i < fields.Len(); i++ {
		field := fields.Get(i)
		blue("  Enter value for field '%s' (type: %s): ", field.Name(), field.Kind())

		input, _ := reader.ReadString('\n')
		input = strings.TrimSpace(input)
		if input == "" {
			continue
		}

		var value protoreflect.Value
		var err error
		switch field.Kind() {
		case protoreflect.StringKind:
			value = protoreflect.ValueOfString(input)
		case protoreflect.Int32Kind, protoreflect.Sint32Kind, protoreflect.Sfixed32Kind:
			v, e := strconv.ParseInt(input, 10, 32)
			if err = e; err == nil {
				value = protoreflect.ValueOfInt32(int32(v))
			}
		case protoreflect.Int64Kind, protoreflect.Sint64Kind, protoreflect.Sfixed64Kind:
			v, e := strconv.ParseInt(input, 10, 64)
			if err = e; err == nil {
				value = protoreflect.ValueOfInt64(v)
			}
		case protoreflect.DoubleKind:
			v, e := strconv.ParseFloat(input, 64)
			if err = e; err == nil {
				value = protoreflect.ValueOfFloat64(v)
			}
		case protoreflect.BoolKind:
			v, e := strconv.ParseBool(input)
			if err = e; err == nil {
				value = protoreflect.ValueOfBool(v)
			}
		default:
			red("Unsupported field type for CLI input: %s\n", field.Kind())
			continue
		}

		if err != nil {
			red("Invalid input for type %s: %v. Field will be left empty.\n", field.Kind(), err)
			continue
		}
		msg.Set(field, value)
	}
	return nil
}