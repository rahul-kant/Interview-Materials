# Spring Boot 3 & Spring 6 - New Features & Deep Dive

## Table of Contents
1. [Java 17+ Baseline](#java-17-baseline)
2. [Virtual Threads (Project Loom)](#virtual-threads-project-loom)
3. [Declarative HTTP Interfaces](#declarative-http-interfaces)
4. [Problem Details for HTTP APIs](#problem-details-for-http-apis)
5. [Micrometer Observability](#micrometer-observability)
6. [GraalVM Native Image Support](#graalvm-native-image-support)

---

## Java 17+ Baseline
Spring Boot 3.0 requires **Java 17** as a minimum version and supports **Java 21**.
- **Jakarta EE 9/10**: Migration from `javax.*` to `jakarta.*` namespace.
- **Records**: Better support for Java Records as DTOs and `@ConfigurationProperties`.

---

## Virtual Threads (Project Loom)

## 🏗️ Architecture Diagram

> [!TIP]
> **Interview Pro-Tip: "What is the biggest change in Spring Boot 3?"**
> While there are many, the **Jakarta EE 9/10 migration** (javax to jakarta) is the most impactful for existing apps. However, **Native Image support** and **Virtual Threads** are the most exciting for new, high-performance applications.

### 🔍 Deep Dive: Virtual Threads vs Platform Threads
- **Platform Threads**: Managed by the OS. 1:1 mapping. Expensive to create and switch. Blocking one thread stops the OS thread.
- **Virtual Threads**: Managed by the JVM. M:N mapping (many virtual threads on few carrier threads). Extremely lightweight. When a virtual thread blocks, the JVM "unmounts" it from the carrier thread, allowing other virtual threads to run.

### 🛠️ Complex Example: Declarative HTTP Client with Auth
```java
public interface GitHubClient {
    @GetExchange("/users/{username}")
    Mono<GitHubUser> getUser(@PathVariable String username);
}

@Configuration
public class GitHubConfig {
    @Bean
    GitHubClient gitHubClient(WebClient.Builder builder) {
        WebClient webClient = builder
            .baseUrl("https://api.github.com")
            .defaultHeader("Authorization", "Bearer " + token)
            .build();
        return HttpServiceProxyFactory
            .builder(WebClientAdapter.forClient(webClient))
            .build()
            .createClient(GitHubClient.class);
    }
}
```

## Virtual Threads (Project Loom)
Available in Spring Boot 3.2+ with Java 21.

### Why Virtual Threads?
Traditional platform threads are expensive (1MB stack size). Virtual threads are lightweight, allowing applications to handle millions of concurrent requests without the complexity of reactive programming.

### Enabling Virtual Threads
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### Impact
- **Blocking I/O**: You can now use blocking I/O (like standard JPA) and still achieve high scalability.
- **Tomcat/Jetty**: Automatically switches to using virtual threads for request handling.

---

## Declarative HTTP Interfaces

## 🏗️ Architecture Diagram

## Declarative HTTP Interfaces
Define HTTP services as Java interfaces, similar to Feign but built-in.

### Example
```java
public interface UserClient {
    @GetExchange("/users/{id}")
    User getUser(@PathVariable Long id);
}

// Configuration
@Configuration
public class ClientConfig {
    @Bean
    UserClient userClient(WebClient.Builder builder) {
        WebClient webClient = builder.baseUrl("https://api.example.com").build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builder(WebClientAdapter.forClient(webClient)).build();
        return factory.createClient(UserClient.class);
    }
}
```

---

## Problem Details for HTTP APIs
Implements [RFC 7807](https://tools.ietf.org/html/rfc7807) for standardized error responses.

### Enabling Problem Details
```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

### Customizing Errors
```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("User Not Found");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
```

---

## Micrometer Observability
Unified observation API for metrics, logging, and tracing.

### Usage
```java
@Service
public class MyService {
    private final ObservationRegistry registry;

    public MyService(ObservationRegistry registry) {
        this.registry = registry;
    }

    public void doWork() {
        Observation.createNotStarted("my.operation", registry)
            .observe(() -> {
                // Business logic
            });
    }
}
```

---

## GraalVM Native Image Support
Spring Boot 3 provides first-class support for compiling to native executables.
- **AOT (Ahead-of-Time)**: Optimizes the application during build time.
- **Benefits**: Sub-second startup, reduced memory usage.

### Build Command
```bash
./mvnw -Pnative native:compile
```
