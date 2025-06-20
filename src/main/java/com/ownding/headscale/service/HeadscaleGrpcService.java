package com.ownding.headscale.service;

import com.ownding.headscale.conf.HeadscaleProperties;
import com.ownding.headscale.dal.vo.HeadscalePreAuthKey;
import com.ownding.headscale.dal.vo.HeadscaleUser;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Headscale gRPC Service
 * This service provides gRPC-based operations for Headscale
 * including namespace/user creation with displayName support
 */
@Service
@Slf4j
public class HeadscaleGrpcService {



    @Autowired
    private HeadscaleProperties headscaleProperties;

    private ManagedChannel channel;

    @PostConstruct
    public void init() {
        try {
            // Initialize gRPC channel
            ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                    .forAddress(headscaleProperties.getGrpcHost(), headscaleProperties.getGrpcPort());

            if (!headscaleProperties.isGrpcTls()) {
                channelBuilder.usePlaintext();
                log.info("Using plaintext gRPC connection");
            } else {
                log.info("Using TLS gRPC connection");
                // For TLS connections, especially with self-signed certificates
                try {
                    // Use default TLS settings, but we might need to handle certificate issues
                    channelBuilder.useTransportSecurity();
                    log.info("TLS transport security enabled");
                } catch (Exception e) {
                    log.warn("Failed to configure TLS, falling back to plaintext", e);
                    channelBuilder.usePlaintext();
                }
            }

            this.channel = channelBuilder
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(5, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(true)
                    .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
                    .build();

            log.info("Headscale gRPC channel initialized: {}:{} (TLS: {})",
                    headscaleProperties.getGrpcHost(),
                    headscaleProperties.getGrpcPort(),
                    headscaleProperties.isGrpcTls());
        } catch (Exception e) {
            log.error("Failed to initialize gRPC channel", e);
            throw new RuntimeException("Failed to initialize Headscale gRPC service", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("Headscale gRPC channel shutdown completed");
            } catch (InterruptedException e) {
                log.warn("Interrupted while shutting down gRPC channel", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Test gRPC connection
     */
    public boolean testGrpcConnection() {
        try {
            if (channel == null || channel.isShutdown()) {
                log.warn("gRPC channel is null or shutdown");
                return false;
            }

            log.info("Testing gRPC connection to {}:{} (TLS: {})",
                    headscaleProperties.getGrpcHost(),
                    headscaleProperties.getGrpcPort(),
                    headscaleProperties.isGrpcTls());

            // Check channel state first
            ConnectivityState state = channel.getState(false);
            log.info("Channel state: {}", state);

            // Try to call a simple gRPC method to test connectivity
            Metadata metadata = new Metadata();
            Metadata.Key<String> authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(authKey, "Bearer " + headscaleProperties.getApiKey());

            headscale.v1.HeadscaleServiceGrpc.HeadscaleServiceBlockingStub stub =
                    headscale.v1.HeadscaleServiceGrpc.newBlockingStub(channel)
                            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                            .withDeadlineAfter(10, TimeUnit.SECONDS); // 10 second timeout for test

            // Try to list users as a connectivity test
            headscale.v1.Headscale.ListUsersRequest request =
                    headscale.v1.Headscale.ListUsersRequest.newBuilder().build();

            log.info("Attempting to call listUsers for connectivity test...");
            log.info("Request details: {}", request.toString());

            headscale.v1.Headscale.ListUsersResponse response = stub.listUsers(request);
            log.info("gRPC connection test successful - received {} users", response.getUsersCount());
            return true;

        } catch (StatusRuntimeException e) {
            log.warn("gRPC connection test failed with status: {} - {} (Cause: {})",
                    e.getStatus().getCode(),
                    e.getStatus().getDescription(),
                    e.getCause() != null ? e.getCause().getMessage() : "Unknown");

            // Log additional details for specific error types
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                log.warn("Service unavailable - check if Headscale gRPC server is running and accessible");
            } else if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
                log.warn("Authentication failed - check API key configuration");
            } else if (e.getStatus().getCode() == Status.Code.CANCELLED) {
                log.warn("Request cancelled - this might indicate TLS/certificate issues or protocol mismatch");
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to test gRPC connection - unexpected error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create user with namespace (displayName) using gRPC
     * This method creates both namespace and user in one operation
     */
    public HeadscaleUser createUserWithNamespace(String username, String displayName) throws IOException {
        if (channel == null || channel.isShutdown()) {
            throw new IOException("gRPC channel is not available");
        }

        try {
            log.info("Creating user with gRPC: username={}, displayName={}", username, displayName);

            // Create gRPC stub with authentication
            Metadata metadata = new Metadata();
            Metadata.Key<String> authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(authKey, "Bearer " + headscaleProperties.getApiKey());

            headscale.v1.HeadscaleServiceGrpc.HeadscaleServiceBlockingStub stub =
                    headscale.v1.HeadscaleServiceGrpc.newBlockingStub(channel)
                            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                            .withDeadlineAfter(headscaleProperties.getGrpcTimeout(), TimeUnit.MILLISECONDS);

            // Build CreateUserRequest with username and displayName
            headscale.v1.Headscale.CreateUserRequest.Builder requestBuilder =
                    headscale.v1.Headscale.CreateUserRequest.newBuilder()
                            .setName(username);

            // Set displayName if provided
            if (displayName != null && !displayName.trim().isEmpty()) {
                requestBuilder.setDisplayName(displayName.trim());
                log.info("Setting display_name: {}", displayName.trim());
            }

            headscale.v1.Headscale.CreateUserRequest request = requestBuilder.build();

            // Call gRPC service
            headscale.v1.Headscale.CreateUserResponse response = stub.createUser(request);

            // Convert response to HeadscaleUser object
            headscale.v1.Headscale.User grpcUser = response.getUser();
            HeadscaleUser headscaleUser = new HeadscaleUser();
            headscaleUser.setId(grpcUser.getId()); // Now string type, no conversion needed
            headscaleUser.setName(grpcUser.getName());

            // Set displayName if available
            if (!grpcUser.getDisplayName().isEmpty()) {
                headscaleUser.setDisplayName(grpcUser.getDisplayName());
                log.info("User created with displayName: {}", grpcUser.getDisplayName());
            }

            if (grpcUser.hasCreatedAt()) {
                // Convert protobuf Timestamp to Date if needed
                // headscaleUser.setCreatedAt(new Date(grpcUser.getCreatedAt().getSeconds() * 1000));
            }

            log.info("Successfully created user via gRPC: {}", headscaleUser.getName());
            return headscaleUser;

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for createUserWithNamespace", e);
            throw new IOException("Failed to create user via gRPC: " + e.getStatus().getDescription());
        } catch (Exception e) {
            log.error("Unexpected error in createUserWithNamespace", e);
            throw new IOException("Failed to create user via gRPC: " + e.getMessage());
        }
    }

    /**
     * Create namespace using gRPC
     * Note: In Headscale, namespace creation is typically done through user creation
     */
    public void createNamespace(String namespaceName) throws IOException {
        if (channel == null || channel.isShutdown()) {
            throw new IOException("gRPC channel is not available");
        }

        try {
            log.info("Creating namespace with gRPC: {}", namespaceName);

            // TODO: Implement actual gRPC call
            // In Headscale, namespaces are created implicitly when creating users
            // This method might not be needed depending on Headscale's gRPC API design

            throw new IOException("gRPC implementation not yet complete - please use REST API fallback");

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for createNamespace", e);
            throw new IOException("Failed to create namespace via gRPC: " + e.getStatus().getDescription());
        } catch (Exception e) {
            log.error("Unexpected error in createNamespace", e);
            throw new IOException("Failed to create namespace via gRPC: " + e.getMessage());
        }
    }

    /**
     * Get users using gRPC
     */
    public List<HeadscaleUser> getUsers() throws IOException {
        if (channel == null || channel.isShutdown()) {
            throw new IOException("gRPC channel is not available");
        }

        try {
            log.info("Getting users with gRPC");

            // Create gRPC stub with authentication
            Metadata metadata = new Metadata();
            Metadata.Key<String> authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(authKey, "Bearer " + headscaleProperties.getApiKey());

            headscale.v1.HeadscaleServiceGrpc.HeadscaleServiceBlockingStub stub =
                    headscale.v1.HeadscaleServiceGrpc.newBlockingStub(channel)
                            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                            .withDeadlineAfter(headscaleProperties.getGrpcTimeout(), TimeUnit.MILLISECONDS);

            // Build ListUsersRequest
            headscale.v1.Headscale.ListUsersRequest request =
                    headscale.v1.Headscale.ListUsersRequest.newBuilder().build();

            // Call gRPC service
            headscale.v1.Headscale.ListUsersResponse response = stub.listUsers(request);

            // Convert response to List<HeadscaleUser>
            List<HeadscaleUser> users = new ArrayList<>();
            for (headscale.v1.Headscale.User grpcUser : response.getUsersList()) {
                HeadscaleUser headscaleUser = new HeadscaleUser();
                headscaleUser.setId(grpcUser.getId()); // Now string type, no conversion needed
                headscaleUser.setName(grpcUser.getName());

                // Set displayName if available
                if (!grpcUser.getDisplayName().isEmpty()) {
                    headscaleUser.setDisplayName(grpcUser.getDisplayName());
                    log.debug("User {} has displayName: {}", grpcUser.getName(), grpcUser.getDisplayName());
                }

                users.add(headscaleUser);
            }

            log.info("Successfully retrieved {} users via gRPC", users.size());
            return users;

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for getUsers", e);
            throw new IOException("Failed to get users via gRPC: " + e.getStatus().getDescription());
        } catch (Exception e) {
            log.error("Unexpected error in getUsers", e);
            throw new IOException("Failed to get users via gRPC: " + e.getMessage());
        }
    }

    /**
     * Create pre-auth key using gRPC
     */
    public HeadscalePreAuthKey createPreAuthKey(String username, Boolean reusable, Boolean ephemeral) throws IOException {
        if (channel == null || channel.isShutdown()) {
            throw new IOException("gRPC channel is not available");
        }

        try {
            log.info("Creating pre-auth key with gRPC for user: {}", username);

            // TODO: Implement actual gRPC call

            throw new IOException("gRPC implementation not yet complete - please use REST API fallback");

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for createPreAuthKey", e);
            throw new IOException("Failed to create pre-auth key via gRPC: " + e.getStatus().getDescription());
        } catch (Exception e) {
            log.error("Unexpected error in createPreAuthKey", e);
            throw new IOException("Failed to create pre-auth key via gRPC: " + e.getMessage());
        }
    }

    /**
     * Check if gRPC is available and properly configured
     */
    public boolean isGrpcAvailable() {
        return channel != null && !channel.isShutdown() && testGrpcConnection();
    }

    /**
     * Get detailed gRPC connection diagnostics
     */
    public String getGrpcDiagnostics() {
        StringBuilder diagnostics = new StringBuilder();

        diagnostics.append("=== Headscale gRPC Diagnostics ===\n");
        diagnostics.append("Host: ").append(headscaleProperties.getGrpcHost()).append("\n");
        diagnostics.append("Port: ").append(headscaleProperties.getGrpcPort()).append("\n");
        diagnostics.append("TLS: ").append(headscaleProperties.isGrpcTls()).append("\n");
        diagnostics.append("Timeout: ").append(headscaleProperties.getGrpcTimeout()).append("ms\n");
        diagnostics.append("API Key: ").append(headscaleProperties.getApiKey() != null ? "***configured***" : "NOT SET").append("\n");

        if (channel == null) {
            diagnostics.append("Channel: NULL\n");
        } else {
            diagnostics.append("Channel State: ").append(channel.getState(false)).append("\n");
            diagnostics.append("Channel Shutdown: ").append(channel.isShutdown()).append("\n");
            diagnostics.append("Channel Terminated: ").append(channel.isTerminated()).append("\n");
        }

        // Test connection
        boolean connectionTest = testGrpcConnection();
        diagnostics.append("Connection Test: ").append(connectionTest ? "PASS" : "FAIL").append("\n");

        // Add troubleshooting suggestions
        diagnostics.append("\n=== Troubleshooting Suggestions ===\n");
        if (!connectionTest) {
            diagnostics.append("1. Try disabling TLS: set headscale.server.grpcTls=false\n");
            diagnostics.append("2. Verify Headscale server supports gRPC on port ").append(headscaleProperties.getGrpcPort()).append("\n");
            diagnostics.append("3. Check if API key is valid for gRPC access\n");
            diagnostics.append("4. Ensure Headscale version supports the gRPC methods being called\n");
        }

        return diagnostics.toString();
    }

    /**
     * Test connection with different ports
     */
    public String testConnectionModes() {
        StringBuilder results = new StringBuilder();
        results.append("=== Connection Mode Tests ===\n");

        // Test current configuration
        boolean currentResult = testGrpcConnection();
        results.append("Current config (Port=").append(headscaleProperties.getGrpcPort())
                .append(", TLS=").append(headscaleProperties.isGrpcTls()).append("): ")
                .append(currentResult ? "SUCCESS" : "FAILED").append("\n");

        if (!currentResult) {
            results.append("\n=== Port Testing Suggestions ===\n");
            results.append("The error 'Invalid protobuf byte sequence' suggests the port is responding\n");
            results.append("but with non-gRPC data (likely HTTP/REST API).\n\n");
            results.append("Common Headscale gRPC ports to try:\n");
            results.append("- 9090 (common gRPC port)\n");
            results.append("- 8080 (if gRPC shares with REST)\n");
            results.append("- 443 (if using TLS)\n");
            results.append("- 50051 (default gRPC port)\n\n");
            results.append("Current port 50443 seems to be responding with HTTP data.\n");

            // Test a few common ports
            int[] commonPorts = {9090, 8080, 50051};
            for (int port : commonPorts) {
                if (port != headscaleProperties.getGrpcPort()) {
                    results.append("Suggestion: Try port ").append(port).append("\n");
                }
            }
        }

        return results.toString();
    }

    /**
     * Test basic gRPC connectivity without calling specific methods
     */
    public String testBasicConnectivity() {
        StringBuilder results = new StringBuilder();
        results.append("=== Basic gRPC Connectivity Test ===\n");

        try {
            if (channel == null || channel.isShutdown()) {
                results.append("FAILED: Channel is null or shutdown\n");
                return results.toString();
            }

            ConnectivityState state = channel.getState(true); // Force connection attempt
            results.append("Channel State: ").append(state).append("\n");

            // Wait a bit for connection to establish
            Thread.sleep(1000);

            ConnectivityState newState = channel.getState(false);
            results.append("Channel State After Wait: ").append(newState).append("\n");

            if (newState == ConnectivityState.READY) {
                results.append("SUCCESS: Basic connectivity established\n");
                results.append("Issue is likely protobuf version mismatch\n");
            } else {
                results.append("FAILED: Cannot establish connection\n");
            }

        } catch (Exception e) {
            results.append("ERROR: ").append(e.getMessage()).append("\n");
        }

        return results.toString();
    }

    /**
     * Test if a specific port responds with gRPC
     */
    public String testSpecificPort(int port, boolean useTls) {
        StringBuilder results = new StringBuilder();
        results.append("=== Testing Port ").append(port).append(" (TLS: ").append(useTls).append(") ===\n");

        ManagedChannel testChannel = null;
        try {
            // Create a temporary channel for testing
            ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                    .forAddress(headscaleProperties.getGrpcHost(), port);

            if (!useTls) {
                channelBuilder.usePlaintext();
            }

            testChannel = channelBuilder
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(5, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(true)
                    .maxInboundMessageSize(4 * 1024 * 1024)
                    .build();

            // Test the connection
            Metadata metadata = new Metadata();
            Metadata.Key<String> authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(authKey, "Bearer " + headscaleProperties.getApiKey());

            headscale.v1.HeadscaleServiceGrpc.HeadscaleServiceBlockingStub stub =
                    headscale.v1.HeadscaleServiceGrpc.newBlockingStub(testChannel)
                            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                            .withDeadlineAfter(5, TimeUnit.SECONDS);

            headscale.v1.Headscale.ListUsersRequest request =
                    headscale.v1.Headscale.ListUsersRequest.newBuilder().build();

            headscale.v1.Headscale.ListUsersResponse response = stub.listUsers(request);
            results.append("SUCCESS: Port ").append(port).append(" responded with ").append(response.getUsersCount()).append(" users\n");

        } catch (StatusRuntimeException e) {
            results.append("FAILED: ").append(e.getStatus().getCode()).append(" - ").append(e.getStatus().getDescription()).append("\n");
            if (e.getCause() != null) {
                results.append("Cause: ").append(e.getCause().getMessage()).append("\n");
            }
        } catch (Exception e) {
            results.append("ERROR: ").append(e.getMessage()).append("\n");
        } finally {
            if (testChannel != null) {
                testChannel.shutdown();
            }
        }

        return results.toString();
    }
}

