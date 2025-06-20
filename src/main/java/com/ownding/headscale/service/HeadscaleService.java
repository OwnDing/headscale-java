package com.ownding.headscale.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import com.ownding.headscale.conf.HeadscaleProperties;
import com.ownding.headscale.dal.vo.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Headscale API Service
 */
@Service
@Slf4j
public class HeadscaleService {

    @Autowired
    private HeadscaleProperties headscaleProperties;

    private OkHttpClient httpClient;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(headscaleProperties.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(headscaleProperties.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(headscaleProperties.getTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Create a new user
     */
    public HeadscaleUser createUser(String username) throws IOException {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        HeadscaleCreateUserRequest request = new HeadscaleCreateUserRequest(username);
        String jsonBody = JSON.toJSONString(request);

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonBody);
        Request httpRequest = new Request.Builder()
                .url(headscaleProperties.getUrl() + "/api/v1/user")
                .post(body)
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create user: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("Create user response: {}", responseBody);

            // Try to parse as a direct user object first
            try {
                return JSON.parseObject(responseBody, HeadscaleUser.class);
            } catch (Exception e) {
                // If that fails, try to parse as wrapped response
                HeadscaleApiResponse<HeadscaleUser> apiResponse = JSON.parseObject(responseBody,
                        new TypeReference<HeadscaleApiResponse<HeadscaleUser>>() {});
                return apiResponse.getItem();
            }
        }
    }

    /**
     * Get user by username
     */
    public HeadscaleUser getUserByName(String username) throws IOException {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        List<HeadscaleUser> users = getUsers();
        if (users != null) {
            for (HeadscaleUser user : users) {
                if (username.equals(user.getName())) {
                    return user;
                }
            }
        }

        throw new IOException("用户不存在: " + username);
    }

    /**
     * Delete a user by ID
     */
    public void deleteUserById(String userId) throws IOException {
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("User ID cannot be blank");
        }

        Request httpRequest = new Request.Builder()
                .url(headscaleProperties.getUrl() + "/api/v1/user/" + userId)
                .delete()
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorMessage = "Failed to delete user: " + response.code() + " " + response.message();

                // Try to get more detailed error information from response body
                if (response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        log.error("Delete user error response: {}", responseBody);

                        // Check for common error patterns
                        if (responseBody.contains("node(s) found") || responseBody.contains("not empty")) {
                            errorMessage = "无法删除用户：该用户下还有设备节点，请先删除所有设备后再删除用户";
                        } else if (responseBody.contains("not found")) {
                            errorMessage = "用户不存在或已被删除";
                        } else {
                            errorMessage += ". 详细信息: " + responseBody;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to read error response body", e);
                    }
                }

                throw new IOException(errorMessage);
            }
        }
    }

    /**
     * Delete a user by username
     */
    public void deleteUser(String username) throws IOException {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        // First get the user to find their ID
        HeadscaleUser user = getUserByName(username);

        // Delete using the user ID
        deleteUserById(user.getId());
    }

    /**
     * Get all users
     */
    public List<HeadscaleUser> getUsers() throws IOException {
        Request httpRequest = new Request.Builder()
                .url(headscaleProperties.getUrl() + "/api/v1/user")
                .get()
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get users: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("Get users response: {}", responseBody);

            // Try to parse as a direct list first
            try {
                return JSON.parseArray(responseBody, HeadscaleUser.class);
            } catch (Exception e) {
                // If that fails, try to parse as wrapped response
                HeadscaleApiResponse<HeadscaleUser> apiResponse = JSON.parseObject(responseBody,
                        new TypeReference<HeadscaleApiResponse<HeadscaleUser>>() {});
                return apiResponse.getItems();
            }
        }
    }

    /**
     * Create a pre-auth key for a user
     */
    public HeadscalePreAuthKey createPreAuthKey(String username, Boolean reusable, Boolean ephemeral) throws IOException {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        // First get the user to find their ID, since the API expects user ID, not username
        HeadscaleUser user;
        try {
            user = getUserByName(username);
        } catch (Exception e) {
            log.error("Failed to find user: {}", username, e);
            throw new IOException("用户不存在: " + username);
        }

        // Create request with user ID instead of username
        HeadscaleCreatePreAuthKeyRequest request = new HeadscaleCreatePreAuthKeyRequest(user.getId(), reusable, ephemeral);
        String jsonBody = JSON.toJSONString(request);
        log.debug("Creating pre-auth key for user: {} (ID: {}) with request: {}", username, user.getId(), jsonBody);

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonBody);
        Request httpRequest = new Request.Builder()
                .url(headscaleProperties.getUrl() + "/api/v1/preauthkey")
                .post(body)
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "";
                try {
                    errorBody = response.body() != null ? response.body().string() : "";
                } catch (Exception e) {
                    log.warn("Failed to read error response body", e);
                }
                log.error("Failed to create pre-auth key for user: {} (ID: {}), status: {}, response: {}",
                        username, user.getId(), response.code(), errorBody);
                throw new IOException("Failed to create pre-auth key: " + response.code() + " " + response.message() +
                        (StringUtils.isNotBlank(errorBody) ? " - " + errorBody : ""));
            }

            String responseBody = response.body().string();
            log.debug("Create pre-auth key response: {}", responseBody);

            // Try to parse as a direct object first
            try {
                return JSON.parseObject(responseBody, HeadscalePreAuthKey.class);
            } catch (Exception e) {
                // If that fails, try to parse as wrapped response
                HeadscaleApiResponse<HeadscalePreAuthKey> apiResponse = JSON.parseObject(responseBody,
                        new TypeReference<HeadscaleApiResponse<HeadscalePreAuthKey>>() {});
                return apiResponse.getItem();
            }
        }
    }

    /**
     * Get all pre-auth keys for a user
     */
    public List<HeadscalePreAuthKey> getPreAuthKeys(String username) throws IOException {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        // First get the user to find their ID, since the API expects user ID, not username
        HeadscaleUser user;
        try {
            user = getUserByName(username);
        } catch (Exception e) {
            log.error("Failed to find user: {}", username, e);
            throw new IOException("用户不存在: " + username);
        }

        // Build URL with user ID instead of username
        String url = headscaleProperties.getUrl() + "/api/v1/preauthkey?user=" + user.getId();
        log.debug("Getting pre-auth keys for user: {} (ID: {}) with URL: {}", username, user.getId(), url);

        Request httpRequest = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "";
                try {
                    errorBody = response.body() != null ? response.body().string() : "";
                } catch (Exception e) {
                    log.warn("Failed to read error response body", e);
                }
                log.error("Failed to get pre-auth keys for user: {} (ID: {}), status: {}, response: {}",
                        username, user.getId(), response.code(), errorBody);
                throw new IOException("Failed to get pre-auth keys: " + response.code() + " " + response.message() +
                        (StringUtils.isNotBlank(errorBody) ? " - " + errorBody : ""));
            }

            String responseBody = response.body().string();
            log.debug("Get pre-auth keys response: {}", responseBody);

            // Try to parse as wrapped response first (this is the actual format for most Headscale APIs)
            try {
                HeadscaleApiResponse<HeadscalePreAuthKey> apiResponse = JSON.parseObject(responseBody,
                        new TypeReference<HeadscaleApiResponse<HeadscalePreAuthKey>>() {});
                List<HeadscalePreAuthKey> keys = apiResponse.getItems();
                return keys != null ? keys : new ArrayList<>();
            } catch (Exception e) {
                log.warn("Failed to parse pre-auth keys as wrapped response, trying direct array", e);
                // If that fails, try to parse as direct array
                try {
                    List<HeadscalePreAuthKey> keys = JSON.parseArray(responseBody, HeadscalePreAuthKey.class);
                    return keys != null ? keys : new ArrayList<>();
                } catch (Exception e2) {
                    log.error("Failed to parse pre-auth keys response: {}", responseBody, e2);
                    // Return empty list instead of throwing exception for better user experience
                    return new ArrayList<>();
                }
            }
        }
    }

    /**
     * Get all nodes/devices
     */
    public List<HeadscaleNode> getNodes() throws IOException {
        Request httpRequest = new Request.Builder()
                .url(headscaleProperties.getUrl() + "/api/v1/node")
                .get()
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get nodes: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("Get nodes response: {}", responseBody);

            // Try to parse as wrapped response first (this is the actual format)
            try {
                HeadscaleApiResponse<HeadscaleNode> apiResponse = JSON.parseObject(responseBody,
                        new TypeReference<HeadscaleApiResponse<HeadscaleNode>>() {});
                return apiResponse.getItems();
            } catch (Exception e) {
                log.warn("Failed to parse nodes as wrapped response, trying direct array", e);
                // If that fails, try to parse as direct array
                try {
                    return JSON.parseArray(responseBody, HeadscaleNode.class);
                } catch (Exception e2) {
                    log.error("Failed to parse nodes response: {}", responseBody, e2);
                    throw new IOException("Failed to parse nodes response: " + e2.getMessage());
                }
            }
        }
    }

    /**
     * Get nodes for a specific user
     */
    public List<HeadscaleNode> getNodesByUser(String username) throws IOException {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        // First get the user to find their ID, since the API expects user ID, not username
        HeadscaleUser user;
        try {
            user = getUserByName(username);
        } catch (Exception e) {
            log.error("Failed to find user: {}", username, e);
            throw new IOException("用户不存在: " + username);
        }

        Request httpRequest = new Request.Builder()
                .url(headscaleProperties.getUrl() + "/api/v1/node?user=" + user.getId())
                .get()
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        log.debug("Getting nodes for user: {} (ID: {}) with URL: {}", username, user.getId(), httpRequest.url());

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "";
                try {
                    errorBody = response.body() != null ? response.body().string() : "";
                } catch (Exception e) {
                    log.warn("Failed to read error response body", e);
                }
                log.error("Failed to get nodes for user: {} (ID: {}), status: {}, response: {}",
                        username, user.getId(), response.code(), errorBody);

                // Check if this is a "user not found" error - this can happen if user was deleted between getUserByName and this call
                if (response.code() == 500 && errorBody.contains("user not found")) {
                    log.warn("User {} (ID: {}) not found when getting nodes, possibly deleted concurrently", username, user.getId());
                    throw new IOException("用户不存在: " + username);
                }

                throw new IOException("Failed to get nodes for user: " + response.code() + " " + response.message() +
                        (StringUtils.isNotBlank(errorBody) ? " - " + errorBody : ""));
            }

            String responseBody = response.body().string();
            log.debug("Get nodes by user response: {}", responseBody);

            // Try to parse as wrapped response first (this is the actual format)
            try {
                HeadscaleApiResponse<HeadscaleNode> apiResponse = JSON.parseObject(responseBody,
                        new TypeReference<HeadscaleApiResponse<HeadscaleNode>>() {});
                return apiResponse.getItems();
            } catch (Exception e) {
                log.warn("Failed to parse nodes by user as wrapped response, trying direct array", e);
                // If that fails, try to parse as direct array
                try {
                    return JSON.parseArray(responseBody, HeadscaleNode.class);
                } catch (Exception e2) {
                    log.error("Failed to parse nodes by user response: {}", responseBody, e2);
                    throw new IOException("Failed to parse nodes by user response: " + e2.getMessage());
                }
            }
        }
    }

    /**
     * Delete a node
     */
    public void deleteNode(String nodeId) throws IOException {
        if (StringUtils.isBlank(nodeId)) {
            throw new IllegalArgumentException("Node ID cannot be blank");
        }

        Request httpRequest = new Request.Builder()
                .url(headscaleProperties.getUrl() + "/api/v1/node/" + nodeId)
                .delete()
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to delete node: " + response.code() + " " + response.message());
            }
        }
    }

    /**
     * Get current ACL policy
     */
    public String getACLPolicy() throws IOException {
        Request httpRequest = new Request.Builder()
                .url(headscaleProperties.getUrl() + "/api/v1/policy")
                .get()
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get ACL policy: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("Get ACL policy response: {}", responseBody);

            // Try to parse as wrapped response first
            try {
                HeadscaleApiResponse<String> apiResponse = JSON.parseObject(responseBody,
                        new TypeReference<HeadscaleApiResponse<String>>() {});
                String policy = apiResponse.getItem();
                return policy != null ? policy : "{}";
            } catch (Exception e) {
                log.warn("Failed to parse ACL policy as wrapped response, trying direct parsing", e);
                // If wrapped parsing fails, try to extract policy field directly
                try {
                    com.alibaba.fastjson.JSONObject jsonObj = JSON.parseObject(responseBody);
                    if (jsonObj.containsKey("policy")) {
                        Object policyObj = jsonObj.get("policy");
                        if (policyObj instanceof String) {
                            return (String) policyObj;
                        } else {
                            return JSON.toJSONString(policyObj);
                        }
                    }
                    return responseBody; // Return raw response if no policy field
                } catch (Exception e2) {
                    log.error("Failed to parse ACL policy response: {}", responseBody, e2);
                    return "{}"; // Return empty JSON object as fallback
                }
            }
        }
    }

    /**
     * Update ACL policy
     */
    public String updateACLPolicy(String aclPolicyJson) throws IOException {
        if (StringUtils.isBlank(aclPolicyJson)) {
            throw new IllegalArgumentException("ACL policy cannot be blank");
        }

        // Validate JSON format
        try {
            JSON.parseObject(aclPolicyJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage());
        }

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, aclPolicyJson);

        Request httpRequest = new Request.Builder()
                .url(headscaleProperties.getUrl() + "/api/v1/policy")
                .put(body)
                .addHeader("Authorization", "Bearer " + headscaleProperties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to update ACL policy: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("Update ACL policy response: {}", responseBody);

            // Try to parse as wrapped response first
            try {
                HeadscaleApiResponse<String> apiResponse = JSON.parseObject(responseBody,
                        new TypeReference<HeadscaleApiResponse<String>>() {});
                String policy = apiResponse.getItem();
                return policy != null ? policy : aclPolicyJson;
            } catch (Exception e) {
                log.warn("Failed to parse ACL update response as wrapped response, trying direct parsing", e);
                // If wrapped parsing fails, try to extract policy field directly
                try {
                    com.alibaba.fastjson.JSONObject jsonObj = JSON.parseObject(responseBody);
                    if (jsonObj.containsKey("policy")) {
                        Object policyObj = jsonObj.get("policy");
                        if (policyObj instanceof String) {
                            return (String) policyObj;
                        } else {
                            return JSON.toJSONString(policyObj);
                        }
                    }
                    return aclPolicyJson; // Return input if no policy field in response
                } catch (Exception e2) {
                    log.error("Failed to parse ACL update response: {}", responseBody, e2);
                    return aclPolicyJson; // Return input as fallback
                }
            }
        }
    }

    /**
     * Check if a user has any nodes
     * Returns false if user doesn't exist or has no nodes
     */
    public boolean userHasNodes(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }

        try {
            List<HeadscaleNode> nodes = getNodesByUser(username);
            return nodes != null && !nodes.isEmpty();
        } catch (Exception e) {
            // Check if the error is specifically "user not found"
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("user not found") || errorMessage.contains("用户不存在"))) {
                log.info("User {} not found when checking nodes, treating as no nodes", username);
                return false;
            }
            log.warn("Failed to check nodes for user: {}", username, e);
            return false;
        }
    }

    /**
     * Delete user with node check
     */
    public void deleteUserSafely(String username) throws IOException {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        // First check if user exists
        HeadscaleUser user;
        try {
            user = getUserByName(username);
        } catch (Exception e) {
            log.warn("User {} not found when attempting to delete, may have been already deleted", username);
            throw new IOException("用户不存在: " + username);
        }

        // Check if user has nodes
        // userHasNodes now handles exceptions internally and returns false if user doesn't exist
        if (userHasNodes(username)) {
            throw new IOException("无法删除用户：该用户下还有设备节点，请先删除所有设备后再删除用户");
        }

        // Proceed with deletion using user ID for consistency
        deleteUserById(user.getId());
    }



    /**
     * Test if the Headscale server is reachable and API key is valid
     */
    public boolean testConnection() {
        try {
            getUsers();
            return true;
        } catch (Exception e) {
            log.error("Failed to test Headscale connection", e);
            return false;
        }
    }
}

