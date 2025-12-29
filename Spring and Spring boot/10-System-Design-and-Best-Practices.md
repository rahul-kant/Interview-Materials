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

### Connection Pool Tuning Deep Dive

#### Pool Sizing Formula
The optimal connection pool size depends on your application characteristics:

**Formula**: `connections = ((core_count * 2) + effective_spindle_count)`

**Example**:
- 4-core CPU + 1 disk = (4 × 2) + 1 = **9 connections**

**Why?**
- Thread can either be CPU-bound or waiting for I/O
- 2x core count handles CPU work
- Additional connections for I/O wait time

#### HikariCP Best Practices

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10  # Don't go crazy! More isn't always better
      minimum-idle: 5         # Keep some connections ready
      idle-timeout: 600000    # 10 minutes
      connection-timeout: 30000  # 30 seconds
      max-lifetime: 1800000   # 30 minutes (less than DB timeout)
      leak-detection-threshold: 60000  # 60 seconds - detect leaks
      pool-name: HikariPool-App
```

#### Leak Detection

**Enable leak detection**:
```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 60000  # Log if connection held > 60s
```

**Common causes**:
1. Unclosed connections (not using try-with-resources)
2. Long-running transactions
3. Nested transactions not completing
4. Exception thrown before connection close

#### Statement Caching

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
```

### Database Connection Pool Exhaustion - Debugging Guide

#### Symptoms
```
org.springframework.jdbc.CannotGetJdbcConnectionException: 
Failed to obtain JDBC Connection
```

#### Step-by-Step Investigation

**1. Check Actuator Metrics**
```yaml
management:
  metrics:
    enable:
      hikaricp: true
```

Access: `/actuator/metrics/hikaricp.connections.active`

**2. Thread Dump Analysis**
```bash
# Get thread dump
jstack <pid> > thread_dump.txt

# Look for threads waiting for connections
grep -A 20 "HikariPool" thread_dump.txt
```

**3. Common Causes & Solutions**

| Cause | Solution |
|-------|----------|
| Unclosed connections | Use try-with-resources or @Transactional |
| Long transactions | Break into smaller transactions |
| Pool size too small | Increase based on formula |
| DB connections maxed out | Check DB max_connections setting |
| Connection leaks | Enable leak detection, fix code |

**4. Monitoring Query**
```java
@Component
public class ConnectionPoolMonitor {
    
    @Scheduled(fixedRate = 30000)
    public void monitorPool() {
        HikariDataSource ds = (HikariDataSource) dataSource;
        log.info("Active: {}, Idle: {}, Total: {}, Waiting: {}", 
            ds.getHikariPoolMXBean().getActiveConnections(),
            ds.getHikariPoolMXBean().getIdleConnections(),
            ds.getHikariPoolMXBean().getTotalConnections(),
            ds.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
}
```

### Query Performance Analysis

#### EXPLAIN ANALYZE Usage

**Enable SQL Logging**:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```

**Analyze Query**:
```sql
EXPLAIN ANALYZE 
SELECT u.* FROM users u 
JOIN orders o ON u.id = o.user_id 
WHERE o.status = 'COMPLETED';
```

**What to Look For**:
- **Seq Scan** (Sequential Scan) = BAD - Full table scan
- **Index Scan** = GOOD - Using index
- **Cost** - Lower is better
- **Rows** - Estimated vs actual rows

#### Index Selection Strategies

**1. Single Column Index**
```sql
CREATE INDEX idx_user_email ON users(email);
```
Use when: Frequently querying by single column

**2. Composite Index**
```sql
CREATE INDEX idx_order_user_status ON orders(user_id, status);
```
Use when: Querying by multiple columns together

**3. Covering Index**
```sql
CREATE INDEX idx_user_covering ON users(email) INCLUDE (first_name, last_name);
```
Use when: Query needs specific columns (index-only scan)

**Index Guidelines**:
- ✅ Add indexes on foreign keys
- ✅ Add indexes on WHERE clause columns
- ✅ Add indexes on JOIN columns
- ❌ Don't over-index (slows INSERT/UPDATE)
- ❌ Avoid indexes on small tables
- ❌ Avoid indexes on columns with low cardinality (few distinct values)

#### Slow Query Debugging

```java
@Component
@Aspect
public class QueryPerformanceAspect {
    
    @Around("execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object logQueryTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        Object result = joinPoint.proceed();
        
        stopWatch.stop();
        long executionTime = stopWatch.getTotalTimeMillis();
        
        if (executionTime > 1000) {
            log.warn("SLOW QUERY: {} took {}ms", 
                joinPoint.getSignature().toShortString(), 
                executionTime);
        }
        
        return result;
    }
}
```

### Memory Management in JPA

#### Entity Detachment Strategies

**Problem**: Keeping entities in Persistence Context consumes memory

**Solution 1: Clear Persistence Context**
```java
@Service
@Transactional
public class BatchProcessingService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public void processBatch(List<Long> userIds) {
        int batchSize = 50;
        
        for (int i = 0; i < userIds.size(); i++) {
            processUser(userIds.get(i));
            
            if (i % batchSize == 0) {
                entityManager.flush();   // Write to DB
                entityManager.clear();   // Clear L1 cache
            }
        }
    }
}
```

**Solution 2: Detach Specific Entity**
```java
public void processLargeDataset() {
    List<User> users = userRepository.findAll();
    
    for (User user : users) {
        // Process user
        entityManager.detach(user);  // Remove from persistence context
    }
}
```

#### DTO vs Entity in REST Responses

**❌ Anti-Pattern**: Returning Entities
```java
@GetMapping("/users")
public List<User> getUsers() {
    return userRepository.findAll();  // BAD: Loads unnecessary data
}
```

**✅ Best Practice**: Return DTOs
```java
@GetMapping("/users")
public List<UserDTO> getUsers() {
    return userRepository.findAll()
        .stream()
        .map(UserMapper::toDTO)
        .collect(Collectors.toList());
}

// Or better - use projections
@GetMapping("/users")
public List<UserSummary> getUsers() {
    return userRepository.findAllProjectedBy();
}

public interface UserSummary {
    Long getId();
    String getUsername();
    String getEmail();
}
```

#### Stateless vs Stateful Queries

**Stateless Session** (For Batch Processing):
```java
@Service
public class BatchService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public void processLargeBatch() {
        Session session = entityManager.unwrap(Session.class);
        SessionFactory sessionFactory = session.getSessionFactory();
        
        StatelessSession stateless = sessionFactory.openStatelessSession();
        try {
            Transaction tx = stateless.beginTransaction();
            
            // Process without L1 cache
            ScrollableResults results = stateless
                .createQuery("FROM User")
                .scroll(ScrollMode.FORWARD_ONLY);
            
            while (results.next()) {
                User user = (User) results.get(0);
                // Process user
            }
            
            tx.commit();
        } finally {
            stateless.close();
        }
    }
}
```

**Benefits of Stateless Session**:
- No L1 cache = Lower memory
- Faster for batch operations
- No dirty checking overhead

**Drawbacks**:
- No lazy loading
- No cascade operations
- Manual transaction management

### Performance Testing with JMH

**Add Dependency**:
```xml
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-core</artifactId>
    <version>1.36</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-generator-annprocess</artifactId>
    <version>1.36</version>
    <scope>test</scope>
</dependency>
```

**Benchmark Example**:
```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class RepositoryBenchmark {
    
    private UserRepository repository;
    
    @Setup
    public void setup() {
        // Initialize Spring context
    }
    
    @Benchmark
    public List<User> benchmarkFindAll() {
        return repository.findAll();
    }
    
    @Benchmark
    public List<User> benchmarkFindAllWithJoinFetch() {
        return repository.findAllWithOrders();
    }
}
```

### Real-World Performance Scenarios

#### Scenario 1: High-Concurrency API

**Problem**: API slows down under 1000+ req/sec

**Investigation**:
1. Check thread pool: `server.tomcat.threads.max=200`
2. Check DB connections: Should be lower than thread pool
3. Add caching for read-heavy endpoints
4. Use async processing for non-critical operations

**Solution**:
```java
@Configuration
public class PerformanceConfig {
    
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandler() {
        return protocolHandler -> {
            protocolHandler.setMaxThreads(200);
            protocolHandler.setMinSpareThreads(50);
        };
    }
}
```

#### Scenario 2: Batch Processing Optimization

**Problem**: Processing 1 million records takes hours

**Solution**:
```java
@Service
public class OptimizedBatchService {
    
    @Transactional
    public void processBatch() {
        int pageSize = 1000;
        int page = 0;
        
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<User> userPage;
        
        do {
            userPage = userRepository.findAll(pageable);
            
            userPage.getContent().forEach(user -> {
                // Process user
            });
            
            entityManager.flush();
            entityManager.clear();
            
            pageable = userPage.nextPageable();
            
        } while (userPage.hasNext());
    }
}
```

**Performance Improvements**:
- Pagination: Processes in chunks
- Clear L1 cache: Prevents memory issues
- Batch size: Balances memory vs DB roundtrips

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

### Cache Eviction Strategies

#### LRU (Least Recently Used)
Removes least recently accessed items when cache is full.

```java
@Configuration
public class CaffeineConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users", "products");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)  // LRU eviction after 1000 entries
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

#### LFU (Least Frequently Used)
Removes items accessed least often.

```java
Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(Duration.ofMinutes(10))
    .weigher((String key, Object value) -> {
        // Custom weighting based on access frequency
        return calculateWeight(value);
    });
```

#### Time-Based Eviction

**Time-to-Live (TTL)**:
```java
@Configuration
public class RedisCacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))  // Evict after 10 minutes
            .disableCachingNullValues();
        
        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
            "users", config.entryTtl(Duration.ofMinutes(30)),
            "products", config.entryTtl(Duration.ofHours(1)),
            "sessions", config.entryTtl(Duration.ofMinutes(5))
        );
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
```

**Time-to-Idle (TTI)**:
```java
Caffeine.newBuilder()
    .expireAfterAccess(10, TimeUnit.MINUTES)  // Evict if not accessed for 10 min
    .build();
```

**Write Expiration**:
```java
Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)  // Evict 5 min after write
    .build();
```

### Distributed Caching with Redis

#### Complete Redis Configuration

```java
@Configuration
@EnableCaching
public class RedisCacheConfiguration {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use Jackson for serialization
        Jackson2JsonRedisSerializer<Object> serializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        serializer.setObjectMapper(mapper);
        
        // Set serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .transactionAware()
            .build();
    }
}
```

#### Redis Cluster Configuration

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-node-1:6379
          - redis-node-2:6379
          - redis-node-3:6379
        max-redirects: 3
      password: ${REDIS_PASSWORD}
      timeout: 60000
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

### Cache Patterns Comparison

#### 1. Cache-Aside (Lazy Loading)

**Flow**: Application checks cache first, loads from DB on miss.

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository repository;
    
    @Autowired
    private RedisTemplate<String, User> redisTemplate;
    
    public User getUser(Long id) {
        String key = "user:" + id;
        
        // Check cache first
        User user = redisTemplate.opsForValue().get(key);
        
        if (user == null) {
            // Cache miss - load from DB
            user = repository.findById(id).orElse(null);
            
            if (user != null) {
                // Store in cache
                redisTemplate.opsForValue().set(
                    key, user, Duration.ofMinutes(10)
                );
            }
        }
        
        return user;
    }
}
```

**Pros**: Only caches what's needed
**Cons**: Cache miss penalty, potential cache stampede

#### 2. Read-Through

**Flow**: Cache automatically loads from DB on miss.

```java
@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        return repository.findById(id).orElse(null);
    }
}
```

**Pros**: Transparent to application
**Cons**: Initial latency on cache miss

#### 3. Write-Through

**Flow**: Writes go to cache and DB synchronously.

```java
@Service
public class ProductService {
    
    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return repository.save(product);
    }
}
```

**Pros**: Cache always consistent
**Cons**: Write latency (dual write)

#### 4. Write-Behind (Write-Back)

**Flow**: Writes to cache immediately, DB asynchronously.

```java
@Service
public class AsyncWriteService {
    
    @Async
    @CacheEvict(value = "products", key = "#product.id")
    public void updateProductAsync(Product product) {
        repository.save(product);
    }
    
    public Product updateProduct(Product product) {
        // Update cache immediately
        redisTemplate.opsForValue().set(
            "product:" + product.getId(), 
            product
        );
        
        // Async DB write
        updateProductAsync(product);
        
        return product;
    }
}
```

**Pros**: Fast writes
**Cons**: Risk of data loss, complex recovery

#### Pattern Comparison Matrix

| Pattern | Read Latency | Write Latency | Consistency | Complexity |
|---------|--------------|---------------|-------------|------------|
| Cache-Aside | Medium (miss penalty) | Low | Eventual | Low |
| Read-Through | Medium | Low | Eventual | Medium |
| Write-Through | Low | High | Strong | Medium |
| Write-Behind | Low | Low | Eventual | High |

### Cache Stampede Prevention

**Problem**: Many requests simultaneously try to load same expired cache entry.

#### Solution 1: Locking

```java
@Service
public class CacheStampedeService {
    
    private final LoadingCache<String, User> cache;
    
    public CacheStampedeService() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .refreshAfterWrite(8, TimeUnit.MINUTES)  // Refresh before expiry
            .build(key -> loadUserFromDB(key));
    }
    
    public User getUser(String userId) {
        return cache.get(userId);
    }
    
    private User loadUserFromDB(String userId) {
        // Only one thread loads, others wait
        return repository.findById(userId).orElse(null);
    }
}
```

#### Solution 2: Probabilistic Early Expiration

```java
@Service
public class ProbabilisticCacheService {
    
    public User getUser(Long id) {
        String key = "user:" + id;
        CachedUser cached = redisTemplate.opsForValue().get(key);
        
        if (cached != null) {
            long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            double beta = 1.0;
            double xfetch = Math.log(Math.random()) * beta;
            
            // Probabilistically refresh before expiration
            if (ttl < -xfetch) {
                refreshCache(id);
            }
            
            return cached.getUser();
        }
        
        return loadAndCache(id);
    }
}
```

#### Solution 3: Lock with Timeout

```java
@Service
public class DistributedLockCacheService {
    
    @Autowired
    private RedissonClient redisson;
    
    public User getUser(Long id) {
        String key = "user:" + id;
        User user = redisTemplate.opsForValue().get(key);
        
        if (user == null) {
            RLock lock = redisson.getLock("lock:user:" + id);
            
            try {
                // Try to acquire lock (wait 5s, auto-release after 10s)
                if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                    try {
                        // Double-check cache
                        user = redisTemplate.opsForValue().get(key);
                        if (user == null) {
                            user = repository.findById(id).orElse(null);
                            if (user != null) {
                                redisTemplate.opsForValue().set(
                                    key, user, Duration.ofMinutes(10)
                                );
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // Couldn't get lock, load from DB directly
                    user = repository.findById(id).orElse(null);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return user;
    }
}
```

### Cache Warming Strategies

#### 1. Startup Cache Loading

```java
@Component
public class CacheWarmer implements ApplicationListener<ContextRefreshedEvent> {
    
    @Autowired
    private UserRepository repository;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Warming cache...");
        
        Cache userCache = cacheManager.getCache("users");
        
        // Load frequently accessed users
        List<User> popularUsers = repository.findTop100ByOrderByLastAccessedDesc();
        
        popularUsers.forEach(user -> {
            userCache.put(user.getId(), user);
        });
        
        log.info("Cache warmed with {} users", popularUsers.size());
    }
}
```

#### 2. Background Refresh

```java
@Service
public class CacheRefreshService {
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void refreshPopularItems() {
        List<Long> popularProductIds = getPopularProductIds();
        
        popularProductIds.forEach(id -> {
            Product product = repository.findById(id).orElse(null);
            if (product != null) {
                redisTemplate.opsForValue().set(
                    "product:" + id,
                    product,
                    Duration.ofMinutes(10)
                );
            }
        });
    }
    
    private List<Long> getPopularProductIds() {
        // Get from analytics or metrics
        return analyticsService.getTopViewedProducts(100);
    }
}
```

#### 3. Predictive Caching

```java
@Service
public class PredictiveCacheService {
    
    @EventListener
    public void onUserLogin(UserLoginEvent event) {
        // Predictively load user's frequent data
        CompletableFuture.runAsync(() -> {
            Long userId = event.getUserId();
            
            // Load user's recent orders
            List<Order> orders = orderRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(userId);
            orders.forEach(order -> {
                cacheManager.getCache("orders")
                    .put(order.getId(), order);
            });
            
            // Load user's wishlist
            List<Product> wishlist = wishlistRepository
                .findByUserId(userId);
            wishlist.forEach(product -> {
                cacheManager.getCache("products")
                    .put(product.getId(), product);
            });
        });
    }
}
```

### Multi-Level Caching

```java
@Configuration
public class MultiLevelCacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisFactory) {
        // L1: Caffeine (local, ultra-fast)
        CaffeineCacheManager l1 = new CaffeineCacheManager();
        l1.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .recordStats());
        
        // L2: Redis (distributed, shared)
        RedisCacheConfiguration redisConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10));
        
        RedisCacheManager l2 = RedisCacheManager
            .builder(redisFactory)
            .cacheDefaults(redisConfig)
            .build();
        
        // Composite cache manager
        CompositeCacheManager composite = new CompositeCacheManager(l1, l2);
        composite.setFallbackToNoOpCache(false);
        
        return composite;
    }
}
```

**Benefits**:
- L1 (Caffeine): ~100 nanoseconds latency
- L2 (Redis): ~1 millisecond latency
- Fallback hierarchy

### Cache Monitoring

```java
@Component
public class CacheMetrics {
    
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    
    @Scheduled(fixedRate = 60000)
    public void recordCacheStats() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            
            if (cache instanceof CaffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> 
                    nativeCache = (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
                        ((CaffeineCache) cache).getNativeCache();
                
                CacheStats stats = nativeCache.stats();
                
                meterRegistry.gauge("cache.hits", 
                    Tags.of("cache", cacheName), stats.hitCount());
                meterRegistry.gauge("cache.misses", 
                    Tags.of("cache", cacheName), stats.missCount());
                meterRegistry.gauge("cache.hit.rate", 
                    Tags.of("cache", cacheName), stats.hitRate());
                meterRegistry.gauge("cache.evictions", 
                    Tags.of("cache", cacheName), stats.evictionCount());
            }
        });
    }
}
```

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
