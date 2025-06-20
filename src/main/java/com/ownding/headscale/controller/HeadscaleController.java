package com.ownding.headscale.controller;


import com.ownding.headscale.common.constant.ApiCode;
import com.ownding.headscale.dal.vo.HeadscaleNode;
import com.ownding.headscale.dal.vo.HeadscalePreAuthKey;
import com.ownding.headscale.dal.vo.HeadscaleUser;
import com.ownding.headscale.dal.vo.Result;
import com.ownding.headscale.service.HeadscaleGrpcService;
import com.ownding.headscale.service.HeadscaleHybridService;
import com.ownding.headscale.service.HeadscaleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/headscale")
@Slf4j
public class HeadscaleController {

    @Autowired
    private HeadscaleService headscaleService;

    @Autowired
    private HeadscaleHybridService hybridService;

    @Autowired
    private HeadscaleGrpcService grpcService;

    /**
     * Test Headscale connection (both REST and gRPC)
     */
    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public Result testConnection() {

        try {
            HeadscaleHybridService.ConnectionStatus status = hybridService.getConnectionStatus();

            if (status.isAnyAvailable()) {
                // Return the status message directly for better display
                return Result.success(status.getStatus());
            } else {
                return Result.toResult(ApiCode.FAIL, "所有Headscale连接都失败");
            }
        } catch (Exception e) {
            log.error("[HeadscaleController#testConnection] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "连接测试失败: " + e.getMessage());
        }
    }

    /**
     * Get detailed connection status
     */
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public Result getConnectionStatus() {

        try {
            HeadscaleHybridService.ConnectionStatus status = hybridService.getConnectionStatus();

            return Result.success(new Object() {
                public final boolean restAvailable = status.isRestAvailable();
                public final boolean grpcAvailable = status.isGrpcAvailable();
                public final String message = status.getStatus();
                public final String restStatus = status.isRestAvailable() ? "可用" : "不可用";
                public final String grpcStatus = status.isGrpcAvailable() ? "可用" : "不可用";
            });
        } catch (Exception e) {
            log.error("[HeadscaleController#getConnectionStatus] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "获取连接状态失败: " + e.getMessage());
        }
    }

    /**
     * Create namespace using gRPC API
     */
    @RequestMapping(value = "/namespaces", method = RequestMethod.POST)
    public Result createNamespace(@RequestParam("namespaceName") String namespaceName) {

        if (StringUtils.isBlank(namespaceName)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "命名空间名称不能为空");
        }

        // 校验命名空间名称格式
        if (!namespaceName.matches("^[a-zA-Z0-9_-]+$")) {
            return Result.toResult(ApiCode.BAD_REQUEST, "命名空间名称只能包含字母、数字、下划线和连字符");
        }

        try {
            hybridService.createNamespace(namespaceName);
            return Result.success("命名空间创建成功");
        } catch (Exception e) {
            log.error("[HeadscaleController#createNamespace] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "创建命名空间失败: " + e.getMessage());
        }
    }

    /**
     * Get all users
     */
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public Result getUsers() {

        try {
            List<HeadscaleUser> users = headscaleService.getUsers();
            return Result.success(users);
        } catch (Exception e) {
            log.error("[HeadscaleController#getUsers] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * Create a new user with optional displayName (namespace)
     */
    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public Result createUser(@RequestParam("username") String username,
                             @RequestParam(value = "displayName", required = false) String displayName) {

        if (StringUtils.isBlank(username)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能为空");
        }

        // 校验用户名不能是纯数字
        if (username.matches("^\\d+$")) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能是纯数字，请使用包含字母的用户名");
        }

        // 校验用户名格式
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名只能包含字母、数字、下划线和连字符");
        }

        // 校验displayName格式（如果提供）
        if (StringUtils.isNotBlank(displayName)) {
            if (!displayName.matches("^[a-zA-Z0-9_-]+$")) {
                return Result.toResult(ApiCode.BAD_REQUEST, "显示名称只能包含字母、数字、下划线和连字符");
            }
        }

        try {
            HeadscaleUser user;
            if (StringUtils.isNotBlank(displayName)) {
                log.info("Creating user with displayName: username={}, displayName={}", username, displayName);
                user = hybridService.createUser(username, displayName);
            } else {
                log.info("Creating user without displayName: username={}", username);
                user = hybridService.createUser(username);
            }
            return Result.success(user);
        } catch (Exception e) {
            log.error("[HeadscaleController#createUser] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "创建用户失败: " + e.getMessage());
        }
    }

    /**
     * Delete a user
     */
    @RequestMapping(value = "/users/{username}", method = RequestMethod.DELETE)
    public Result deleteUser(@PathVariable("username") String username) {

        if (StringUtils.isBlank(username)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能为空");
        }

        try {
            headscaleService.deleteUserSafely(username);
            return Result.success("用户删除成功");
        } catch (Exception e) {
            log.error("[HeadscaleController#deleteUser] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Check if user can be deleted (has no nodes)
     */
    @RequestMapping(value = "/users/{username}/can-delete", method = RequestMethod.GET)
    public Result canDeleteUser(@PathVariable("username") String username) {

        if (StringUtils.isBlank(username)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能为空");
        }

        try {
            boolean hasNodes = headscaleService.userHasNodes(username);
            return Result.success(new Object() {
                public final boolean canDelete = !hasNodes;
                public final String message = hasNodes ? "该用户下还有设备节点，无法删除" : "可以删除";
            });
        } catch (Exception e) {
            log.error("[HeadscaleController#canDeleteUser] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "检查失败: " + e.getMessage());
        }
    }

    /**
     * Get pre-auth keys for a user
     */
    @RequestMapping(value = "/users/{username}/preauth-keys", method = RequestMethod.GET)
    public Result getPreAuthKeys(@PathVariable("username") String username) {

        if (StringUtils.isBlank(username)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能为空");
        }

        try {
            log.info("Getting pre-auth keys for user: {}", username);
            List<HeadscalePreAuthKey> keys = headscaleService.getPreAuthKeys(username);
            log.info("Successfully retrieved {} pre-auth keys for user: {}", keys != null ? keys.size() : 0, username);
            return Result.success(keys);
        } catch (Exception e) {
            log.error("[HeadscaleController#getPreAuthKeys] exception for user: {}", username, e);
            return Result.toResult(ApiCode.SERVER_ERROR, "获取预授权密钥失败: " + e.getMessage());
        }
    }

    /**
     * Create a pre-auth key for a user
     */
    @RequestMapping(value = "/users/{username}/preauth-keys", method = RequestMethod.POST)
    public Result createPreAuthKey(@PathVariable("username") String username,
                                   @RequestParam(value = "reusable", defaultValue = "false") Boolean reusable,
                                   @RequestParam(value = "ephemeral", defaultValue = "false") Boolean ephemeral) {

        if (StringUtils.isBlank(username)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能为空");
        }

        try {
            HeadscalePreAuthKey key = headscaleService.createPreAuthKey(username, reusable, ephemeral);
            return Result.success(key);
        } catch (Exception e) {
            log.error("[HeadscaleController#createPreAuthKey] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "创建预授权密钥失败: " + e.getMessage());
        }
    }

    /**
     * Get all nodes/devices
     */
    @RequestMapping(value = "/nodes", method = RequestMethod.GET)
    public Result getNodes() {

        try {
            List<HeadscaleNode> nodes = headscaleService.getNodes();
            return Result.success(nodes);
        } catch (Exception e) {
            log.error("[HeadscaleController#getNodes] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "获取设备节点失败: " + e.getMessage());
        }
    }

    /**
     * Get nodes for a specific user
     */
    @RequestMapping(value = "/users/{username}/nodes", method = RequestMethod.GET)
    public Result getNodesByUser(@PathVariable("username") String username) {

        if (StringUtils.isBlank(username)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能为空");
        }

        try {
            List<HeadscaleNode> nodes = headscaleService.getNodesByUser(username);
            return Result.success(nodes);
        } catch (Exception e) {
            log.error("[HeadscaleController#getNodesByUser] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "获取用户设备节点失败: " + e.getMessage());
        }
    }

    /**
     * Delete a node
     */
    @RequestMapping(value = "/nodes/{nodeId}", method = RequestMethod.DELETE)
    public Result deleteNode(@PathVariable("nodeId") String nodeId) {

        if (StringUtils.isBlank(nodeId)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "节点ID不能为空");
        }

        try {
            headscaleService.deleteNode(nodeId);
            return Result.success("设备节点删除成功");
        } catch (Exception e) {
            log.error("[HeadscaleController#deleteNode] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "删除设备节点失败: " + e.getMessage());
        }
    }

    /**
     * Get online devices count and status
     */
    @RequestMapping(value = "/nodes/status", method = RequestMethod.GET)
    public Result getNodesStatus() {

        try {
            List<HeadscaleNode> nodes = headscaleService.getNodes();

            long onlineCount = nodes.stream().filter(node -> Boolean.TRUE.equals(node.getOnline())).count();
            long totalCount = nodes.size();
            long offlineCount = totalCount - onlineCount;

            return Result.success(new Object() {
                public final long total = totalCount;
                public final long online = onlineCount;
                public final long offline = offlineCount;
            });
        } catch (Exception e) {
            log.error("[HeadscaleController#getNodesStatus] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "获取设备状态失败: " + e.getMessage());
        }
    }

    /**
     * Get current ACL policy
     */
    @RequestMapping(value = "/acl", method = RequestMethod.GET)
    public Result getACLPolicy() {

        try {
            String aclPolicy = headscaleService.getACLPolicy();
            return Result.success(aclPolicy);
        } catch (Exception e) {
            log.error("[HeadscaleController#getACLPolicy] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "获取ACL策略失败: " + e.getMessage());
        }
    }

    /**
     * Update ACL policy
     */
    @RequestMapping(value = "/acl", method = RequestMethod.PUT)
    public Result updateACLPolicy(@RequestBody String aclPolicyJson) {

        if (StringUtils.isBlank(aclPolicyJson)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "ACL策略不能为空");
        }

        try {
            String updatedPolicy = headscaleService.updateACLPolicy(aclPolicyJson);
            return Result.success(updatedPolicy);
        } catch (Exception e) {
            log.error("[HeadscaleController#updateACLPolicy] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "更新ACL策略失败: " + e.getMessage());
        }
    }





    /**
     * Get gRPC diagnostics information
     */
    @RequestMapping(value = "/grpc/diagnostics", method = RequestMethod.GET)
    public Result getGrpcDiagnostics() {

        try {
            String diagnostics = grpcService.getGrpcDiagnostics();
            return Result.success(diagnostics);
        } catch (Exception e) {
            log.error("[HeadscaleController#getGrpcDiagnostics] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "获取gRPC诊断信息失败: " + e.getMessage());
        }
    }

    /**
     * Test different gRPC connection modes
     */
    @RequestMapping(value = "/grpc/test-modes", method = RequestMethod.GET)
    public Result testGrpcConnectionModes() {

        try {
            String results = grpcService.testConnectionModes();
            return Result.success(results);
        } catch (Exception e) {
            log.error("[HeadscaleController#testGrpcConnectionModes] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "测试gRPC连接模式失败: " + e.getMessage());
        }
    }

    /**
     * Test basic gRPC connectivity
     */
    @RequestMapping(value = "/grpc/test-basic", method = RequestMethod.GET)
    public Result testBasicGrpcConnectivity() {

        try {
            String results = grpcService.testBasicConnectivity();
            return Result.success(results);
        } catch (Exception e) {
            log.error("[HeadscaleController#testBasicGrpcConnectivity] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "测试基本gRPC连接失败: " + e.getMessage());
        }
    }

    /**
     * Test specific gRPC port
     */
    @RequestMapping(value = "/grpc/test-port", method = RequestMethod.GET)
    public Result testGrpcPort(@RequestParam("port") int port,
                               @RequestParam(value = "tls", defaultValue = "false") boolean useTls) {

        try {
            String results = grpcService.testSpecificPort(port, useTls);
            return Result.success(results);
        } catch (Exception e) {
            log.error("[HeadscaleController#testGrpcPort] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "测试gRPC端口失败: " + e.getMessage());
        }
    }

    /**
     * Test gRPC user creation with displayName (for debugging)
     */
    @RequestMapping(value = "/users/test-grpc-create", method = RequestMethod.POST)
    public Result testGrpcCreateUser(@RequestParam("username") String username,
                                     @RequestParam(value = "displayName", required = false) String displayName) {

        if (StringUtils.isBlank(username)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能为空");
        }

        // 校验用户名不能是纯数字
        if (username.matches("^\\d+$")) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能是纯数字，请使用包含字母的用户名");
        }

        // 校验用户名格式
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名只能包含字母、数字、下划线和连字符");
        }

        // 校验displayName格式（如果提供）
        if (StringUtils.isNotBlank(displayName)) {
            if (!displayName.matches("^[a-zA-Z0-9_-]+$")) {
                return Result.toResult(ApiCode.BAD_REQUEST, "显示名称只能包含字母、数字、下划线和连字符");
            }
        }

        try {
            HeadscaleUser user;
            if (StringUtils.isNotBlank(displayName)) {
                log.info("Testing gRPC user creation with displayName: username={}, displayName={}", username, displayName);
                user = hybridService.createUser(username, displayName);
            } else {
                log.info("Testing gRPC user creation without displayName: username={}", username);
                user = hybridService.createUser(username);
            }
            return Result.success(user);
        } catch (Exception e) {
            log.error("[HeadscaleController#testGrpcCreateUser] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "测试gRPC用户创建失败: " + e.getMessage());
        }
    }

    /**
     * Test simple user creation (for debugging)
     */
    @RequestMapping(value = "/users/test-create", method = RequestMethod.POST)
    public Result testCreateUser(@RequestParam("username") String username) {

        if (StringUtils.isBlank(username)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能为空");
        }

        // 校验用户名不能是纯数字
        if (username.matches("^\\d+$")) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能是纯数字，请使用包含字母的用户名");
        }

        // 校验用户名格式
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名只能包含字母、数字、下划线和连字符");
        }

        try {
            log.info("Testing simple user creation for username: {}", username);
            HeadscaleUser user = headscaleService.createUser(username);
            log.info("Test user creation successful: {}", user.getName());
            return Result.success(user);
        } catch (Exception e) {
            log.error("[HeadscaleController#testCreateUser] exception", e);
            return Result.toResult(ApiCode.SERVER_ERROR, "测试创建用户失败: " + e.getMessage());
        }
    }

    /**
     * Debug endpoint to test pre-auth keys API call
     */
    @RequestMapping(value = "/debug/users/{username}/preauth-keys", method = RequestMethod.GET)
    public Result debugGetPreAuthKeys(@PathVariable("username") String username) {

        if (StringUtils.isBlank(username)) {
            return Result.toResult(ApiCode.BAD_REQUEST, "用户名不能为空");
        }

        try {
            log.info("Debug: Getting pre-auth keys for user: {}", username);

            // First check if user exists
            try {
                HeadscaleUser user = headscaleService.getUserByName(username);
                log.info("Debug: User found - ID: {}, Name: {}, DisplayName: {}",
                        user.getId(), user.getName(), user.getDisplayName());
            } catch (Exception e) {
                log.warn("Debug: User not found or error getting user: {}", e.getMessage());
                return Result.toResult(ApiCode.BAD_REQUEST, "用户不存在: " + username);
            }

            // Try to get pre-auth keys
            List<HeadscalePreAuthKey> keys = headscaleService.getPreAuthKeys(username);
            log.info("Debug: Successfully retrieved {} pre-auth keys for user: {}",
                    keys != null ? keys.size() : 0, username);

            final String finalUsername = username;
            final List<HeadscalePreAuthKey> finalKeys = keys;
            return Result.success(new Object() {
                public final String username = finalUsername;
                public final int keyCount = finalKeys != null ? finalKeys.size() : 0;
                public final List<HeadscalePreAuthKey> keys = finalKeys;
            });
        } catch (Exception e) {
            log.error("[HeadscaleController#debugGetPreAuthKeys] exception for user: {}", username, e);
            return Result.toResult(ApiCode.SERVER_ERROR, "调试获取预授权密钥失败: " + e.getMessage());
        }
    }
}

