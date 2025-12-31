# Basic Authentication - Complete Guide

> **Level:** Beginner to Advanced  
> **Time to Master:** 2-3 weeks  
> **Interview Focus:** Common in screening rounds

---

# 1. Basic Authentication

## 1️⃣ ELI5 (Explain Like I’m 5)
Imagine you want to enter a secret club. Every time you walk up to the door, the bouncer asks, "Who are you?" and "What's the password?". You have to say "My name is Alice" and "My password is 12345" **every single time** you want to go in, even if you just went out for a second. The bouncer has no memory used for this; he just checks your ID and password against his list right then and there.

## 2️⃣ REAL-LIFE ANALOGY
**The Passport Check:**
- When you cross an international border, you show your passport.
- The officer checks perfectly: **Does this specific passport match the person standing here?**
- If you cross the border again 5 minutes later, you **must show the passport again**.
- There is no "I just showed you!"—Basic Auth is stateless and requires credentials on every request.

## 3️⃣ WHY THIS EXISTS
**Ideally:** We needed a way for computers to talk to each other over the web where the server didn't need to "remember" anything about previous conversations (stateless).
**Before this:** Proprietary, complex login handshakes existed.
**The Problem Solved:** It provides the absolute simplest mechanism to say "This is me" over HTTP.
**What breaks if we don't use it?** Simple scripts, automated jobs, and legacy systems often rely on this because it requires zero complex setup (no token exchanges, no redirects).

## 4️⃣ CORE CONCEPTS
- **Stateless:** Validated on *every* HTTP request.
- **Header-Based:** Credentials sent in the `Authorization` header.
- **Base64 Encoded:** Credentials (`username:password`) are joined by a colon and encoded. **NOT ENCRYPTED!**
- **Actors:**
    - **Client:** The browser or script sending the request.
    - **Server:** Validates credentials against a database or memory.

## 5️⃣ VISUAL DIAGRAMS

**Request Flow:**

```mermaid
sequenceDiagram
    participant Client
    participant Server

    Client->>Server: GET /api/resource (No Header)
    Server-->>Client: 401 Unauthorized (WWW-Authenticate: Basic)

    Note right of Client: User enters user/pass<br/>Encoded as Base64(user:pass)

    Client->>Server: GET /api/resource<br/>Authorization: Basic dXNlcjpwYXNzd29yZA==
    Server->>Server: Decode Base64<br/>Check Database
    Server-->>Client: 200 OK (Resource Data)
```

## 6️⃣ SIMPLE EXAMPLE (BEGINNER)

**Raw HTTP Request:**
```http
GET /protected-resource HTTP/1.1
Host: api.example.com
Authorization: Basic YWxpY2U6c2VjcmV0MQ==
```
*(Note: `YWxpY2U6c2VjcmV0MQ==` is Base64 for `alice:secret1`)*

**Java (Spring Boot) Minimal Example:**

```java
// Spring Security Configuration for Basic Auth
@Configuration
@EnableWebSecurity
public class BasicAuthSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults()); // <--- This enables Basic Auth
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
            .username("user")
            .password("password")
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
```

## 7️⃣ DEEP DIVE (ADVANCED)

### Token Lifecycle (or lack thereof)
There is no "token". The lifecycle of a credential verification is the duration of a single HTTP request handler execution.

### Cryptography (The Base64 Trap)
- **Base64 is NOT Encryption.** It is encoding. `alice:secret1` -> `YWxpY2U6c2VjcmV0MQ==`.
- Anyone who intercepts the header can decode it instantly.
- **Requirement:** MUST be used over HTTPS (TLS). Without HTTPS, you are broadcasting your password to the world in plain text.

### Performance & Scalability
- **Pros:** Extremely fast for the client (no handshake).
- **Cons:** **Database Load.** Since every request requires validation, the server might hit the database to check the password hash on *every single API call*.
- **Mitigation:** High-performance caching layers (Redis) for credentials, but this introduces cache invalidation risks.

## 8️⃣ SPRING & JAVA IMPLEMENTATION

### 8.1 Complete Project Setup

**Dependencies (Maven - pom.xml):**
```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Redis for Caching (Optional but Recommended) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Lombok (Optional - Reduces Boilerplate) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**Application Configuration (application.yml):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/securitydb
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  # Redis Configuration for Caching
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 60000
  
  # Cache Configuration
  cache:
    type: redis
    redis:
      time-to-live: 600000 # 10 minutes

server:
  port: 8080
  
# Custom Security Properties
app:
  security:
    basic-auth:
      realm: "Secure API"
      cache-enabled: true
```

### 8.2 Database-Backed Authentication

**User Entity:**
```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password; // BCrypt hashed
    
    @Column(nullable = false)
    private boolean enabled = true;
    
    @Column(nullable = false)
    private boolean accountNonExpired = true;
    
    @Column(nullable = false)
    private boolean accountNonLocked = true;
    
    @Column(nullable = false)
    private boolean credentialsNonExpired = true;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
}
```

**User Repository:**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

**Custom UserDetailsService Implementation:**
```java
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    @Cacheable(value = "users", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with username: " + username));
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .disabled(!user.isEnabled())
            .accountExpired(!user.isAccountNonExpired())
            .accountLocked(!user.isAccountNonLocked())
            .credentialsExpired(!user.isCredentialsNonExpired())
            .authorities(user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList()))
            .build();
    }
}
```

### 8.3 Security Configuration

**Main Security Configuration:**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // For @PreAuthorize, @PostAuthorize
@Slf4j
public class BasicAuthSecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    
    @Value("${app.security.basic-auth.realm:Secure API}")
    private String realm;
    
    public BasicAuthSecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF - Disabled for stateless Basic Auth APIs
            .csrf(csrf -> csrf.disable())
            
            // Authorization Rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**", "/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            
            // Basic Authentication
            .httpBasic(basic -> basic
                .realmName(realm)
                .authenticationEntryPoint(customAuthenticationEntryPoint())
            )
            
            // Session Management - Stateless for API
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Security Headers
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .xssProtection(Customizer.withDefaults())
                .cacheControl(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000) // 1 year
                )
                .frameOptions(frame -> frame.deny())
            );
        
        return http.build();
    }
    
    /**
     * Custom Authentication Entry Point
     * Prevents browser's default Basic Auth popup dialog
     */
    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("Authentication failed: {} for URI: {}", 
                authException.getMessage(), request.getRequestURI());
            
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            
            // Only send WWW-Authenticate for non-AJAX requests
            String requestedWith = request.getHeader("X-Requested-With");
            if (!"XMLHttpRequest".equals(requestedWith)) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
            }
            
            // Return JSON error response
            String jsonResponse = String.format(
                "{\"error\": \"Unauthorized\", \"message\": \"%s\", \"path\": \"%s\"}",
                authException.getMessage(),
                request.getRequestURI()
            );
            response.getWriter().write(jsonResponse);
        };
    }
    
    /**
     * Password Encoder - BCrypt with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    /**
     * Authentication Manager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
```

### 8.4 Custom Authentication Provider (Advanced)

**For custom authentication logic (e.g., checking against external systems):**

```java
@Component
@Slf4j
public class CustomAuthenticationProvider implements AuthenticationProvider {
    
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    
    public CustomAuthenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }
    
    @Override
    public Authentication authenticate(Authentication authentication) 
            throws AuthenticationException {
        
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        // Check if user is blocked due to too many failed attempts
        if (loginAttemptService.isBlocked(username)) {
            log.warn("User {} is blocked due to too many failed login attempts", username);
            throw new LockedException("Account is locked due to too many failed attempts");
        }
        
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Verify password
            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                loginAttemptService.loginFailed(username);
                throw new BadCredentialsException("Invalid username or password");
            }
            
            // Additional custom checks
            if (!userDetails.isAccountNonLocked()) {
                throw new LockedException("Account is locked");
            }
            
            if (!userDetails.isEnabled()) {
                throw new DisabledException("Account is disabled");
            }
            
            if (!userDetails.isAccountNonExpired()) {
                throw new AccountExpiredException("Account has expired");
            }
            
            if (!userDetails.isCredentialsNonExpired()) {
                throw new CredentialsExpiredException("Credentials have expired");
            }
            
            // Success - reset failed attempts
            loginAttemptService.loginSucceeded(username);
            
            log.info("User {} authenticated successfully", username);
            
            return new UsernamePasswordAuthenticationToken(
                userDetails, password, userDetails.getAuthorities());
                
        } catch (UsernameNotFoundException e) {
            loginAttemptService.loginFailed(username);
            throw new BadCredentialsException("Invalid username or password");
        }
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

**Login Attempt Service (Rate Limiting):**
```java
@Service
@Slf4j
public class LoginAttemptService {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION = 15 * 60 * 1000; // 15 minutes
    
    private final LoadingCache<String, Integer> attemptsCache;
    private final LoadingCache<String, Long> lockCache;
    
    public LoginAttemptService() {
        attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, Integer>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });
            
        lockCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, Long>() {
                @Override
                public Long load(String key) {
                    return 0L;
                }
            });
    }
    
    public void loginSucceeded(String username) {
        attemptsCache.invalidate(username);
        lockCache.invalidate(username);
    }
    
    public void loginFailed(String username) {
        int attempts = attemptsCache.getUnchecked(username);
        attempts++;
        attemptsCache.put(username, attempts);
        
        if (attempts >= MAX_ATTEMPTS) {
            lockCache.put(username, System.currentTimeMillis());
            log.warn("User {} has been locked due to {} failed attempts", username, attempts);
        }
    }
    
    public boolean isBlocked(String username) {
        Long lockTime = lockCache.getUnchecked(username);
        if (lockTime == 0) {
            return false;
        }
        
        if (System.currentTimeMillis() - lockTime > LOCK_TIME_DURATION) {
            attemptsCache.invalidate(username);
            lockCache.invalidate(username);
            return false;
        }
        
        return true;
    }
}
```

### 8.5 REST Controllers

**Public Controller:**
```java
@RestController
@RequestMapping("/api/public")
public class PublicController {
    
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of(
            "message", "Hello! This is a public endpoint.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

**Protected Controller:**
```java
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {
    
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        return ResponseEntity.ok(Map.of(
            "username", userDetails.getUsername(),
            "authorities", userDetails.getAuthorities(),
            "timestamp", LocalDateTime.now()
        ));
    }
    
    @GetMapping("/data")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> getUserData() {
        return ResponseEntity.ok(Map.of(
            "data", "Sensitive user data",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
```

**Admin Controller:**
```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {
    
    private final UserRepository userRepository;
    
    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        List<Map<String, Object>> userList = users.stream()
            .map(user -> Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "roles", user.getRoles(),
                "enabled", user.isEnabled()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(userList);
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        log.info("User with id {} deleted", id);
        return ResponseEntity.noContent().build();
    }
}
```

### 8.6 Database Initialization

**Data Loader (for testing):**
```java
@Component
@Slf4j
public class DataLoader implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public DataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Loading initial user data...");
            
            // Create admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of("ADMIN", "USER"));
            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);
            
            // Create regular user
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRoles(Set.of("USER"));
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            log.info("Initial users created: admin/admin123, user/user123");
        }
    }
}
```

### 8.7 Performance Optimization with Redis

**Cache Configuration:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

## 9️⃣ HANDS-ON LAB (NO COST)

### Lab 1: Quick Start with In-Memory Authentication

**Goal:** Get Basic Auth running in 5 minutes.

**Step 1: Create Project**
```bash
# Using Spring Initializr CLI
curl https://start.spring.io/starter.tgz \
  -d dependencies=web,security \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.2.0 \
  -d baseDir=basic-auth-demo \
  | tar -xzvf -

cd basic-auth-demo
```

**Step 2: Create a Simple Controller**
```java
// src/main/java/com/example/demo/HelloController.java
package com.example.demo;

import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api")
public class HelloController {
    
    @GetMapping("/hello")
    public String hello(Principal principal) {
        return "Hello, " + principal.getName() + "!";
    }
    
    @GetMapping("/admin")
    public String admin() {
        return "Admin access granted!";
    }
}
```

**Step 3: Run the Application**
```bash
./mvnw spring-boot:run
```

**Step 4: Test with curl**
```bash
# 1. Without credentials - Should fail with 401
curl -v http://localhost:8080/api/hello

# 2. With default credentials (check console for generated password)
curl -u user:YOUR_GENERATED_PASSWORD http://localhost:8080/api/hello

# 3. Using Base64 encoded credentials
echo -n "user:YOUR_PASSWORD" | base64
# Output: dXNlcjpZT1VSX1BBU1NXT1JE

curl -H "Authorization: Basic dXNlcjpZT1VSX1BBU1NXT1JE" \
     http://localhost:8080/api/hello
```

---

### Lab 2: Database-Backed Authentication with Docker

**Goal:** Build a production-ready setup with PostgreSQL and Redis.

**Step 1: Docker Compose Setup**

Create `docker-compose.yml`:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: security-postgres
    environment:
      POSTGRES_DB: securitydb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: security-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: security-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@admin.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres

volumes:
  postgres_data:
  redis_data:
```

**Step 2: Start Infrastructure**
```bash
docker-compose up -d

# Verify services are running
docker-compose ps

# Check logs
docker-compose logs -f postgres
```

**Step 3: Add Complete Dependencies**

Add to `pom.xml`:
```xml
<!-- Guava for Login Attempt Caching -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.1.3-jre</version>
</dependency>

<!-- Actuator for Health Checks -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Step 4: Implement All Components**

Use the complete code from Section 8 (User Entity, Repository, Services, Controllers).

**Step 5: Run and Test**
```bash
# Start the application
./mvnw spring-boot:run

# Wait for startup, then test
```

**Testing Scenarios:**

```bash
# 1. Test Public Endpoint (No Auth Required)
curl http://localhost:8080/api/public/hello

# 2. Test User Endpoint (Requires Authentication)
curl -u user:user123 http://localhost:8080/api/user/profile

# 3. Test Admin Endpoint (Requires ADMIN role)
curl -u admin:admin123 http://localhost:8080/api/admin/users

# 4. Test with wrong credentials (should fail)
curl -u user:wrongpassword http://localhost:8080/api/user/profile

# 5. Test account lockout (5 failed attempts)
for i in {1..6}; do
  echo "Attempt $i:"
  curl -u user:wrongpassword http://localhost:8080/api/user/profile
  sleep 1
done

# 6. Test JSON response format
curl -i -u user:user123 http://localhost:8080/api/user/profile

# 7. Test AJAX request (no browser popup)
curl -H "X-Requested-With: XMLHttpRequest" \
     http://localhost:8080/api/user/profile

# 8. Test with Postman-style verbose output
curl -v -u user:user123 http://localhost:8080/api/user/profile
```

---

### Lab 3: Performance Testing with Redis Caching

**Goal:** Measure the impact of caching on authentication performance.

**Step 1: Install Apache Bench**
```bash
# macOS
brew install httpd

# Verify installation
ab -V
```

**Step 2: Benchmark WITHOUT Caching**

Temporarily disable caching in `CustomUserDetailsService`:
```java
// Remove @Cacheable annotation
public UserDetails loadUserByUsername(String username) {
    // ... existing code
}
```

Run benchmark:
```bash
# 1000 requests, 10 concurrent
ab -n 1000 -c 10 -A user:user123 \
   http://localhost:8080/api/user/profile

# Note the "Requests per second" value
```

**Step 3: Benchmark WITH Caching**

Re-enable `@Cacheable` and restart:
```bash
# Same benchmark
ab -n 1000 -c 10 -A user:user123 \
   http://localhost:8080/api/user/profile

# Compare "Requests per second" - should be 2-3x faster
```

**Step 4: Monitor Redis**
```bash
# Connect to Redis CLI
docker exec -it security-redis redis-cli

# Monitor cache activity
MONITOR

# In another terminal, make requests
curl -u user:user123 http://localhost:8080/api/user/profile

# Check cached keys
KEYS *

# View cached user data
GET users::user
```

---

### Lab 4: Security Headers Verification

**Goal:** Verify all security headers are properly configured.

**Test Security Headers:**
```bash
# Check all headers
curl -I -u user:user123 http://localhost:8080/api/user/profile

# Expected headers:
# X-Content-Type-Options: nosniff
# X-XSS-Protection: 0
# Cache-Control: no-cache, no-store, max-age=0, must-revalidate
# Strict-Transport-Security: max-age=31536000 ; includeSubDomains
# X-Frame-Options: DENY
```

**Using Security Header Scanner:**
```bash
# Install securityheaders.com CLI (optional)
npm install -g observatory-cli

# Scan your application
observatory localhost:8080 --format=report
```

---

### Lab 5: Integration Testing

**Create Test Class:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BasicAuthIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void publicEndpoint_ShouldBeAccessible_WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/public/hello"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void userEndpoint_ShouldFail_WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void userEndpoint_ShouldSucceed_WithUserRole() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("user"));
    }
    
    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void adminEndpoint_ShouldFail_WithUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminEndpoint_ShouldSucceed_WithAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    void basicAuth_ShouldWork_WithValidCredentials() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                .with(httpBasic("user", "user123")))
            .andExpect(status().isOk());
    }
    
    @Test
    void basicAuth_ShouldFail_WithInvalidCredentials() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                .with(httpBasic("user", "wrongpassword")))
            .andExpect(status().isUnauthorized());
    }
}
```

**Run Tests:**
```bash
./mvnw test
```

---

### Troubleshooting Guide

**Problem 1: "Access Denied" even with correct credentials**
```bash
# Check if user exists in database
docker exec -it security-postgres psql -U postgres -d securitydb \
  -c "SELECT * FROM users;"

# Check user roles
docker exec -it security-postgres psql -U postgres -d securitydb \
  -c "SELECT * FROM user_roles;"

# Verify password encoding
# Password should start with $2a$ or $2b$ (BCrypt)
```

**Problem 2: Redis connection refused**
```bash
# Check if Redis is running
docker ps | grep redis

# Test Redis connection
docker exec -it security-redis redis-cli ping
# Should return: PONG

# Check application logs for connection errors
./mvnw spring-boot:run | grep -i redis
```

**Problem 3: Database connection errors**
```bash
# Verify PostgreSQL is running
docker exec -it security-postgres pg_isready

# Test connection manually
docker exec -it security-postgres psql -U postgres -d securitydb

# Check application.yml database URL
cat src/main/resources/application.yml | grep url
```

**Problem 4: Account locked after failed attempts**
```bash
# Clear the lock cache
docker exec -it security-redis redis-cli FLUSHALL

# Or wait 15 minutes for automatic unlock
```

**Problem 5: CORS errors in browser**

Add CORS configuration:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

---

### Cleanup

```bash
# Stop and remove containers
docker-compose down

# Remove volumes (WARNING: Deletes all data)
docker-compose down -v

# Remove project
cd ..
rm -rf basic-auth-demo
```

## 🔟 INTERVIEW QUESTIONS

**Beginner:**
Q: What is the difference between encoding and encryption in Basic Auth?
A: Encoding (Base64) is reversible format translation. Encryption requires a key to reverse. Basic Auth uses encoding, so it is insecure without HTTPS.

**Senior:**
Q: How does Basic Auth impact database scaling?
A: It requires credential validation on every request, potentially causing a "thundering herd" on the User/Auth table. Caching is required but risky.

**Architect:**
Q: You have a legacy system using Basic Auth. Steps to modernize?
A: 1. Enforce HTTPS. 2. Migrate to an API Gateway that terminates Basic Auth, validates it once, and issues a short-lived internal JWT for microservices.

## 1️⃣1️⃣ WHEN TO USE / WHEN NOT TO USE

| Scenario | Decision | Reason |
| :--- | :--- | :--- |
| **Public Internet Users** | ❌ NO | Credentials exposed on every request; hard to revoke. |
| **Internal Microservices** | ⚠️ MAYBE | Simple, but mTLS is better. |
| **Simple Scripts / CRON** | ✅ YES | Easy to implement in `curl` or Python scripts. |
| **Mobile Apps** | ❌ NO | Storing username/password on device is insecure. |

## 1️⃣2️⃣ SECURITY ATTACKS & DEFENSE (THREAT MODELING)

**STRIDE Analysis for Basic Auth:**

| Threat | Attack Vector | Impact | Mitigation |
| :--- | :--- | :--- | :--- |
| **Spoofing** | Attacker sniffs the `Authorization` header on public WiFi. | Identity theft. Attacker becomes the user forever. | **Enforce HTTPS (TLS 1.2+).** Never allow HTTP. Use HSTS. |
| **Tampering** | Man-in-the-Middle modifies the request body. | Integrity loss. Attacker changes "Pay Alice" to "Pay Bob". | **TLS (HTTPS)** guarantees integrity. |
| **Information Disclosure** | Browser caches the credentials; Proxy logs the header. | Credential leakage to sysadmins or local users. | Use `Cache-Control: no-store`. Configure logs to mask Headers. |
| **Repudiation** | User claims "I didn't send that request." | Cannot prove origin if logs are poor. | Enable detailed Access Logging with IP and User-Agent. |

## 1️⃣3️⃣ MIND MAP

*   **Basic Auth**
    *   **Mechanism**: Base64(user:pass) in Header
    *   **State**: Stateless (Every request checked)
    *   **Security**: Minimal (Depends 100% on HTTPS)
    *   **Use Cases**: Machine-to-Machine, Scripts, Internal Tools

---

### 🔟 ARCHITECT DECISION NOTES
> **Implementation**: **Basic Auth**
>
> **The Verdict**: **AVOID directly in modern production apps.**
>
> **Reasoning**: While reliable and simple, the "password on every request" model increases the attack surface significantly. If a request is intercepted **once**, the attacker has the user's permanent password (not just a temporary token).
>
> **Modern Pattern**: If you MUST use it (e.g., legacy clients), terminate it at the Edge (API Gateway/Load Balancer). The Gateway validates the Basic Auth against an IdP, swaps it for a short-lived JWT, and forwards the request to your backend services. This keeps the detailed password handling out of your microservices.

---



---

## 🎯 Next Steps

1. **Practice:** Complete all hands-on labs
2. **Test:** Answer all interview questions
3. **Build:** Create a project using Basic Auth
4. **Move Forward:** Proceed to 02-Bearer-Token-Complete.md

---

**File:** 01-Basic-Authentication-Complete.md  
**Last Updated:** December 30, 2025  
**Part of:** Complete Security Study Guide
