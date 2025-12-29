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

### Performance Comparison: Virtual Threads vs Platform Threads

#### Benchmark Results

**Test Setup**: 10,000 concurrent requests, each making a blocking database call

| Metric | Platform Threads | Virtual Threads | Improvement |
|--------|-----------------|-----------------|-------------|
| Memory Usage | ~10 GB (1MB × 10k) | ~200 MB | **50x less** |
| Startup Time | Same | Same | - |
| Throughput | 5,000 req/s | 50,000 req/s | **10x better** |
| Response Time (p99) | 2,000ms | 200ms | **10x faster** |

#### When to Use Virtual Threads

✅ **Use Virtual Threads When**:
- High number of concurrent, blocking I/O operations
- Using JDBC, RestTemplate, or other blocking APIs
- Microservices with many external API calls
- Traditional Spring MVC applications with high load

❌ **Don't Use Virtual Threads When**:
- CPU-intensive workloads (no benefit)
- Already using reactive programming (WebFlux)
- JVM version < 21

#### Practical Example: Before and After

**Before (Platform Threads)**:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        // This blocks a platform thread
        // With 200 threads max, only 200 concurrent requests possible
        return restTemplate.getForObject(
            "https://external-api/users/" + id, 
            User.class
        );
    }
}
```

**After (Virtual Threads - Spring Boot 3.2+)**:
```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true
```

```java
// Same code, but now runs on virtual threads!
// Can handle millions of concurrent requests
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        // Now runs on virtual thread
        // When blocked, virtual thread is "unmounted"
        // Carrier thread serves other virtual threads
        return restTemplate.getForObject(
            "https://external-api/users/" + id, 
            User.class
        );
    }
}
```

#### Threading Model Deep Dive

**Platform Threads**:
```
Request 1 → Platform Thread 1 (Blocks for 100ms during I/O) → Response 1
Request 2 → Platform Thread 2 (Blocks for 100ms during I/O) → Response 2
...
Request 201 → ❌ WAITING (No threads available)
```

**Virtual Threads**:
```
Request 1 → Virtual Thread 1 → Carrier Thread A → I/O starts
  → Virtual Thread unmounts
  → Carrier Thread A picks up Virtual Thread 1000 → ...
Request 1 I/O completes → Virtual Thread 1 remounts → Response 1

Effectively handles 1M+ concurrent requests with ~10 carrier threads!
```

#### Configuration Best Practices

```yaml
spring:
  threads:
    virtual:
      enabled: true
  
  # Tomcat configuration for virtual threads
  tomcat:
    threads:
      max: 200  # This becomes carrier thread pool size
      min-spare: 10
```

```java
@Configuration
public class VirtualThreadConfig {
    
    // Custom executor using virtual threads
    @Bean(name = "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    // For @Async methods
    @Bean
    public TaskExecutor taskExecutor() {
        return new TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
```

#### Monitoring Virtual Threads

```java
@RestController
@RequestMapping("/actuator/custom")
public class ThreadMetricsController {
    
    @GetMapping("/thread-info")
    public Map<String, Object> getThreadInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Get all threads
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        
        long virtualThreadCount = Arrays.stream(threads)
            .filter(Thread::isVirtual)
            .count();
        
        long platformThreadCount = Arrays.stream(threads)
            .filter(t -> !t.isVirtual())
            .count();
        
        info.put("virtualThreads", virtualThreadCount);
        info.put("platformThreads", platformThreadCount);
        info.put("totalThreads", Thread.activeCount());
        
        return info;
    }
}
```

#### Common Pitfalls

❌ **Don't Pin Virtual Threads**:
```java
// BAD: synchronized blocks pin virtual threads to carrier threads
public synchronized void badMethod() {
    // Blocking operation - virtual thread stays on carrier
    callExternalAPI();
}

// GOOD: Use ReentrantLock instead
private final ReentrantLock lock = new ReentrantLock();

public void goodMethod() {
    lock.lock();
    try {
        callExternalAPI();  // Virtual thread can unmount
    } finally {
        lock.unlock();
    }
}
```

❌ **Don't Use ThreadLocal Heavily**:
```java
// BAD: With millions of virtual threads, this wastes memory
ThreadLocal<UserContext> context = new ThreadLocal<>();

// GOOD: Use scoped values (JEP 429 - Preview in Java 20)
ScopedValue<UserContext> context = ScopedValue.newInstance();
```

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

### Why Problem Details?

**Before (Inconsistent Error Responses)**:
```json
// Error from Service A
{
  "error": "User not found",
  "code": 404
}

// Error from Service B
{
  "message": "Invalid request",
  "status": 400,
  "errors": ["Field 'email' is required"]
}

// Different formats, hard for clients to handle!
```

**After (RFC 7807 - Standardized)**:
```json
{
  "type": "https://api.example.com/errors/user-not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "User with ID 123 does not exist",
  "instance": "/api/users/123",
  "timestamp": "2024-01-15T10:30:00Z",
  "traceId": "abc123"
}
```

### Enabling Problem Details
```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

### Complete Practical Implementation

#### 1. Custom Problem Detail Factory

```java
@Component
public class CustomProblemDetailFactory {
    
    public ProblemDetail createProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            String type,
            URI instance) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://api.example.com/errors/" + type));
        problemDetail.setInstance(instance);
        
        // Add custom properties
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", getCurrentTraceId());
        
        return problemDetail;
    }
    
    private String getCurrentTraceId() {
        // Get from MDC or Micrometer tracing
        return MDC.get("traceId");
    }
}
```

#### 2. Global Exception Handler with Problem Details

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    @Autowired
    private CustomProblemDetailFactory problemDetailFactory;
    
    // Handle custom business exceptions
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request) {
        
        log.error("User not found: {}", ex.getMessage());
        
        ProblemDetail pd = problemDetailFactory.createProblemDetail(
            HttpStatus.NOT_FOUND,
            "User Not Found",
            ex.getMessage(),
            "user-not-found",
            URI.create(request.getRequestURI())
        );
        
        pd.setProperty("userId", ex.getUserId());
        return pd;
    }
    
    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        
        ProblemDetail pd = problemDetailFactory.createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            "Request validation failed. Please check the errors.",
            "validation-error",
            URI.create(request.getRequestURI())
        );
        
        pd.setProperty("errors", errors);
        pd.setProperty("errorCount", errors.size());
        
        return pd;
    }
    
    // Handle unauthorized access
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        ProblemDetail pd = problemDetailFactory.createProblemDetail(
            HttpStatus.FORBIDDEN,
            "Access Denied",
            "You don't have permission to access this resource",
            "access-denied",
            URI.create(request.getRequestURI())
        );
        
        pd.setProperty("requiredRole", ex.getMessage());
        return pd;
    }
    
    // Handle duplicate resource
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        
        ProblemDetail pd = problemDetailFactory.createProblemDetail(
            HttpStatus.CONFLICT,
            "Resource Already Exists",
            ex.getMessage(),
            "duplicate-resource",
            URI.create(request.getRequestURI())
        );
        
        pd.setProperty("existingResourceId", ex.getExistingId());
        return pd;
    }
    
    // Handle rate limiting
    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(ex.getLimit()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", String.valueOf(ex.getResetTime()));
        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfter()));
        
        ProblemDetail pd = problemDetailFactory.createProblemDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Rate Limit Exceeded",
            "You have exceeded the rate limit. Please try again later.",
            "rate-limit-exceeded",
            URI.create(request.getRequestURI())
        );
        
        pd.setProperty("limit", ex.getLimit());
        pd.setProperty("resetTime", ex.getResetTime());
        pd.setProperty("retryAfter", ex.getRetryAfter());
        
        return pd;
    }
    
    // Handle generic exceptions
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        ProblemDetail pd = problemDetailFactory.createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            "internal-error",
            URI.create(request.getRequestURI())
        );
        
        // Don't expose internal error details in production
        if (isDevEnvironment()) {
            pd.setProperty("exceptionClass", ex.getClass().getName());
            pd.setProperty("stackTrace", Arrays.toString(ex.getStackTrace()));
        }
        
        return pd;
    }
    
    private boolean isDevEnvironment() {
        // Check if running in development mode
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}
```

#### 3. Custom Exception Classes

```java
@Getter
public class UserNotFoundException extends RuntimeException {
    private final Long userId;
    
    public UserNotFoundException(Long userId) {
        super("User with ID " + userId + " not found");
        this.userId = userId;
    }
}

@Getter
public class DuplicateResourceException extends RuntimeException {
    private final Long existingId;
    
    public DuplicateResourceException(String message, Long existingId) {
        super(message);
        this.existingId = existingId;
    }
}

@Getter
public class RateLimitExceededException extends RuntimeException {
    private final int limit;
    private final long resetTime;
    private final int retryAfter;
    
    public RateLimitExceededException(int limit, long resetTime, int retryAfter) {
        super("Rate limit exceeded");
        this.limit = limit;
        this.resetTime = resetTime;
        this.retryAfter = retryAfter;
    }
}
```

#### 4. Client-Side Consumption

```java
@Service
@Slf4j
public class ApiClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public User getUser(Long id) {
        try {
            return restTemplate.getForObject(
                "https://api.example.com/users/" + id, 
                User.class
            );
        } catch (HttpClientErrorException ex) {
            handleProblemDetail(ex);
            throw ex;
        }
    }
    
    private void handleProblemDetail(HttpClientErrorException ex) {
        String responseBody = ex.getResponseBodyAsString();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode problemDetail = mapper.readTree(responseBody);
            
            String type = problemDetail.get("type").asText();
            String title = problemDetail.get("title").asText();
            String detail = problemDetail.get("detail").asText();
            int status = problemDetail.get("status").asInt();
            
            log.error("API Error - Type: {}, Title: {}, Detail: {}, Status: {}", 
                type, title, detail, status);
            
            // Handle specific error types
            if (type.contains("user-not-found")) {
                // Handle user not found
                Long userId = problemDetail.get("userId").asLong();
                log.warn("User {} not found", userId);
            } else if (type.contains("rate-limit-exceeded")) {
                // Handle rate limiting
                int retryAfter = problemDetail.get("retryAfter").asInt();
                log.warn("Rate limited. Retry after {} seconds", retryAfter);
            }
            
        } catch (Exception e) {
            log.error("Failed to parse problem detail", e);
        }
    }
}
```

#### 5. Testing Problem Details

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProblemDetailTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldReturnProblemDetailForNotFound() throws Exception {
        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://api.example.com/errors/user-not-found"))
            .andExpect(jsonPath("$.title").value("User Not Found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").exists())
            .andExpect(jsonPath("$.instance").value("/api/users/999"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.userId").value(999));
    }
    
    @Test
    void shouldReturnProblemDetailForValidationError() throws Exception {
        String invalidUser = """
            {
                "email": "invalid-email",
                "age": -1
            }
            """;
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUser))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.example.com/errors/validation-error"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errorCount").value(2));
    }
}
```

### Benefits of Problem Details

✅ **Standardized Format**: All errors follow the same structure  
✅ **Machine-Readable**: Clients can parse and handle errors programmatically  
✅ **Human-Friendly**: Clear titles and details for debugging  
✅ **Extensible**: Add custom properties without breaking the standard  
✅ **Traceable**: Include trace IDs for debugging distributed systems  
✅ **SEO-Friendly**: Type URLs can link to documentation

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

### Production Deployment Guide

#### Why Native Images?

**Traditional JVM**:
```
Startup: 5-10 seconds
Memory: 200-500 MB (initial heap)
Warmup: Needs time for JIT optimization
Container Size: 250+ MB
```

**GraalVM Native Image**:
```
Startup: 0.05-0.1 seconds (50-100ms)
Memory: 20-50 MB (no heap overhead)
Warmup: Instant peak performance
Container Size: 50-100 MB
```

#### Performance Comparison

| Metric | Traditional JVM | Native Image | Improvement |
|--------|----------------|--------------|-------------|
| Startup Time | 5-10s | 0.05-0.1s | **100x faster** |
| Memory Usage | 200-500 MB | 20-50 MB | **10x less** |
| Container Size | 250+ MB | 50-100 MB | **3-5x smaller** |
| Peak Performance | Higher (after warmup) | Good (immediate) | JIT wins long-running |
| Build Time | 30s | 5-10 min | Native slower |

#### When to Use Native Images

✅ **Perfect For**:
- **Serverless** (AWS Lambda, Google Cloud Functions)
- **Kubernetes** with scale-to-zero
- **CLI tools** and utilities
- **Microservices** with fast startup needs
- **Cost optimization** (less memory = cheaper)

❌ **Not Ideal For**:
- Long-running monoliths (JIT may outperform over time)
- Heavy reflection usage (requires hints)
- Dynamic class loading
- Applications with frequent code changes (long build times)

#### Complete Native Image Setup

##### 1. Prerequisites

```bash
# Install GraalVM
# Using SDKMAN (Recommended)
sdk install java 21-graalce
sdk use java 21-graalce

# Or download from https://www.graalvm.org/downloads/

# Verify installation
java -version
# Should show: GraalVM CE 21
```

##### 2. Maven Configuration

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<dependencies>
    <!-- Your regular dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>

<!-- Native profile -->
<profiles>
    <profile>
        <id>native</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.graalvm.buildtools</groupId>
                    <artifactId>native-maven-plugin</artifactId>
                    <configuration>
                        <imageName>${project.artifactId}</imageName>
                        <buildArgs>
                            <buildArg>--verbose</buildArg>
                            <buildArg>-H:+ReportExceptionStackTraces</buildArg>
                            <buildArg>--no-fallback</buildArg>
                            <!-- Memory settings -->
                            <buildArg>-J-Xmx8g</buildArg>
                        </buildArgs>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

##### 3. Build Native Image

```bash
# Clean build
./mvnw clean package -Pnative

# This takes 5-10 minutes
# Output: target/your-app (native executable)

# Test the native executable
./target/your-app

# Check startup time (should be ~50-100ms)
```

##### 4. Reflection Configuration

**Problem**: Native images don't support reflection by default

**Solution**: Register classes for reflection

```java
// Option 1: Using @RegisterReflectionForBinding
@RestController
@RegisterReflectionForBinding({User.class, Order.class})
public class UserController {
    // Methods that use reflection
}

// Option 2: RuntimeHints (More control)
@Configuration
public class MyRuntimeHints implements RuntimeHintsRegistrar {
    
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Register for reflection
        hints.reflection().registerType(
            User.class,
            MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
            MemberCategory.INVOKE_PUBLIC_METHODS,
            MemberCategory.DECLARED_FIELDS
        );
        
        // Register resource patterns
        hints.resources().registerPattern("data/*.json");
        
        // Register for serialization
        hints.serialization().registerType(User.class);
        
        // Register JNI access
        hints.jni().registerType(MyNativeClass.class);
    }
}

// Register the hints
@ImportRuntimeHints(MyRuntimeHints.class)
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

##### 5. Testing Native Images

```java
// Use @SpringBootTest with native profile
@SpringBootTest
@ActiveProfiles("test")
class UserControllerNativeTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldWorkInNativeImage() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());
    }
}

// Run native tests
// ./mvnw test -PnativeTest
```

##### 6. Docker Multi-Stage Build for Native Image

```dockerfile
# Stage 1: Build native image
FROM ghcr.io/graalvm/native-image-community:21 AS builder

WORKDIR /build

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Install Maven
RUN microdnf install -y wget tar && \
    wget https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz && \
    tar xzf apache-maven-3.9.5-bin.tar.gz && \
    ln -s /build/apache-maven-3.9.5/bin/mvn /usr/bin/mvn

# Build native image
RUN mvn clean package -Pnative -DskipTests

# Stage 2: Runtime
FROM debian:bookworm-slim

# Install required libraries
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ca-certificates && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy native executable
COPY --from=builder /build/target/my-app ./application

# Create non-root user
RUN useradd -r -u 1001 appuser && \
    chown appuser:appuser /app
USER appuser

EXPOSE 8080

# Run native image
ENTRYPOINT ["./application"]
```

**Build and run**:
```bash
# Build Docker image
docker build -t my-app:native .

# Run container
docker run -p 8080:8080 my-app:native

# Check memory usage (should be ~50MB)
docker stats
```

##### 7. Optimization Tips

```xml
<!-- pom.xml - Optimize build -->
<configuration>
    <buildArgs>
        <!-- Reduce build time -->
        <buildArg>-Ob</buildArg>  <!-- Quick build mode -->
        
        <!-- Memory optimization -->
        <buildArg>--gc=serial</buildArg>  <!-- Use serial GC -->
        
        <!-- Size optimization -->
        <buildArg>--no-fallback</buildArg>
        <buildArg>-H:+StaticExecutableWithDynamicLibC</buildArg>
        
        <!-- Debugging -->
        <buildArg>-H:+ReportExceptionStackTraces</buildArg>
        <buildArg>--verbose</buildArg>
    </buildArgs>
</configuration>
```

##### 8. Common Issues and Solutions

**Issue 1: ClassNotFoundException at Runtime**
```java
// Solution: Register class for reflection
@RegisterReflectionForBinding(MyClass.class)
```

**Issue 2: Resources Not Found**
```java
// Solution: Register resource pattern
@Configuration
@ImportRuntimeHints(MyResourceHints.class)
public class AppConfig {
    
    static class MyResourceHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("templates/*");
            hints.resources().registerPattern("static/**");
        }
    }
}
```

**Issue 3: Slow Build Times**
```bash
# Solution: Use Buildpacks (faster incremental builds)
./mvnw spring-boot:build-image -Pnative

# Or use GraalVM Enterprise (faster builds, requires license)
```

**Issue 4: Large Binary Size**
```bash
# Solution: Strip debug symbols
upx --best --lzma target/my-app

# Before: 80MB
# After: 25MB
```

##### 9. Monitoring Native Images

```java
@Configuration
public class NativeMetricsConfig {
    
    @Bean
    public MeterBinder nativeImageMetrics() {
        return (registry) -> {
            // Memory metrics
            Gauge.builder("native.memory.used", 
                Runtime.getRuntime(), Runtime::totalMemory)
                .baseUnit("bytes")
                .register(registry);
            
            // Startup time
            Gauge.builder("native.startup.time", 
                () -> ManagementFactory.getRuntimeMXBean().getUptime())
                .baseUnit("milliseconds")
                .register(registry);
        };
    }
}
```

##### 10. Production Deployment Checklist

```yaml
# application-prod.yml
spring:
  threads:
    virtual:
      enabled: true  # Use with native for max performance

# JVM arguments (if needed)
java:
  opts: |
    -XX:MaxRAMPercentage=75.0
    -XX:+ExitOnOutOfMemoryError
```

**Kubernetes Deployment**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app-native
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: app
        image: my-app:native
        resources:
          requests:
            memory: "64Mi"    # Native uses less memory
            cpu: "100m"
          limits:
            memory: "128Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 1  # Fast startup!
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 1  # Ready immediately
```

##### 11. Cost Savings Analysis

**Example: 1000 microservice instances**

| Metric | JVM | Native Image | Savings |
|--------|-----|--------------|---------|
| Memory per instance | 512 MB | 64 MB | 87.5% |
| Total memory | 500 GB | 64 GB | **$3,000/month** |
| Cold start time | 5s | 0.05s | **Better UX** |
| Container pulls | Slow | Fast | **Faster deploys** |

##### 12. Limitations to Know

❌ **Not Supported**:
- Dynamic class loading (`Class.forName()` with unknown classes)
- JVMTI, JMX (limited support)
- InvokeDynamic (limited)
- Some JDBC drivers (check compatibility)

✅ **Supported** (with hints):
- Reflection (with registration)
- Serialization (with registration)
- Resources (with registration)
- Most Spring features

##### 13. Best Practices

1. **Test thoroughly**: Always test native builds
2. **Use GraalVM reachability metadata**: Community contributions for popular libraries
3. **Profile first**: Ensure native image benefits your use case
4. **Monitor memory**: Native images have different memory characteristics
5. **Keep builds fast**: Use CI caching for native-image builds

```bash
# Example: GitHub Actions caching
- name: Cache GraalVM
  uses: actions/cache@v3
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-graalvm-${{ hashFiles('**/pom.xml') }}
```

### Interview Tips for Native Images

**Q: When would you use Native Images over traditional JVM?**
> "Native images excel in scenarios with frequent cold starts like serverless or Kubernetes scale-to-zero. The 100x faster startup and 10x memory reduction provide significant cost savings. However, for long-running applications where peak throughput matters, the JIT compiler's adaptive optimization may outperform. I'd profile both to make data-driven decisions."

**Q: How do you handle reflection in Native Images?**
> "Spring Boot 3's AOT engine automatically generates most reflection hints at build time. For custom cases, I use `@RegisterReflectionForBinding` for simple scenarios or implement `RuntimeHintsRegistrar` for complex requirements, registering classes, resources, and serialization needs."
