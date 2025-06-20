# Headscale Management API

[![Java](https://img.shields.io/badge/Java-1.8+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A comprehensive Spring Boot application that provides both REST API and gRPC interfaces for managing [Headscale](https://github.com/juanfont/headscale) servers. This project offers a unified hybrid service that intelligently chooses between REST and gRPC protocols based on feature availability and reliability.

## 🚀 Features

- **Hybrid API Support**: Seamlessly switches between REST API and gRPC based on operation requirements
- **User Management**: Create, list, delete, and manage Headscale users with display name support
- **Node Management**: Monitor and manage connected devices/nodes
- **Pre-Auth Key Management**: Generate and manage pre-authentication keys for device registration
- **Connection Testing**: Built-in health checks for both REST and gRPC connections
- **Intelligent Fallback**: Automatically falls back to REST API when gRPC is unavailable
- **Comprehensive Logging**: Detailed logging for debugging and monitoring

## 📋 Prerequisites

- Java 1.8 or higher
- Maven 3.6 or higher
- A running Headscale server with API access
- Valid Headscale API key

## 🛠️ Installation

### Clone the Repository

```bash
git clone https://github.com/OwnDing/headscale-java.git
cd headscale
```

### Build the Project

```bash
mvn clean compile
```

### Generate Protocol Buffer Classes

```bash
mvn protobuf:compile protobuf:compile-custom
```

### Package the Application

```bash
mvn clean package
```

## ⚙️ Configuration

Configure your Headscale server connection in `src/main/resources/application.properties`:

```properties
# Application name
spring.application.name=headscale

# Headscale REST API Configuration
headscale.server.url=https://your-headscale-server:8080
headscale.server.apiKey=your-api-key-here
headscale.server.timeout=30000
headscale.server.retryAttempts=3

# Headscale gRPC Configuration
headscale.server.grpcHost=your-headscale-server
headscale.server.grpcPort=50443
headscale.server.grpcTls=false
headscale.server.grpcTimeout=30000
```

### Configuration Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `headscale.server.url` | Headscale server REST API URL | `http://localhost:8080` |
| `headscale.server.apiKey` | Headscale API authentication key | - |
| `headscale.server.timeout` | REST API request timeout (ms) | `30000` |
| `headscale.server.retryAttempts` | Number of retry attempts | `3` |
| `headscale.server.grpcHost` | gRPC server hostname | `localhost` |
| `headscale.server.grpcPort` | gRPC server port | `50443` |
| `headscale.server.grpcTls` | Enable TLS for gRPC | `false` |
| `headscale.server.grpcTimeout` | gRPC connection timeout (ms) | `30000` |

## 🚀 Running the Application

### Development Mode

```bash
mvn spring-boot:run
```

### Production Mode

```bash
java -jar target/headscale-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080` by default.

## 📚 API Documentation

### REST Endpoints

#### Connection Testing
- `GET /headscale/test` - Test both REST and gRPC connections
- `GET /headscale/status` - Get detailed connection status

#### User Management
- `GET /headscale/users` - List all users
- `POST /headscale/users` - Create a new user
- `DELETE /headscale/users/{username}` - Delete a user (with safety checks)

#### Node Management
- `GET /headscale/nodes` - List all nodes
- `GET /headscale/nodes/user/{username}` - Get nodes for a specific user

#### Pre-Auth Keys
- `GET /headscale/preauthkeys/{username}` - Get pre-auth keys for a user
- `POST /headscale/preauthkeys` - Create a new pre-auth key

### Example API Calls

#### Test Connection
```bash
curl -X GET http://localhost:8080/headscale/test
```

#### List Users
```bash
curl -X GET http://localhost:8080/headscale/users
```

#### Create User
```bash
curl -X POST http://localhost:8080/headscale/users \
  -H "Content-Type: application/json" \
  -d '{"username": "newuser", "displayName": "New User"}'
```

## 🏗️ Architecture

The application follows a layered architecture:

- **Controller Layer**: REST API endpoints (`HeadscaleController`)
- **Service Layer**: 
  - `HeadscaleService`: REST API operations
  - `HeadscaleGrpcService`: gRPC operations
  - `HeadscaleHybridService`: Intelligent hybrid operations
- **Configuration**: `HeadscaleProperties` for server configuration
- **Data Transfer Objects**: VOCs for API data exchange

### Hybrid Service Strategy

The `HeadscaleHybridService` implements an intelligent routing strategy:

1. **gRPC First**: Attempts gRPC for operations requiring advanced features (e.g., display names)
2. **REST Fallback**: Falls back to REST API for standard operations
3. **Error Handling**: Graceful degradation with detailed error reporting

## 🧪 Testing

### Run Unit Tests

```bash
mvn test
```

### Integration Testing

The application includes connection testing endpoints to verify both REST and gRPC connectivity:

```bash
# Test all connections
curl http://localhost:8080/headscale/test

# Get detailed status
curl http://localhost:8080/headscale/status
```

## 📦 Dependencies

### Core Dependencies
- **Spring Boot 2.7.18**: Web framework and dependency injection
- **Lombok**: Reduces boilerplate code
- **FastJSON 1.2.58**: JSON processing
- **OkHttp 3.14.9**: HTTP client for REST API calls
- **Apache Commons Lang3 3.9**: Utility functions

### gRPC Dependencies
- **gRPC Netty Shaded 1.53.0**: gRPC runtime
- **gRPC Protobuf 1.53.0**: Protocol buffer support
- **gRPC Stub 1.53.0**: Generated stub classes
- **Protobuf Java 3.21.12**: Protocol buffer runtime

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Headscale](https://github.com/juanfont/headscale) - The open-source Tailscale control server
- [Spring Boot](https://spring.io/projects/spring-boot) - The application framework
- [gRPC](https://grpc.io/) - High-performance RPC framework

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/OwnDing/headscale-java/issues) page
2. Create a new issue with detailed information
3. Include logs and configuration (without sensitive data)

---

**Note**: This project is not officially affiliated with Headscale or Tailscale. It's a community-driven management tool for Headscale servers.
