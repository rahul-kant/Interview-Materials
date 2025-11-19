# Spring & Spring Boot - Frequently Asked Interview Questions

## Table of Contents
1. [Core Spring Questions](#core-spring-questions)
2. [Spring Boot Questions](#spring-boot-questions)
3. [REST API Questions](#rest-api-questions)
4. [Spring Data JPA Questions](#spring-data-jpa-questions)
5. [Spring Security Questions](#spring-security-questions)
6. [Microservices Questions](#microservices-questions)
7. [Performance & Optimization Questions](#performance--optimization-questions)
8. [Scenario-Based Questions](#scenario-based-questions)

---

## Core Spring Questions

### Q1: What is Spring Framework and why is it popular?

**Answer**: Spring is a comprehensive framework for building Java enterprise applications. It's popular because:
- **Dependency Injection**: Loose coupling between components
- **AOP Support**: Cross-cutting concerns like logging, security
- **Transaction Management**: Declarative transaction support
- **Integration**: Easy integration with other frameworks
- **Testing**: Excellent support for unit and integration testing
- **Modularity**: Use only what you need

### Q2: Explain Dependency Injection and its types.

**Answer**: DI is a design pattern where objects receive their dependencies from external sources rather than creating them. Types:

1. **Constructor Injection** (Recommended):
```java
@Service
public class UserService {
    private final UserRepository repository;
    
    @Autowired // Optional in Spring 4.3+
    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}
```

2. **Setter Injection**:
```java
@Autowired
public void setRepository(UserRepository repository) {
    this.repository = repository;
}
```

3. **Field Injection** (Not recommended):
```java
@Autowired
private UserRepository repository;
```

### Q3: What is the difference between BeanFactory and ApplicationContext?

**Answer**:
| Feature | BeanFactory | ApplicationContext |
|---------|-------------|-------------------|
| Bean creation | Lazy | Eager |
| Event propagation | No | Yes |
| Internationalization | No | Yes |
| AOP support | Manual | Automatic |
| Enterprise features | Limited | Full |

**Recommendation**: Always use ApplicationContext in production.

### Q4: Explain the Bean lifecycle in Spring.

**Answer**:
1. Instantiation (Constructor called)
2. Populate properties (Dependency Injection)
3. setBeanName() - if BeanNameAware
4. setBeanFactory() - if BeanFactoryAware
5. setApplicationContext() - if ApplicationContextAware
6. postProcessBeforeInitialization() - BeanPostProcessor
7. @PostConstruct or afterPropertiesSet()
8. Custom init-method
9. postProcessAfterInitialization() - BeanPostProcessor
10. **Bean ready to use**
11. @PreDestroy or destroy()
12. Custom destroy-method

### Q5: What are the different bean scopes in Spring?

**Answer**:
- **Singleton** (default): One instance per Spring container
- **Prototype**: New instance each time requested
- **Request**: One instance per HTTP request (web apps)
- **Session**: One instance per HTTP session (web apps)
- **Application**: One instance per ServletContext
- **WebSocket**: One instance per WebSocket session

### Q6: What is @Qualifier and when do you use it?

**Answer**: Used to resolve ambiguity when multiple beans of the same type exist.

```java
@Configuration
public class Config {
    @Bean
    @Qualifier("mysql")
    public DataSource mysqlDataSource() { }
    
    @Bean
    @Qualifier("postgres")
    public DataSource postgresDataSource() { }
}

@Service
public class MyService {
    @Autowired
    @Qualifier("mysql")
    private DataSource dataSource;
}
```

### Q7: What is @Primary annotation?

**Answer**: Marks a bean as the primary candidate when multiple beans of the same type exist. Unlike @Qualifier, it doesn't require changes in injection points.

```java
@Bean
@Primary
public DataSource primaryDataSource() {
    return new HikariDataSource();
}
```

### Q8: Difference between @Component, @Service, @Repository, and @Controller?

**Answer**:
- **@Component**: Generic stereotype for any Spring-managed component
- **@Service**: Business logic layer, specialization of @Component
- **@Repository**: Data access layer, adds automatic exception translation
- **@Controller**: Web layer (Spring MVC), returns views
- **@RestController**: REST API layer, combines @Controller + @ResponseBody

### Q9: What is AOP (Aspect-Oriented Programming)?

**Answer**: AOP allows separation of cross-cutting concerns (logging, security, transactions) from business logic.

**Key Concepts**:
- **Aspect**: Module encapsulating cross-cutting concern
- **Join Point**: Point in program execution (method call)
- **Advice**: Action taken at join point (before, after, around)
- **Pointcut**: Expression matching join points
- **Weaving**: Linking aspects with other objects

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Executing: " + joinPoint.getSignature().getName());
    }
    
    @Around("@annotation(com.example.Timed)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long time = System.currentTimeMillis() - start;
        log.info("Execution time: " + time + "ms");
        return result;
    }
}
```

### Q10: How does circular dependency occur and how to resolve it?

**Answer**: Occurs when Bean A depends on Bean B, and Bean B depends on Bean A.

**Resolution**:
1. **@Lazy annotation**:
```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(@Lazy ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}
```

2. **Setter injection** instead of constructor injection
3. **Redesign** to eliminate circular dependency (preferred)

---

## Spring Boot Questions

### Q11: What is Spring Boot and its advantages?

**Answer**: Spring Boot is an opinionated framework that simplifies Spring application development.

**Advantages**:
- Auto-configuration based on classpath
- Standalone applications with embedded servers
- Production-ready features (Actuator)
- Minimal XML configuration
- Opinionated defaults
- Easy dependency management with starters

### Q12: Explain Spring Boot auto-configuration.

**Answer**: Auto-configuration automatically configures Spring application based on classpath dependencies.

**How it works**:
1. @EnableAutoConfiguration scans classpath
2. Loads auto-configuration classes from spring.factories
3. Uses @Conditional annotations to determine what to configure
4. Creates beans if not already defined

**Example**:
```java
@ConditionalOnClass(DataSource.class)
@ConditionalOnMissingBean(DataSource.class)
public class DataSourceAutoConfiguration {
    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}
```

### Q13: What is the difference between @SpringBootApplication and @EnableAutoConfiguration?

**Answer**:
```java
@SpringBootApplication = 
    @Configuration + 
    @EnableAutoConfiguration + 
    @ComponentScan
```

@SpringBootApplication is a convenience annotation combining three annotations.

### Q14: How to disable specific auto-configuration?

**Answer**:
```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
public class MyApplication { }
```

Or in application.properties:
```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

### Q15: What is Spring Boot Actuator?

**Answer**: Provides production-ready features for monitoring and managing applications.

**Key Endpoints**:
- /actuator/health - Application health
- /actuator/metrics - Application metrics
- /actuator/info - Application information
- /actuator/env - Environment properties
- /actuator/loggers - Logger configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always
```

### Q16: Difference between application.properties and application.yml?

**Answer**:
- Both serve same purpose
- YAML is more readable for hierarchical data
- YAML supports multi-document files (multiple profiles in one file)
- Properties is simpler for flat configuration

**application.yml**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db
    username: root
```

**application.properties**:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/db
spring.datasource.username=root
```

### Q17: How to implement custom health indicator?

**Answer**:
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        boolean serviceUp = checkService();
        
        if (serviceUp) {
            return Health.up()
                .withDetail("status", "Service is running")
                .build();
        }
        
        return Health.down()
            .withDetail("error", "Service unavailable")
            .build();
    }
}
```

### Q18: What are Spring Boot Starters?

**Answer**: Dependency descriptors that include all required dependencies for a feature.

**Common Starters**:
- spring-boot-starter-web: Web applications
- spring-boot-starter-data-jpa: JPA with Hibernate
- spring-boot-starter-security: Spring Security
- spring-boot-starter-test: Testing frameworks
- spring-boot-starter-actuator: Monitoring

### Q18a: What are Spring Profiles and how do you use them?

**Answer**: Profiles provide a way to segregate application configuration and make it available only in certain environments (dev, test, prod).

**Use Cases**:
- Different database configurations per environment
- Enable/disable features based on environment
- Different logging levels
- Mock services in development

**Defining Profile-Specific Configuration**:
```
application.yml              # Default
application-dev.yml          # Development profile
application-test.yml         # Test profile
application-prod.yml         # Production profile
```

**Activating Profiles** (4 methods):

1. **In application.yml**:
```yaml
spring:
  profiles:
    active: dev
```

2. **Command line argument**:
```bash
java -jar myapp.jar --spring.profiles.active=prod
```

3. **Environment variable**:
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar myapp.jar
```

4. **Programmatically**:
```java
SpringApplication app = new SpringApplication(MyApplication.class);
app.setAdditionalProfiles("dev");
app.run(args);
```

**Profile-Specific Beans**:
```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
    
    @Bean
    @Profile("prod")
    public DataSource prodDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://prod-server:3306/db");
        return ds;
    }
    
    @Bean
    @Profile({"dev", "test"})  // Multiple profiles
    public MockService mockService() {
        return new MockService();
    }
    
    @Bean
    @Profile("!prod")  // All profiles except prod
    public DebugService debugService() {
        return new DebugService();
    }
}
```

**Multi-Document YAML**:
```yaml
# Default configuration
spring:
  application:
    name: myapp

---
# Development profile
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:testdb

---
# Production profile
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:mysql://prod-server:3306/mydb
```

**Best Practices**:
- Use meaningful profile names (dev, staging, prod)
- Never commit sensitive credentials to version control
- Use environment variables for sensitive data in prod
- Document which profiles are available
- Test with the same profile you'll use in production

---

## REST API Questions

### Q19: Difference between @Controller and @RestController?

**Answer**:
- **@Controller**: Returns views (HTML), used in traditional MVC
- **@RestController**: Returns data (JSON/XML), combines @Controller + @ResponseBody

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

### Q20: What is the difference between PUT and PATCH?

**Answer**:
- **PUT**: Complete replacement of resource (all fields required)
- **PATCH**: Partial update (only changed fields)
- PUT is idempotent, PATCH typically isn't

```java
@PutMapping("/{id}")
public UserDTO updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
    // All fields must be provided
}

@PatchMapping("/{id}")
public UserDTO partialUpdate(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
    // Only changed fields provided
}
```

### Q21: How to handle exceptions globally in Spring Boot?

**Answer**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        
        return new ResponseEntity<>(
            new ValidationErrorResponse(errors), 
            HttpStatus.BAD_REQUEST
        );
    }
}
```

### Q22: How to implement pagination in REST API?

**Answer**:
```java
@GetMapping("/users")
public Page<UserDTO> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id,asc") String[] sort) {
    
    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
    return userService.findAll(pageable);
}
```

### Q23: How to version REST APIs?

**Answer**:
1. **URI Versioning** (Most common):
```java
@GetMapping("/api/v1/users")
@GetMapping("/api/v2/users")
```

2. **Header Versioning**:
```java
@GetMapping(value = "/api/users", headers = "API-Version=1")
```

3. **Media Type Versioning**:
```java
@GetMapping(value = "/api/users", produces = "application/vnd.company.app-v1+json")
```

### Q24: What is Content Negotiation?

**Answer**: Process where client and server agree on data format.
- Client uses **Accept** header
- Server uses **Content-Type** header

```java
@GetMapping(value = "/users/{id}", 
            produces = {MediaType.APPLICATION_JSON_VALUE, 
                       MediaType.APPLICATION_XML_VALUE})
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}
```

---

## Spring Data JPA Questions

### Q25: What is the difference between JPA, Hibernate, and Spring Data JPA?

**Answer**:
- **JPA**: Specification (javax.persistence.*)
- **Hibernate**: Implementation of JPA
- **Spring Data JPA**: Abstraction layer over JPA

```
Spring Data JPA → JPA API → Hibernate → JDBC → Database
```

### Q26: Explain the N+1 problem and its solutions.

**Answer**: N+1 problem occurs when fetching N entities triggers N additional queries for related entities.

**Problem**:
```java
List<User> users = userRepository.findAll(); // 1 query
for (User user : users) {
    user.getOrders().size(); // N queries
}
```

**Solutions**:
1. **JOIN FETCH**:
```java
@Query("SELECT u FROM User u JOIN FETCH u.orders")
List<User> findAllWithOrders();
```

2. **Entity Graph**:
```java
@EntityGraph(attributePaths = {"orders"})
List<User> findAll();
```

3. **Batch Fetching**:
```java
@BatchSize(size = 10)
private List<Order> orders;
```

### Q27: Difference between @JoinColumn and mappedBy?

**Answer**:
- **@JoinColumn**: Owning side of relationship, creates foreign key
- **mappedBy**: Non-owning side, refers to property on owning side

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user") // Non-owning side
    private List<Order> orders;
}

@Entity
public class Order {
    @ManyToOne
    @JoinColumn(name = "user_id") // Owning side
    private User user;
}
```

### Q28: When to use CascadeType.ALL?

**Answer**: Use when parent and child have tightly coupled lifecycle.

**Caution**:
- Avoid for many-to-many relationships
- CascadeType.REMOVE can delete unintended data
- Prefer specific cascade types

```java
@OneToMany(mappedBy = "user", cascade = {
    CascadeType.PERSIST,
    CascadeType.MERGE
})
private List<Order> orders;
```

### Q29: Difference between EAGER and LAZY fetching?

**Answer**:
- **EAGER**: Loads related entities immediately
- **LAZY**: Loads on-demand when accessed

```java
@OneToMany(fetch = FetchType.LAZY) // Default for collections
private List<Order> orders;

@ManyToOne(fetch = FetchType.EAGER) // Default for single associations
private User user;
```

**Best Practice**: Use LAZY by default, fetch eagerly only when needed.

### Q30: What is the difference between save() and saveAndFlush()?

**Answer**:
- **save()**: Persists to persistence context, writes to DB at transaction commit
- **saveAndFlush()**: Immediately writes to DB

```java
User user = new User();
userRepository.save(user); // Flushed at transaction end
userRepository.saveAndFlush(user); // Flushed immediately
```

---

## Spring Security Questions

### Q31: Difference between Authentication and Authorization?

**Answer**:
- **Authentication**: Verifying identity (Who are you?)
- **Authorization**: Checking permissions (What can you do?)

### Q32: How does Spring Security work internally?

**Answer**: Uses filter chain:
1. Request intercepted by SecurityFilterChain
2. UsernamePasswordAuthenticationFilter attempts authentication
3. AuthenticationManager delegates to AuthenticationProvider
4. UserDetailsService loads user details
5. PasswordEncoder verifies password
6. SecurityContext populated with Authentication
7. FilterSecurityInterceptor checks authorization
8. Request proceeds if authorized, else 403

### Q33: Difference between @Secured and @PreAuthorize?

**Answer**:
- **@Secured**: Simple role checking, no SpEL
```java
@Secured("ROLE_ADMIN")
public void deleteUser(Long id) { }
```

- **@PreAuthorize**: Supports SpEL expressions
```java
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public void updateUser(Long userId) { }
```

### Q34: How to implement JWT authentication?

**Answer**:
1. User logs in with credentials
2. Server validates and generates JWT
3. Client includes JWT in Authorization header
4. Server validates JWT on each request
5. Extract user info from JWT and set SecurityContext

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String jwt = getJwtFromRequest(request);
        
        if (jwt != null && tokenProvider.validateToken(jwt)) {
            String username = tokenProvider.getUsernameFromToken(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
            
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### Q35: What is CSRF and how to prevent it?

**Answer**: Cross-Site Request Forgery is when attacker tricks user into unwanted action.

**Prevention**:
- Use CSRF tokens (Spring Security default)
- Check Origin/Referer headers
- SameSite cookie attribute
- For stateless APIs, disable CSRF and use JWT

```java
http.csrf(csrf -> csrf.disable()); // For stateless APIs
```

---

## Microservices Questions

### Q36: What are the benefits of microservices?

**Answer**:
- Independent deployment
- Technology diversity
- Scalability
- Fault isolation
- Team autonomy
- Easier maintenance

### Q37: How do microservices communicate?

**Answer**:
1. **Synchronous**: REST, gRPC
2. **Asynchronous**: Message queues (RabbitMQ, Kafka)
3. **Event-driven**: Event bus

### Q38: What is Service Discovery?

**Answer**: Mechanism to automatically detect services in network.

**Example with Eureka**:
```java
@EnableEurekaClient
@SpringBootApplication
public class ServiceApplication { }
```

### Q39: What is Circuit Breaker pattern?

**Answer**: Prevents cascading failures by monitoring service calls and "opening circuit" when failures exceed threshold.

```java
@CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
public User getUser(Long id) {
    return restTemplate.getForObject("http://user-service/users/" + id, User.class);
}

public User getUserFallback(Long id, Exception e) {
    return new User(); // Default user
}
```

### Q40: What is API Gateway?

**Answer**: Single entry point for all clients, handles:
- Routing
- Authentication
- Rate limiting
- Load balancing
- Request/Response transformation

---

## Performance & Optimization Questions

### Q41: How to optimize Spring Boot application startup time?

**Answer**:
1. Use lazy initialization: `spring.main.lazy-initialization=true`
2. Exclude unused auto-configurations
3. Use spring-context-indexer
4. Reduce classpath scanning scope
5. Use GraalVM native image
6. Profile and remove unnecessary dependencies

### Q42: How to implement caching in Spring Boot?

**Answer**:
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("users", "products");
    }
}

@Service
public class UserService {
    @Cacheable("users")
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public void updateUser(User user) {
        userRepository.save(user);
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void clearCache() { }
}
```

### Q43: How to implement connection pooling?

**Answer**: Spring Boot uses HikariCP by default.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Q44: How to handle large file uploads?

**Answer**:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

```java
@PostMapping("/upload")
public ResponseEntity<String> uploadFile(
        @RequestParam("file") MultipartFile file) {
    
    if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("File is empty");
    }
    
    // Process file in chunks
    try (InputStream is = file.getInputStream()) {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            // Process chunk
        }
    }
    
    return ResponseEntity.ok("File uploaded successfully");
}
```

---

## Scenario-Based Questions

### Q45: How would you design a system to handle 1 million requests per day?

**Answer**:
1. **Load Balancing**: Distribute traffic across multiple instances
2. **Caching**: Redis/Memcached for frequently accessed data
3. **Database Optimization**: Read replicas, connection pooling
4. **Async Processing**: Message queues for non-critical operations
5. **CDN**: Static content delivery
6. **Monitoring**: Track metrics, set up alerts
7. **Auto-scaling**: Scale based on load

### Q46: How to handle database connection pool exhaustion?

**Answer**:
1. **Immediate**: Increase pool size
```yaml
spring.datasource.hikari.maximum-pool-size=20
```

2. **Long-term**:
- Find and fix connection leaks
- Use @Transactional properly
- Implement connection timeout
- Add connection pool monitoring
- Consider read replicas

### Q47: Application is slow. How do you debug?

**Answer**:
1. **Enable Actuator metrics**: Check /actuator/metrics
2. **Database queries**: Enable SQL logging, find slow queries
3. **Profiling**: Use JProfiler, YourKit
4. **Thread dumps**: Check for deadlocks
5. **Memory analysis**: Check for memory leaks
6. **Network**: Check external service calls
7. **Logs**: Analyze application logs

### Q48: How to implement distributed tracing?

**Answer**: Use Spring Cloud Sleuth + Zipkin

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

```yaml
spring:
  zipkin:
    base-url: http://localhost:9411
  sleuth:
    sampler:
      probability: 1.0
```

### Q49: How to implement graceful shutdown?

**Answer**:
```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

```java
@Component
public class GracefulShutdownHandler implements ApplicationListener<ContextClosedEvent> {
    
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("Graceful shutdown initiated");
        // Clean up resources
        // Complete pending requests
        // Close connections
    }
}
```

### Q50: How to implement rate limiting?

**Answer**:
```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        String clientId = getClientId(request);
        RateLimiter limiter = limiters.computeIfAbsent(
            clientId, 
            k -> RateLimiter.create(100.0) // 100 requests/second
        );
        
        if (!limiter.tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        
        return true;
    }
}
```

---

## FAANG & Top Tier Company Questions

These questions are frequently asked in interviews at Google, Amazon, Meta, Netflix, and other top tech companies. They focus on depth, system design, and internals.

### Q51: How does Spring handle concurrent requests in a Singleton bean? (Google)
**Answer**:
Spring beans are Singletons by default, meaning one instance is shared across all requests.
- **Statelessness**: Singleton beans should be stateless (no instance variables that change per request).
- **Thread Safety**: If state is needed, use `ThreadLocal` or `RequestScope`.
- **Concurrency**: The web container (Tomcat) assigns a separate thread for each request, but they all access the same bean instance.

### Q52: Design a Rate Limiter using Spring Boot. (Amazon)
**Answer**:
- **Algorithm**: Token Bucket or Leaky Bucket.
- **Implementation**:
  - Use Redis (Lua scripts) for distributed rate limiting.
  - Use `Bucket4j` library.
  - Create a custom annotation `@RateLimit` and an Aspect to intercept requests.
  - Return `429 Too Many Requests` if limit exceeded.

### Q53: How would you handle distributed transactions in Microservices? (Uber/Netflix)
**Answer**:
Distributed transactions (2PC) are hard to scale. Use **SAGA Pattern**:
- **Choreography**: Services emit events; other services listen and react.
- **Orchestration**: A central coordinator tells services what to do.
- **Compensation**: If a step fails, execute compensating transactions to undo previous steps.

### Q54: Explain the internal working of @Transactional. (Meta)
**Answer**:
- Spring uses **AOP Proxies** (CGLIB or JDK Dynamic Proxy).
- When a method is called, the proxy intercepts it.
- It asks the `TransactionManager` to start a transaction.
- It executes the actual method.
- If successful, it commits; if an unchecked exception occurs, it rollbacks.
- **Gotcha**: Calling a `@Transactional` method from within the same class bypasses the proxy, so no transaction is started (Self-invocation problem).

### Q55: How do you optimize a Spring Boot application for high throughput? (High Frequency Trading firms)
**Answer**:
- **WebFlux**: Use non-blocking I/O (Netty) instead of blocking Tomcat.
- **Connection Pooling**: Tune HikariCP (pool size, timeouts).
- **Caching**: Use Redis/Caffeine to reduce DB hits.
- **Serialization**: Use Protobuf instead of JSON for internal communication.
- **GC Tuning**: Use ZGC or Shenandoah for low latency.

---

## Quick Tips for Interviews

### 1. Always Mention Trade-offs
When answering, discuss pros and cons of different approaches.

### 2. Use Real-World Examples
Reference projects you've worked on.

### 3. Follow Up Questions
Be prepared for deeper dives into any topic.

### 4. Code on Whiteboard
Practice writing code without IDE assistance.

### 5. System Design
Think about scalability, reliability, and performance.

### 6. Ask Clarifying Questions
Don't make assumptions; ask for requirements.

### 7. Latest Versions
Stay updated with Spring Boot 3.x and Spring 6.

### 8. Testing
Mention how you would test your solutions.

### 9. Security
Always consider security implications.

### 10. Best Practices
Demonstrate knowledge of industry best practices.

---

## Additional Resources

- Spring Framework Documentation
- Baeldung Tutorials
- Spring Boot Reference Guide
- Spring Security Documentation
- LeetCode System Design
- Design Patterns (Gang of Four)
- Effective Java by Joshua Bloch
- Clean Code by Robert Martin
