package com.ownding.headscale.service;

import com.ownding.headscale.dal.vo.HeadscalePreAuthKey;
import com.ownding.headscale.dal.vo.HeadscaleUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Headscale Hybrid Service
 * This service provides a unified interface that can use either gRPC or REST API
 * It attempts to use gRPC first for operations that support displayName,
 * and falls back to REST API when needed
 */
@Service
@Slf4j
public class HeadscaleHybridService {

    @Autowired
    private HeadscaleService restService;

    @Autowired
    private HeadscaleGrpcService grpcService;

    /**
     * Create user with optional displayName (namespace)
     * Uses gRPC when displayName is provided, falls back to REST API otherwise
     */
    public HeadscaleUser createUser(String username, String displayName) throws IOException {
        // If displayName is provided, try gRPC first
        if (displayName != null && !displayName.trim().isEmpty()) {
            if (grpcService.isGrpcAvailable()) {
                try {
                    log.info("Creating user via gRPC with displayName: username={}, displayName={}",
                            username, displayName);
                    return grpcService.createUserWithNamespace(username, displayName);
                } catch (IOException e) {
                    log.warn("gRPC user creation failed, falling back to REST API: {}", e.getMessage());
                    log.warn("displayName '{}' will not be set when using REST API fallback", displayName);
                    // Fall through to REST API
                }
            } else {
                log.warn("gRPC is not available. displayName '{}' will not be set when using REST API", displayName);
            }
        }

        // Use REST API (either as fallback or when no displayName provided)
        log.info("Creating user via REST API: username={}", username);
        return restService.createUser(username);
    }

    /**
     * Create user without displayName (uses REST API)
     */
    public HeadscaleUser createUser(String username) throws IOException {
        return restService.createUser(username);
    }

    /**
     * Create namespace using gRPC if available
     */
    public void createNamespace(String namespaceName) throws IOException {
        if (grpcService.isGrpcAvailable()) {
            try {
                log.info("Attempting to create namespace via gRPC: {}", namespaceName);
                grpcService.createNamespace(namespaceName);
                return;
            } catch (IOException e) {
                log.warn("gRPC namespace creation failed: {}", e.getMessage());
                throw e; // Re-throw since REST API doesn't support namespace creation
            }
        }

        throw new IOException("Namespace creation requires gRPC API, but gRPC is not available. " +
                "REST API does not support namespace creation.");
    }

    /**
     * Get all users (uses REST API as it's working well)
     */
    public List<HeadscaleUser> getUsers() throws IOException {
        return restService.getUsers();
    }

    /**
     * Get user by name (uses REST API)
     */
    public HeadscaleUser getUserByName(String username) throws IOException {
        return restService.getUserByName(username);
    }

    /**
     * Delete user (uses REST API)
     */
    public void deleteUser(String username) throws IOException {
        restService.deleteUser(username);
    }

    /**
     * Delete user safely (uses REST API)
     */
    public void deleteUserSafely(String username) throws IOException {
        restService.deleteUserSafely(username);
    }

    /**
     * Create pre-auth key (uses REST API as it's working)
     */
    public HeadscalePreAuthKey createPreAuthKey(String username, Boolean reusable, Boolean ephemeral) throws IOException {
        return restService.createPreAuthKey(username, reusable, ephemeral);
    }

    /**
     * Get pre-auth keys (uses REST API as it's working)
     */
    public List<HeadscalePreAuthKey> getPreAuthKeys(String username) throws IOException {
        return restService.getPreAuthKeys(username);
    }

    /**
     * Check if user has nodes (uses REST API)
     */
    public boolean userHasNodes(String username) throws IOException {
        return restService.userHasNodes(username);
    }

    /**
     * Test connection (tests both REST and gRPC)
     */
    public boolean testConnection() {
        boolean restOk = restService.testConnection();
        boolean grpcOk = grpcService.testGrpcConnection();

        log.info("Connection test results - REST: {}, gRPC: {}", restOk, grpcOk);
        return restOk; // REST is primary, so return its status
    }

    /**
     * Get connection status for both APIs
     */
    public ConnectionStatus getConnectionStatus() {
        boolean restOk = restService.testConnection();
        boolean grpcOk = grpcService.testGrpcConnection();

        return new ConnectionStatus(restOk, grpcOk);
    }

    /**
     * Connection status holder
     */
    public static class ConnectionStatus {
        private final boolean restAvailable;
        private final boolean grpcAvailable;

        public ConnectionStatus(boolean restAvailable, boolean grpcAvailable) {
            this.restAvailable = restAvailable;
            this.grpcAvailable = grpcAvailable;
        }

        public boolean isRestAvailable() {
            return restAvailable;
        }

        public boolean isGrpcAvailable() {
            return grpcAvailable;
        }

        public boolean isAnyAvailable() {
            return restAvailable || grpcAvailable;
        }

        public String getStatus() {
            if (restAvailable && grpcAvailable) {
                return "Both REST and gRPC APIs are available";
            } else if (restAvailable) {
                return "Only REST API is available";
            } else if (grpcAvailable) {
                return "Only gRPC API is available";
            } else {
                return "No APIs are available";
            }
        }
    }
}
