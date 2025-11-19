# Microservices & Cloud Native Spring - Deep Dive

## Table of Contents
1. [Microservices Architecture](#microservices-architecture)
2. [Spring Cloud Overview](#spring-cloud-overview)
3. [Service Discovery (Eureka)](#service-discovery-eureka)
4. [API Gateway (Spring Cloud Gateway)](#api-gateway-spring-cloud-gateway)
5. [Distributed Configuration](#distributed-configuration)
6. [Circuit Breakers (Resilience4j)](#circuit-breakers-resilience4j)
7. [Distributed Tracing](#distributed-tracing)
8. [Microservices Patterns](#microservices-patterns)

---

## Microservices Architecture

### Monolith vs Microservices

| Feature | Monolithic Architecture | Microservices Architecture |
|---------|-------------------------|----------------------------|
| **Structure** | Single codebase, single deployable unit | Multiple small, independent services |
| **Database** | Shared database | Database per service |
| **Scaling** | Scale the entire application | Scale individual services independently |
| **Technology** | Single technology stack | Polyglot (can use different stacks) |
| **Complexity** | Low initially, high as it grows | High operational complexity |
| **Deployment** | Risky (one bug can bring down everything) | Isolated (failures are contained) |

### Key Characteristics
1. **Independently Deployable**: Can deploy one service without affecting others.
2. **Loosely Coupled**: Services interact via well-defined APIs.
3. **Organized around Business Capabilities**: e.g., Order Service, User Service.
4. **Owned by Small Teams**: "Two-pizza teams".

---

## Spring Cloud Overview

Spring Cloud provides tools for developers to quickly build some of the common patterns in distributed systems.

### Key Components
- **Spring Cloud Netflix**: Eureka (Discovery), Hystrix (Circuit Breaker - Legacy), Zuul (Gateway - Legacy).
- **Spring Cloud Config**: Centralized configuration.
- **Spring Cloud Gateway**: API Gateway.
- **Spring Cloud Sleuth / Micrometer Tracing**: Distributed tracing.
- **Spring Cloud OpenFeign**: Declarative REST client.
- **Spring Cloud Stream**: Event-driven microservices.

---

## Service Discovery (Eureka)

In a dynamic environment, service instances have changing IP addresses. Service Discovery allows services to find each other without hardcoding URLs.

### 1. Eureka Server (The Registry)

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

**Application Class**:
```java
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
```

**application.yml**:
```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false # It's a server, don't register itself
    fetch-registry: false
```

### 2. Eureka Client (The Microservice)

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**Application Class**:
```java
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

**application.yml**:
```yaml
spring:
  application:
    name: user-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

## API Gateway (Spring Cloud Gateway)

The API Gateway acts as a single entry point for all clients. It handles routing, security, rate limiting, and monitoring.

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**application.yml**:
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true # Auto-create routes from Eureka
          lower-case-service-id: true
      routes:
        - id: user-service
          uri: lb://user-service # Load balanced URI
          predicates:
            - Path=/users/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/orders/**
```

### Cross-Cutting Concerns in Gateway
- **Authentication**: Validate JWT tokens.
- **Rate Limiting**: Prevent abuse.
- **Logging**: Log all incoming requests.

---

## Distributed Configuration

Manage configuration for all services in a central place (Git repository).

### 1. Config Server

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

**Application Class**:
```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**application.yml**:
```yaml
server:
  port: 8888

spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/my-org/config-repo
          default-label: main
```

### 2. Config Client

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

**bootstrap.yml** (or application.yml in newer versions with `spring.config.import`):
```yaml
spring:
  application:
    name: user-service
  config:
    import: optional:configserver:http://localhost:8888
```

---

## Circuit Breakers (Resilience4j)

Prevent cascading failures when a dependent service is down.

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

**Service Usage**:
```java
@Service
public class OrderService {

    private final RestTemplate restTemplate;

    public OrderService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUser")
    public UserDTO getUser(Long userId) {
        return restTemplate.getForObject("http://user-service/users/" + userId, UserDTO.class);
    }

    // Fallback method must have same signature + Exception
    public UserDTO fallbackGetUser(Long userId, Throwable t) {
        return new UserDTO(userId, "Default User", "default@example.com");
    }
}
```

**application.yml**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 5s
        failureRateThreshold: 50
```

---

## Distributed Tracing

Track a request as it flows through multiple microservices.

**Tools**:
- **Micrometer Tracing**: Facade for tracing.
- **Zipkin**: Visualization server.

**pom.xml**:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

**Concept**:
- **Trace ID**: Unique ID for the entire request chain.
- **Span ID**: Unique ID for a single operation/service call.

Logs will look like:
`[user-service, 65e9f9s8d7f, 89s7d8f9s7d, true] ...`
(AppName, TraceId, SpanId, Exportable)

---

## Microservices Patterns

### 1. SAGA Pattern (Distributed Transactions)

Since each service has its own DB, we can't use ACID transactions across services. SAGA is a sequence of local transactions.

- **Choreography**: Events trigger actions in other services.
  - Order Service: `OrderCreated` event.
  - Inventory Service: Listens to `OrderCreated`, reserves stock, emits `StockReserved`.
  - Payment Service: Listens to `StockReserved`, processes payment.
- **Orchestration**: A central coordinator (Saga Orchestrator) tells services what to do.

### 2. CQRS (Command Query Responsibility Segregation)

Separate read and write models.
- **Command**: Updates data (Write DB).
- **Query**: Reads data (Read DB / Materialized View).
- **Sync**: Events sync Write DB to Read DB.

### 3. API Composition

How to fetch data from multiple services?
- **API Gateway Composition**: Gateway calls Service A and Service B, combines results.
- **BFF (Backend For Frontend)**: Specific backend for mobile, web, etc., that aggregates data.

### 4. Event-Driven Architecture

Services communicate via asynchronous events (Kafka/RabbitMQ) instead of synchronous HTTP calls.
- **Producer**: Emits event.
- **Consumer**: Reacts to event.
- **Decoupling**: Producer doesn't know who consumes.

```java
// Producer (using Spring Cloud Stream)
@Bean
public Supplier<OrderEvent> orderSupplier() {
    return () -> new OrderEvent(orderId, "CREATED");
}

// Consumer
@Bean
public Consumer<OrderEvent> orderConsumer() {
    return event -> {
        System.out.println("Received order: " + event.getOrderId());
    };
}
```
