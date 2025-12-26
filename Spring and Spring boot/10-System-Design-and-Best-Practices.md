# System Design & Best Practices - Deep Dive

## Table of Contents
1. [Performance Optimization](#performance-optimization)
2. [Design Patterns in Spring](#design-patterns-in-spring)
3. [Production Readiness](#production-readiness)
4. [Database Optimization](#database-optimization)
5. [Caching Strategies](#caching-strategies)
6. [JVM Tuning](#jvm-tuning)
7. [Distributed Tracing](#distributed-tracing)
8. [API Versioning](#api-versioning)
9. [Memory Management](#memory-management)

---

## Performance Optimization

## 🏗️ Architecture Diagram

> [!TIP]
> **Interview Pro-Tip: "How do you handle high-concurrency in Spring?"**
> 1. **Non-blocking I/O**: Use WebFlux for I/O bound tasks.
> 2. **Caching**: Use Redis to offload the DB.
> 3. **Connection Pooling**: Tune HikariCP.
> 4. **Async**: Use `@Async` for fire-and-forget tasks.
> 5. **Database Tuning**: Use Read-Replicas and Indexing.

### 🔍 Deep Dive: L1 vs L2 Cache in Hibernate
- **L1 Cache (Session Level)**: Always on. It's mandatory and bound to the `EntityManager` session. It prevents multiple queries for the same entity within the same transaction.
- **L2 Cache (SessionFactory Level)**: Optional. Shared across all sessions. Requires a provider like **Ehcache** or **Hazelcast**. It's great for "Read-Heavy" data that doesn't change often.

### 🛠️ Complex Example: Multi-Level Caching (Redis + Caffeine)
```java
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Caffeine for L1 (In-memory, ultra-fast)
        CaffeineCacheManager caffeine = new CaffeineCacheManager("local-cache");
        
        // Redis for L2 (Distributed, shared)
        RedisCacheManager redis = RedisCacheManager.builder(connectionFactory).build();
        
        return new CompositeCacheManager(caffeine, redis);
    }
}
```

## Performance Optimization

### 1. Connection Pooling (HikariCP)

Spring Boot uses HikariCP by default, which is very fast.

**Configuration**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20 # Default is 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000
```

### 2. Asynchronous Processing

Offload long-running tasks to a separate thread pool.

**Enable Async**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
```

**Usage**:
```java
@Service
public class EmailService {
    
    @Async("taskExecutor")
    public CompletableFuture<String> sendEmail(String recipient) {
        // Simulate delay
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        return CompletableFuture.completedFuture("Email sent");
    }
}
```

### 3. Lazy Initialization

Delays bean creation until needed. Reduces startup time but may delay first request.

**Global Config**:
```yaml
spring:
  main:
    lazy-initialization: true
```

**Per Bean**:
```java
@Component
@Lazy
public class HeavyBean { ... }
```

---

## Design Patterns in Spring

## 🏗️ Architecture Diagram

## Design Patterns in Spring

### 1. Singleton Pattern
- **Concept**: One instance per container.
- **Spring**: Default bean scope.
- **Usage**: Stateless services, repositories.

### 2. Factory Pattern
- **Concept**: Object creation logic hidden.
- **Spring**: `BeanFactory`, `FactoryBean`.
- **Usage**: Creating complex beans (e.g., `LocalContainerEntityManagerFactoryBean`).

### 3. Proxy Pattern
- **Concept**: Controls access to an object.
- **Spring**: AOP, Transactions (`@Transactional`), Security.
- **Usage**: Adding behavior (logging, tx management) without modifying code.

### 4. Template Method Pattern
- **Concept**: Skeleton of an algorithm in a method.
- **Spring**: `JdbcTemplate`, `RestTemplate`, `JmsTemplate`.
- **Usage**: Standardizes resource acquisition, usage, and release.

### 5. Observer Pattern
- **Concept**: Notify dependents of state changes.
- **Spring**: Application Events.
- **Usage**: Decoupling components.

**Example**:
```java
// Event
public class UserRegisteredEvent extends ApplicationEvent {
    private String username;

    public UserRegisteredEvent(Object source, String username) {
        super(source);
        this.username = username;
    }
    // getter
}

// Publisher
@Service
public class UserService {
    @Autowired
    private ApplicationEventPublisher publisher;

    public void register(String username) {
        // save user
        publisher.publishEvent(new UserRegisteredEvent(this, username));
    }
}

// Listener
@Component
public class NotificationService {
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        System.out.println("Sending welcome email to " + event.getUsername());
    }
}
```

---

## Production Readiness

### 1. Logging (SLF4J & Logback)

**Best Practices**:
- Use SLF4J interface.
- Use appropriate levels (ERROR, WARN, INFO, DEBUG).
- Don't log sensitive info (passwords, PII).
- Use structured logging (JSON) for log aggregation (ELK stack).

**Configuration (logback-spring.xml)**:
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

### 2. Graceful Shutdown

Allows active requests to complete before shutting down.

**application.yml**:
```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 20s
```

### 3. Docker Containerization

**Dockerfile**:
```dockerfile
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

**Multi-Stage Build** (Better):
```dockerfile
FROM maven:3.8.5-openjdk-17 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /home/app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

---

## Database Optimization

### 1. Indexing
Ensure columns used in `WHERE`, `JOIN`, and `ORDER BY` clauses are indexed.

### 2. N+1 Problem
Occurs when fetching a list of entities (1 query) and then fetching related entities for each item (N queries).

**Solution**: Use `JOIN FETCH`.

```java
@Query("SELECT u FROM User u JOIN FETCH u.roles")
List<User> findAllWithRoles();
```

### 3. Batch Processing
Use `saveAll()` instead of `save()` in a loop. Configure batch size.

**application.yml**:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
          order_inserts: true
          order_updates: true
```

---

## Caching Strategies

## 🏗️ Architecture Diagram

## Caching Strategies

### 1. Spring Cache Abstraction

**Enable Caching**:
```java
@SpringBootApplication
@EnableCaching
public class Application { ... }
```

**Usage**:
```java
@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        // Expensive DB call
        return repository.findById(id).orElse(null);
    }
    
    @CacheEvict(value = "products", key = "#id")
    public void updateProduct(Long id, Product product) {
        repository.save(product);
    }
    
    @CacheEvict(value = "products", allEntries = true)
    public void clearCache() {
        // Clears entire cache
    }
}
```

### 2. Redis Configuration

**pom.xml**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**application.yml**:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

---

## JVM Tuning
Optimizing the JVM is crucial for high-performance Spring Boot applications.

### 1. Garbage Collection (GC)
- **G1GC (Garbage First GC)**: Default since Java 9. Good for large heaps with low pause time requirements.
- **ZGC (Z Garbage Collector)**: Available since Java 15. Designed for sub-millisecond pause times even with very large heaps.

**Enable ZGC**:
```bash
java -XX:+UseZGC -jar app.jar
```

### 2. Memory Settings
- **Xms**: Initial heap size.
- **Xmx**: Maximum heap size.
- **XX:MaxMetaspaceSize**: Limit for class metadata.

**Best Practice**: Set `Xms` and `Xmx` to the same value to avoid heap resizing overhead.

---

## Distributed Tracing
Distributed tracing helps track requests across multiple services.

### 1. Micrometer Tracing
Spring Boot 3 uses Micrometer Tracing (replacing Spring Cloud Sleuth).

**Dependencies**:
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

### 2. Visualization
Use **Zipkin** or **Jaeger** to visualize the traces.

---

## API Versioning
Versioning ensures backward compatibility as your API evolves.

### 1. URI Versioning (Most Common)
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller { ... }
```

### 2. Header Versioning
```java
@GetMapping(value = "/users", headers = "X-API-VERSION=1")
public List<User> getUsersV1() { ... }
```

### 3. Media Type Versioning (Content Negotiation)
```java
@GetMapping(value = "/users", produces = "application/vnd.company.app-v1+json")
public List<User> getUsersV1() { ... }
```

---

## Memory Management
Preventing memory leaks is essential for long-running applications.

### 1. Common Causes of Leaks
- **Static Collections**: Holding references in static lists/maps.
- **Unclosed Resources**: Not closing streams, database connections, or HTTP clients.
- **Inner Classes**: Non-static inner classes holding a reference to the outer class.

### 2. Detection Tools
- **VisualVM**: Basic monitoring and heap dumps.
- **Eclipse MAT (Memory Analyzer Tool)**: Deep analysis of heap dumps to find leak suspects.
- **YourKit**: Commercial profiler with advanced leak detection.
