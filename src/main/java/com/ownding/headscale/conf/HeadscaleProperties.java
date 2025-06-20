package com.ownding.headscale.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Headscale configuration properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "headscale.server")
public class HeadscaleProperties {

    /**
     * Headscale server URL (for REST API)
     */
    private String url = "http://localhost:8080";

    /**
     * Headscale API key
     */
    private String apiKey;

    /**
     * Request timeout in milliseconds
     */
    private int timeout = 30000;

    /**
     * Number of retry attempts for failed requests
     */
    private int retryAttempts = 3;

    /**
     * gRPC server host
     */
    private String grpcHost = "localhost";

    /**
     * gRPC server port
     */
    private int grpcPort = 50443;

    /**
     * Whether to use TLS for gRPC connection
     */
    private boolean grpcTls = false;

    /**
     * gRPC connection timeout in milliseconds
     */
    private int grpcTimeout = 30000;
}
