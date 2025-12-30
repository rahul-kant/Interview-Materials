# 16. Security: Authentication, Authorization & Identity Management [Deep Dive]

> **Target Audience**: Beginners to Principal Architects
> **Goal**: From "Explain Like I'm 5" to rigorous production architecture.

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

# 2. Bearer Token Authentication

## 1️⃣ ELI5 (Explain Like I’m 5)
Imagine you go to an arcade. You give the cashier $20 (Authentication), and they give you a bag of gold tokens. Now, to play a game, you just drop a token in the machine. The game machine doesn't care who you are or check your ID usage. It just checks: **"Do you have a valid token?"**
Crucially, if you drop your bag of tokens and someone else picks it up, **they can play your games**. The machine respects the *Bearer* of the token, not necessarily the owner.

## 2️⃣ REAL-LIFE ANALOGY
**The Cashier's Check / Cash:**
- A check made out to "Cash" or a $20 bill.
- Whoever holds it (bears it) can spend it.
- The bank/store doesn't check ID; the value is in the paper itself.

## 3️⃣ WHY THIS EXISTS
**Ideally:** We don't want to send the username and password on every request (like Basic Auth) because that's risky.
**The Fix:** We send credentials *once* to get a temporary key (Token). We use that key for the next hour.
**The Benefit:** If the token is stolen, it expires soon. If the password is stolen, it's permanent.

## 4️⃣ CORE CONCEPTS
- **Bearer:** "Give access to the bearer of this token."
- **Header:** `Authorization: Bearer <token-string>`
- **Decoupling:** Authentication (Login) is separated from Authorization (Access).
- **Format Agnostic:** The token can be a UUID (Opaque) or a JSON object (JWT). "Bearer" is just the *transport mechanism*.

## 5️⃣ VISUAL DIAGRAMS

**Token Exchange Flow:**
```mermaid
sequenceDiagram
    participant User
    participant App
    participant AuthServer
    participant ResourceAPI

    User->>App: Login (User/Pass)
    App->>AuthServer: Send Credentials (HTTPS)
    AuthServer-->>App: Return Token "abc-123-xyz" (Expires in 1hr)
    
    Note right of App: App stores Token safely
    
    App->>ResourceAPI: GET /data<br/>Authorization: Bearer abc-123-xyz
    ResourceAPI->>ResourceAPI: Check is token valid?
    ResourceAPI-->>App: 200 OK (Data)
```

## 6️⃣ SIMPLE EXAMPLE (BEGINNER)

**Raw HTTP Request:**
```http
GET /my-profile HTTP/1.1
Host: api.todo-app.com
Authorization: Bearer 9812-7812-adba-1232
```

## 7️⃣ DEEP DIVE (ADVANCED)

### Token Granularity
Unlike Basic Auth (which is total access), Tokens can have **Scopes** (permissions). A token might be valid *only* for "Reading Emails" but not "Deleting Emails".

### Stateless vs Stateful
- **Opaque Tokens:** Random strings (UUID). The server *must* check a database to see who implies. (Stateful).
- **Self-Contained (JWT):** The token contains the data. Server just verifies the signature. (Stateless).
*Note: Bearer Auth supports BOTH.*

### Revocation
- **Opaque:** Easy. Just delete the UUID from the database (Redis).
- **JWT:** Hard. Requires a deny-list (blacklist) or short expiry times.

## 8️⃣ SPRING & JAVA IMPLEMENTATION

### 8.1 Opaque Token Implementation with Redis

**Token Entity:**
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpaqueToken {
    private String tokenValue;
    private String username;
    private Set<String> scopes;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private String clientId;
    private Map<String, Object> additionalInfo;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public long getSecondsUntilExpiration() {
        return ChronoUnit.SECONDS.between(LocalDateTime.now(), expiresAt);
    }
}
```

**Token Repository (Redis):**
```java
@Repository
@Slf4j
public class TokenRepository {
    
    private final RedisTemplate<String, OpaqueToken> redisTemplate;
    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";
    
    public TokenRepository(RedisTemplate<String, OpaqueToken> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public void save(OpaqueToken token) {
        String key = TOKEN_PREFIX + token.getTokenValue();
        long ttl = token.getSecondsUntilExpiration();
        
        redisTemplate.opsForValue().set(key, token, ttl, TimeUnit.SECONDS);
        
        // Track user's active tokens
        String userKey = USER_TOKENS_PREFIX + token.getUsername();
        redisTemplate.opsForSet().add(userKey, token.getTokenValue());
        redisTemplate.expire(userKey, ttl, TimeUnit.SECONDS);
        
        log.debug("Saved token for user: {}, expires in: {} seconds", 
            token.getUsername(), ttl);
    }
    
    public Optional<OpaqueToken> findByTokenValue(String tokenValue) {
        String key = TOKEN_PREFIX + tokenValue;
        OpaqueToken token = redisTemplate.opsForValue().get(key);
        
        if (token != null && token.isExpired()) {
            delete(tokenValue);
            return Optional.empty();
        }
        
        return Optional.ofNullable(token);
    }
    
    public void delete(String tokenValue) {
        String key = TOKEN_PREFIX + tokenValue;
        OpaqueToken token = redisTemplate.opsForValue().get(key);
        
        if (token != null) {
            redisTemplate.delete(key);
            
            // Remove from user's token set
            String userKey = USER_TOKENS_PREFIX + token.getUsername();
            redisTemplate.opsForSet().remove(userKey, tokenValue);
            
            log.debug("Deleted token for user: {}", token.getUsername());
        }
    }
    
    public Set<String> findTokensByUsername(String username) {
        String userKey = USER_TOKENS_PREFIX + username;
        return redisTemplate.opsForSet().members(userKey);
    }
    
    public void deleteAllUserTokens(String username) {
        Set<String> tokens = findTokensByUsername(username);
        if (tokens != null) {
            tokens.forEach(this::delete);
        }
        log.info("Deleted all tokens for user: {}", username);
    }
}
```

**Token Service:**
```java
@Service
@Slf4j
public class TokenService {
    
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    
    @Value("${app.security.token.expiration:3600}") // 1 hour default
    private long tokenExpirationSeconds;
    
    @Value("${app.security.refresh-token.expiration:604800}") // 7 days default
    private long refreshTokenExpirationSeconds;
    
    public TokenService(TokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Generate a new access token
     */
    public OpaqueToken generateAccessToken(String username, Set<String> scopes) {
        String tokenValue = UUID.randomUUID().toString() + "-" + 
                           System.currentTimeMillis();
        
        LocalDateTime now = LocalDateTime.now();
        
        OpaqueToken token = new OpaqueToken();
        token.setTokenValue(tokenValue);
        token.setUsername(username);
        token.setScopes(scopes != null ? scopes : Set.of("read"));
        token.setIssuedAt(now);
        token.setExpiresAt(now.plusSeconds(tokenExpirationSeconds));
        token.setClientId("default-client");
        token.setAdditionalInfo(new HashMap<>());
        
        tokenRepository.save(token);
        
        log.info("Generated access token for user: {}, expires at: {}", 
            username, token.getExpiresAt());
        
        return token;
    }
    
    /**
     * Generate a refresh token (longer expiration)
     */
    public OpaqueToken generateRefreshToken(String username) {
        String tokenValue = "refresh_" + UUID.randomUUID().toString();
        
        LocalDateTime now = LocalDateTime.now();
        
        OpaqueToken token = new OpaqueToken();
        token.setTokenValue(tokenValue);
        token.setUsername(username);
        token.setScopes(Set.of("refresh"));
        token.setIssuedAt(now);
        token.setExpiresAt(now.plusSeconds(refreshTokenExpirationSeconds));
        token.setClientId("default-client");
        token.setAdditionalInfo(Map.of("type", "refresh"));
        
        tokenRepository.save(token);
        
        log.info("Generated refresh token for user: {}", username);
        
        return token;
    }
    
    /**
     * Validate and retrieve token
     */
    public Optional<OpaqueToken> validateToken(String tokenValue) {
        return tokenRepository.findByTokenValue(tokenValue);
    }
    
    /**
     * Revoke a specific token
     */
    public void revokeToken(String tokenValue) {
        tokenRepository.delete(tokenValue);
        log.info("Revoked token: {}", tokenValue);
    }
    
    /**
     * Revoke all tokens for a user (logout from all devices)
     */
    public void revokeAllUserTokens(String username) {
        tokenRepository.deleteAllUserTokens(username);
        log.info("Revoked all tokens for user: {}", username);
    }
    
    /**
     * Refresh access token using refresh token
     */
    public OpaqueToken refreshAccessToken(String refreshTokenValue) {
        Optional<OpaqueToken> refreshToken = tokenRepository.findByTokenValue(refreshTokenValue);
        
        if (refreshToken.isEmpty()) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        
        if (!refreshToken.get().getScopes().contains("refresh")) {
            throw new InvalidTokenException("Not a refresh token");
        }
        
        // Generate new access token
        return generateAccessToken(
            refreshToken.get().getUsername(),
            Set.of("read", "write")
        );
    }
}
```

### 8.2 Authentication Filter

**Bearer Token Authentication Filter:**
```java
@Component
@Slf4j
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {
    
    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;
    
    public BearerTokenAuthenticationFilter(
            TokenService tokenService,
            UserDetailsService userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                authenticateWithToken(token);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
    
    private void authenticateWithToken(String token) {
        Optional<OpaqueToken> opaqueToken = tokenService.validateToken(token);
        
        if (opaqueToken.isPresent()) {
            OpaqueToken validToken = opaqueToken.get();
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(
                validToken.getUsername());
            
            // Create authentication with scopes
            List<GrantedAuthority> authorities = new ArrayList<>(userDetails.getAuthorities());
            
            // Add scope-based authorities
            validToken.getScopes().forEach(scope -> 
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
            
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    authorities
                );
            
            authentication.setDetails(validToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Authenticated user: {} with scopes: {}", 
                validToken.getUsername(), validToken.getScopes());
        }
    }
}
```

### 8.3 Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class BearerTokenSecurityConfig {
    
    private final BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    
    public BearerTokenSecurityConfig(
            BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.bearerTokenAuthenticationFilter = bearerTokenAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Stateless API
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            
            // Add Bearer Token filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(bearerTokenAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### 8.4 Authentication Controller

```java
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthenticationController {
    
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    
    public AuthenticationController(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Extract roles as scopes
            Set<String> scopes = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.replace("ROLE_", "").toLowerCase())
                .collect(Collectors.toSet());
            
            scopes.addAll(Set.of("read", "write"));
            
            OpaqueToken accessToken = tokenService.generateAccessToken(
                userDetails.getUsername(), scopes);
            
            OpaqueToken refreshToken = tokenService.generateRefreshToken(
                userDetails.getUsername());
            
            log.info("User {} logged in successfully", request.getUsername());
            
            return ResponseEntity.ok(new TokenResponse(
                accessToken.getTokenValue(),
                refreshToken.getTokenValue(),
                "Bearer",
                accessToken.getSecondsUntilExpiration()
            ));
            
        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(null);
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        try {
            OpaqueToken newAccessToken = tokenService.refreshAccessToken(
                request.getRefreshToken());
            
            return ResponseEntity.ok(new TokenResponse(
                newAccessToken.getTokenValue(),
                request.getRefreshToken(), // Same refresh token
                "Bearer",
                newAccessToken.getSecondsUntilExpiration()
            ));
            
        } catch (InvalidTokenException e) {
            log.warn("Invalid refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7); // Remove "Bearer "
        tokenService.revokeToken(token);
        
        SecurityContextHolder.clearContext();
        
        log.info("User logged out");
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        String username = authentication.getName();
        tokenService.revokeAllUserTokens(username);
        
        SecurityContextHolder.clearContext();
        
        log.info("User {} logged out from all devices", username);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/introspect")
    public ResponseEntity<TokenIntrospectionResponse> introspect(
            @RequestParam String token) {
        
        Optional<OpaqueToken> opaqueToken = tokenService.validateToken(token);
        
        if (opaqueToken.isEmpty()) {
            return ResponseEntity.ok(new TokenIntrospectionResponse(false));
        }
        
        OpaqueToken validToken = opaqueToken.get();
        
        return ResponseEntity.ok(new TokenIntrospectionResponse(
            true,
            validToken.getUsername(),
            validToken.getScopes(),
            validToken.getExpiresAt().toEpochSecond(ZoneOffset.UTC),
            validToken.getIssuedAt().toEpochSecond(ZoneOffset.UTC),
            validToken.getClientId()
        ));
    }
}

// DTOs
@Data
@AllArgsConstructor
@NoArgsConstructor
class LoginRequest {
    private String username;
    private String password;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class RefreshRequest {
    private String refreshToken;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class TokenIntrospectionResponse {
    private boolean active;
    private String username;
    private Set<String> scopes;
    private long exp;
    private long iat;
    private String clientId;
    
    public TokenIntrospectionResponse(boolean active) {
        this.active = active;
    }
}

class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
```

### 8.5 Redis Configuration

```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, OpaqueToken> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, OpaqueToken> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use Jackson for JSON serialization
        Jackson2JsonRedisSerializer<OpaqueToken> serializer =
            new Jackson2JsonRedisSerializer<>(OpaqueToken.class);
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        serializer.setObjectMapper(objectMapper);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
```

### 8.6 Scope-Based Authorization

**Method Security with Scopes:**
```java
@RestController
@RequestMapping("/api/documents")
@Slf4j
public class DocumentController {
    
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read')")
    public ResponseEntity<List<String>> getDocuments() {
        return ResponseEntity.ok(List.of("doc1.pdf", "doc2.pdf"));
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_write')")
    public ResponseEntity<String> createDocument(@RequestBody String content) {
        log.info("Creating document");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body("Document created");
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_delete') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        log.info("Deleting document: {}", id);
        return ResponseEntity.noContent().build();
    }
}
```

## 9️⃣ HANDS-ON LAB (NO COST)

### Lab 1: Complete Token Lifecycle

**Goal:** Implement and test the full token authentication flow.

**Step 1: Setup Project**
Use the complete code from Section 8 (Token Entity, Repository, Service, Filter, Controllers).

**Step 2: Test Login Flow**
```bash
# 1. Login and get tokens
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}'

# Response:
# {
#   "accessToken": "abc-123-xyz-456",
#   "refreshToken": "refresh_def-789-uvw-012",
#   "tokenType": "Bearer",
#   "expiresIn": 3600
# }

# Save the access token
export ACCESS_TOKEN="abc-123-xyz-456"
export REFRESH_TOKEN="refresh_def-789-uvw-012"
```

**Step 3: Use Access Token**
```bash
# Access protected endpoint
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/user/profile

# Test scope-based access
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/documents

# Test without token (should fail)
curl http://localhost:8080/api/user/profile
```

**Step 4: Token Introspection**
```bash
# Check token validity
curl "http://localhost:8080/api/auth/introspect?token=$ACCESS_TOKEN"

# Response:
# {
#   "active": true,
#   "username": "user",
#   "scopes": ["read", "write", "user"],
#   "exp": 1735545600,
#   "iat": 1735542000,
#   "clientId": "default-client"
# }
```

**Step 5: Token Refresh**
```bash
# When access token expires, use refresh token
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"

# Get new access token
export NEW_ACCESS_TOKEN="new-token-value"
```

**Step 6: Logout**
```bash
# Single device logout
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# Logout from all devices
curl -X POST http://localhost:8080/api/auth/logout-all \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Lab 2: Multi-Device Session Management

**Goal:** Manage tokens across multiple devices.

**Scenario:** User logs in from Phone, Laptop, and Tablet.

```bash
# Device 1: Phone
PHONE_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}' \
  | jq -r '.accessToken')

# Device 2: Laptop  
LAPTOP_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}' \
  | jq -r '.accessToken')

# Device 3: Tablet
TABLET_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}' \
  | jq -r '.accessToken')

# Verify all tokens work
curl -H "Authorization: Bearer $PHONE_TOKEN" \
  http://localhost:8080/api/user/profile

curl -H "Authorization: Bearer $LAPTOP_TOKEN" \
  http://localhost:8080/api/user/profile

curl -H "Authorization: Bearer $TABLET_TOKEN" \
  http://localhost:8080/api/user/profile

# Check active sessions in Redis
docker exec -it security-redis redis-cli KEYS "user:tokens:user"
docker exec -it security-redis redis-cli SMEMBERS "user:tokens:user"

# Logout from all devices
curl -X POST http://localhost:8080/api/auth/logout-all \
  -H "Authorization: Bearer $PHONE_TOKEN"

# Verify all tokens are now invalid
curl -H "Authorization: Bearer $LAPTOP_TOKEN" \
  http://localhost:8080/api/user/profile
# Should return 401
```

---

### Lab 3: Token Security Testing

**Goal:** Test security vulnerabilities and protections.

**Test 1: Token Replay Attack**
```bash
# Get a token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}' \
  | jq -r '.accessToken')

# Use it multiple times (should work - tokens are reusable until expiry)
for i in {1..5}; do
  curl -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/user/profile
done

# Logout
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"

# Try to reuse after logout (should fail)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile
```

**Test 2: Token Tampering**
```bash
# Get a valid token
VALID_TOKEN="abc-123-xyz-456"

# Modify the token (simulate tampering)
TAMPERED_TOKEN="abc-123-xyz-HACKED"

# Try to use tampered token (should fail)
curl -H "Authorization: Bearer $TAMPERED_TOKEN" \
  http://localhost:8080/api/user/profile
```

**Test 3: Scope Escalation**
```bash
# Login as regular user
USER_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}' \
  | jq -r '.accessToken')

# Try to access admin endpoint (should fail with 403)
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8080/api/admin/users

# Check token scopes
curl "http://localhost:8080/api/auth/introspect?token=$USER_TOKEN" \
  | jq '.scopes'
```

**Test 4: Token Expiration**
```bash
# Temporarily set token expiration to 10 seconds in application.yml
# app.security.token.expiration: 10

# Get token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}' \
  | jq -r '.accessToken')

# Use immediately (should work)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile

# Wait 15 seconds
sleep 15

# Try again (should fail - token expired)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile
```

---

### Lab 4: Performance Testing

**Goal:** Compare opaque vs JWT token performance.

**Benchmark Opaque Token Validation:**
```bash
# Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}' \
  | jq -r '.accessToken')

# Benchmark with Apache Bench
ab -n 1000 -c 10 \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile

# Note: Each request requires Redis lookup
# Typical: 500-1000 req/sec
```

**Monitor Redis Performance:**
```bash
# Connect to Redis
docker exec -it security-redis redis-cli

# Monitor commands in real-time
MONITOR

# In another terminal, make requests
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile

# Check Redis stats
INFO stats
```

---

### Lab 5: Integration Testing

**Create Test Class:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BearerTokenIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String accessToken;
    private String refreshToken;
    
    @BeforeEach
    void setup() {
        // Generate tokens for testing
        OpaqueToken token = tokenService.generateAccessToken(
            "user", Set.of("read", "write"));
        accessToken = token.getTokenValue();
        
        OpaqueToken refresh = tokenService.generateRefreshToken("user");
        refreshToken = refresh.getTokenValue();
    }
    
    @Test
    void login_ShouldReturnTokens_WithValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user", "user123");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").isNumber());
    }
    
    @Test
    void accessProtectedEndpoint_ShouldSucceed_WithValidToken() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("user"));
    }
    
    @Test
    void accessProtectedEndpoint_ShouldFail_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void refreshToken_ShouldReturnNewAccessToken() throws Exception {
        RefreshRequest request = new RefreshRequest(refreshToken);
        
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.accessToken").value(not(accessToken)));
    }
    
    @Test
    void logout_ShouldInvalidateToken() throws Exception {
        // Use token (should work)
        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());
        
        // Logout
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNoContent());
        
        // Try to use token again (should fail)
        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void scopeBasedAccess_ShouldEnforcePermissions() throws Exception {
        // Token with only 'read' scope
        OpaqueToken readOnlyToken = tokenService.generateAccessToken(
            "user", Set.of("read"));
        
        // GET should work
        mockMvc.perform(get("/api/documents")
                .header("Authorization", "Bearer " + readOnlyToken.getTokenValue()))
            .andExpect(status().isOk());
        
        // POST should fail (requires 'write' scope)
        mockMvc.perform(post("/api/documents")
                .header("Authorization", "Bearer " + readOnlyToken.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
}
```

**Run Tests:**
```bash
./mvnw test -Dtest=BearerTokenIntegrationTest
```

---

### Troubleshooting

**Problem 1: "Token not found" even after login**
```bash
# Check if token was saved in Redis
docker exec -it security-redis redis-cli KEYS "token:*"

# Check token TTL
docker exec -it security-redis redis-cli TTL "token:YOUR_TOKEN_VALUE"

# Verify Redis connection in application
curl http://localhost:8080/actuator/health
```

**Problem 2: Token works but scopes not enforced**
```bash
# Check token scopes
curl "http://localhost:8080/api/auth/introspect?token=YOUR_TOKEN"

# Verify @EnableMethodSecurity is present in config
# Ensure @PreAuthorize uses correct syntax: hasAuthority('SCOPE_read')
```

**Problem 3: Refresh token not working**
```bash
# Verify refresh token exists
docker exec -it security-redis redis-cli GET "token:refresh_YOUR_TOKEN"

# Check if refresh token has 'refresh' scope
curl "http://localhost:8080/api/auth/introspect?token=refresh_YOUR_TOKEN" \
  | jq '.scopes'
```

## 🔟 INTERVIEW QUESTIONS

**Beginner:**
Q: What happens if I copy your Bearer token and use it on my computer?
A: It will work properly. The server treats the bearer as the legitimate user. This is why TLS and XSS protection are critical.

**Senior:**
Q: Comparison: Bearer Token vs Session Cookie?
A: Cookies can be HttpOnly (XSS safe) but suffer from CSRF. Bearer Tokens are CSRF safe (since JS must attach them) but vulnerable to XSS theft. Mobile apps prefer Tokens; Legacy Web Apps prefer Cookies.

**Architect:**
Q: How do you handle "Logout" with Bearer tokens?
A: You cannot "delete" a token from the client effectively (they could have copied it). You must either: 1. Use short expiry (15 min). 2. Maintain a blacklist (stateful). 3. Rotate the signing keys (nuclear option).

## 1️⃣1️⃣ WHEN TO USE / WHEN NOT TO USE

| Scenario | Decision | Reason |
| :--- | :--- | :--- |
| **Mobile Apps** | ✅ YES | Native apps handle header injection easily. |
| **SPA (React/Angular)** | ⚠️ CAUTION | Storing valid tokens in LocalStorage is an XSS risk. |
| **Server-to-Server** | ✅ YES | Standard for Microservices. |
| **Banking/High-Security** | ⚠️ WITH CAUTION | Often paired with Mutual TLS (mTLS) so "bearing" the token isn't enough; you must also own the certificate. |

## 1️⃣2️⃣ SECURITY ATTACKS & DEFENSE (THREAT MODELING)

**STRIDE Analysis for Bearer Tokens:**

*   **Spoofing:**
**STRIDE Analysis for Bearer Tokens:**

| Threat | Attack Vector | Impact | Mitigation |
| :--- | :--- | :--- | :--- |
| **Spoofing** | **Token Replay.** Attacker steals the Bearer token (XSS/Sniffing) and uses it. | Impersonation. The server cannot distinguish thief from user. | **Short-lived tokens** (15 min). Use **Sender-Constrained Tokens** (DPoP/mTLS) for high security. |
| **Tampering** | Attacker modifies the scopes inside the JWT. | Privilege Escalation. User grants themselves Admin rights. | **Digital Signatures** (HMAC/RSA). Server MUST verify signature before reading payload. |
| **Information Disclosure** | Token leaked in URL query params or Server Logs. | Account takeover. | **NEVER** use `access_token=` in URLs. Use Headers only. Mask logs. |
| **Elevation of Privilege** | User creates a token with `role: admin`. | Full System Compromise. | **Signature Validation.** Ensure secret keys are rotated and secure. |
*   **Bearer Auth**
    *   **Mechanism**: Possession = Access
    *   **Risk**: Theft is fatal (until expiry)
    *   **Variant**: Opaque (Reference) vs JWT (Value)
    *   **Key Feature**: Scopes & Expiry

---

### 🔟 ARCHITECT DECISION NOTES
> **Implementation**: **Bearer Authentication**
>
> **The Verdict**: **The Standard for Modern APIs.**
>
> **Reasoning**: It cleanly separates the *Identity Provider* (who issues the token) from the *Resource Server* (who consumes it). This is the foundation of OAuth2 and OIDC.
>
> **Trade-off**: You accept the "Bearer" risk (stolen token = stolen identity). For extremely high-security environments (finance, defense), you might implement **Sender-Constrained Tokens** (DPoP or mTLS), where the token is cryptographically bound to the client's connection, so stealing it is useless.

---

# 3. OAuth 2.0 (Open Authorization)

## 1️⃣ ELI5 (Explain Like I’m 5)
**The Valet Key Analogy:**
You have a luxury car (Your Data). You drive to a hotel and give the keys to the Valet (The App).
You do **not** give the Valet your master key that opens the glovebox and trunk and allows selling the car.
You give them a **Valet Key** (Access Token).
This key has limits:
- Can drive the car (Scope: `drive`)
- Cannot open the trunk (Scope: `no-trunk`)
- Stops working after 30 minutes (Expiry)

OAuth 2.0 is the framework for creating these "Valet Keys" so apps can access your data without stealing your master password.

## 2️⃣ REAL-LIFE ANALOGY
**"Log in with Google":**
- You want to play a new game (Client).
- The game asks: "Can I see your name and email?" (Authorization Request).
- You are redirected to Google (Authorization Server).
- You sign in to Google (not the game!).
- You say "Yes" to the game.
- Google gives the game a token.
- The game uses the token to get your name. **The game never sees your Google password.**

## 3️⃣ WHY THIS EXISTS
**Ideally:** I want a printing service to print my Google Photos.
**Before OAuth:** I had to give the printing service my Google username and password. This is insanely dangerous (they could delete my emails!).
**The Fix:** OAuth lets me give the printer purely "Read-Only Access to Photos" and nothing else.

## 4️⃣ CORE CONCEPTS
- **Resource Owner:** YOU (The User).
- **Client:** The App (e.g., The Printing Service).
- **Authorization Server:** The Authority (e.g., Google/Okta).
- **Resource Server:** The API (e.g., Google Photos API).
- **Grant Types:** different ways to get the key (Auth Code, Client Creds, Refresh Token).

## 5️⃣ VISUAL DIAGRAMS

**Authorization Code Flow (The Gold Standard):**

```mermaid
sequenceDiagram
    participant User
    participant App as Client App
    participant Auth as Auth Server
    participant API as Resource Server

    User->>App: Click "Connect with Google"
    App->>Auth: Redirect to Auth Server<br/>(client_id, scope, redirect_uri)
    Auth->>User: "Allow App to read Photos?"
    User->>Auth: Yes!
    Auth->>App: Redirect back with CODE (Temporary)
    App->>Auth: Exchange CODE for TOKEN (Back-channel)
    Auth-->>App: Access Token + Refresh Token
    App->>API: GET /photos<br/>Authorization: Bearer <Token>
    API-->>App: 200 OK (Photos)
```

## 6️⃣ SIMPLE EXAMPLE (BEGINNER)

**1. The Authorization Link (Frontend):**
```text
https://accounts.google.com/o/oauth2/auth?
  client_id=YOUR_APP_ID
  &redirect_uri=https://yourapp.com/callback
  &response_type=code
  &scope=email profile
```

**2. The Exchange (Backend):**
```http
POST /token
Host: oauth2.googleapis.com
Content-Type: application/x-www-form-urlencoded

code=AUTH_CODE_FROM_URL
&client_id=...
&client_secret=...
&redirect_uri=...
&grant_type=authorization_code
```
**Response:** `{ "access_token": "ya29...", "expires_in": 3600 }`

## 7️⃣ DEEP DIVE (ADVANCED)

### Opaque vs JWT Tokens (Architect Decision)
OAuth2 spec does **not** define the token format.
- **Opaque:** Random string. Client must call `/introspect` endpoint on Auth Server to validate. Good for immediate revocation.
- **JWT:** Structural token. Resource Server validates signature locally. Faster, scalable, but harder to revoke.

### Grant Types Breakdown
1.  **Authorization Code:** For Server-side apps. (Safest).
2.  **PKCE (Proof Key for Code Exchange):** For Mobile/SPA. Prevents code interception. **Mandatory for modern frontend apps.**
3.  **Client Credentials:** For Machine-to-Machine (Service A calling Service B). No User involved.
4.  **Implicit:** ❌ DEPRECATED. Do not use.

## 8️⃣ SPRING & JAVA IMPLEMENTATION

### 8.1 Spring Authorization Server Setup

**Dependencies (pom.xml):**
```xml
<dependencies>
    <!-- Spring Authorization Server -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
    </dependency>
    
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Spring Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
</dependencies>
```

**Authorization Server Configuration:**
```java
@Configuration
@Slf4j
public class AuthorizationServerConfig {
    
    /**
     * Configure OAuth2 Authorization Server
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http) throws Exception {
        
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults()); // Enable OpenID Connect 1.0
        
        http
            // Redirect to login page when not authenticated
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            )
            // Accept access tokens for User Info and/or Client Registration
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults()));
        
        return http.build();
    }
    
    /**
     * Default security filter chain for login
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) 
            throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());
        
        return http.build();
    }
    
    /**
     * Register OAuth2 clients
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // Client 1: Authorization Code + PKCE (for SPAs)
        RegisteredClient spaClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("spa-client")
            .clientSecret("{noop}") // No secret for public clients
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:3000/callback")
            .redirectUri("http://localhost:3000/silent-renew")
            .postLogoutRedirectUri("http://localhost:3000/")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .scope("read")
            .scope("write")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .requireProofKey(true) // Require PKCE
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(15))
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .reuseRefreshTokens(false) // Rotate refresh tokens
                .build())
            .build();
        
        // Client 2: Client Credentials (for service-to-service)
        RegisteredClient serviceClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("service-client")
            .clientSecret("{bcrypt}$2a$10$...") // BCrypt encoded secret
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scope("service.read")
            .scope("service.write")
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .build())
            .build();
        
        // Client 3: Authorization Code (for traditional web apps)
        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("web-client")
            .clientSecret("{bcrypt}$2a$10$...")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/login/oauth2/code/web-client")
            .redirectUri("http://localhost:8080/authorized")
            .postLogoutRedirectUri("http://localhost:8080/")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .scope("read")
            .scope("write")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(30))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .build())
            .build();
        
        return new InMemoryRegisteredClientRepository(spaClient, serviceClient, webClient);
    }
    
    /**
     * JWK Source for signing tokens
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
        
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }
    
    /**
     * Generate RSA key pair for JWT signing
     */
    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }
    
    /**
     * JWT decoder for validating tokens
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
    
    /**
     * Authorization Server settings
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://localhost:9000") // Authorization server URL
            .build();
    }
}
```

### 8.2 PKCE Implementation (For SPAs and Mobile Apps)

**PKCE Flow Explanation:**
```
1. Client generates random code_verifier (43-128 chars)
2. Client creates code_challenge = BASE64URL(SHA256(code_verifier))
3. Client sends code_challenge in authorization request
4. Server stores code_challenge
5. Client sends code_verifier in token exchange
6. Server verifies: SHA256(code_verifier) == stored code_challenge
```

**Frontend (React/JavaScript) - PKCE Generation:**
```javascript
// Generate random code verifier
function generateCodeVerifier() {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return base64URLEncode(array);
}

// Generate code challenge from verifier
async function generateCodeChallenge(verifier) {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const hash = await crypto.subtle.digest('SHA-256', data);
    return base64URLEncode(new Uint8Array(hash));
}

// Base64 URL encoding
function base64URLEncode(buffer) {
    return btoa(String.fromCharCode(...buffer))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
}

// Usage
const codeVerifier = generateCodeVerifier();
const codeChallenge = await generateCodeChallenge(codeVerifier);

// Store code_verifier in sessionStorage
sessionStorage.setItem('code_verifier', codeVerifier);

// Build authorization URL
const authUrl = `http://localhost:9000/oauth2/authorize?` +
    `response_type=code` +
    `&client_id=spa-client` +
    `&redirect_uri=http://localhost:3000/callback` +
    `&scope=openid profile email read write` +
    `&code_challenge=${codeChallenge}` +
    `&code_challenge_method=S256` +
    `&state=${generateRandomState()}`;

// Redirect to authorization server
window.location.href = authUrl;
```

**Backend Validation (Spring handles this automatically):**
```java
// Spring Authorization Server automatically validates PKCE
// when requireProofKey(true) is set in ClientSettings

// Custom PKCE validator (if needed)
@Component
public class CustomPkceValidator {
    
    public boolean validate(String codeVerifier, String codeChallenge) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            String computedChallenge = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hash);
            
            return computedChallenge.equals(codeChallenge);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}
```

### 8.3 Client Credentials Flow (Service-to-Service)

**Service A (Client) - Requesting Token:**
```java
@Service
@Slf4j
public class ServiceAClient {
    
    private final RestTemplate restTemplate;
    private final String tokenEndpoint = "http://localhost:9000/oauth2/token";
    private final String clientId = "service-client";
    private final String clientSecret = "service-secret";
    
    private String cachedToken;
    private Instant tokenExpiry;
    
    public ServiceAClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }
    
    /**
     * Get access token with caching
     */
    public String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        
        return refreshToken();
    }
    
    private String refreshToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", "service.read service.write");
        
        HttpEntity<MultiValueMap<String, String>> request = 
            new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                tokenEndpoint,
                request,
                TokenResponse.class
            );
            
            TokenResponse tokenResponse = response.getBody();
            this.cachedToken = tokenResponse.getAccessToken();
            this.tokenExpiry = Instant.now().plusSeconds(
                tokenResponse.getExpiresIn() - 60); // 60s buffer
            
            log.info("Obtained new access token, expires in {} seconds", 
                tokenResponse.getExpiresIn());
            
            return cachedToken;
            
        } catch (Exception e) {
            log.error("Failed to obtain access token", e);
            throw new RuntimeException("Token acquisition failed", e);
        }
    }
    
    /**
     * Call Service B with token
     */
    public String callServiceB(String endpoint) {
        String token = getAccessToken();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "http://service-b:8081" + endpoint,
            HttpMethod.GET,
            request,
            String.class
        );
        
        return response.getBody();
    }
}

@Data
class TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private long expiresIn;
    
    @JsonProperty("scope")
    private String scope;
}
```

**Service B (Resource Server) - Validating Token:**
```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/service/**").hasAuthority("SCOPE_service.read")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        
        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // Point to Authorization Server's JWK Set endpoint
        return NimbusJwtDecoder.withJwkSetUri(
            "http://localhost:9000/oauth2/jwks"
        ).build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = 
            new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            grantedAuthoritiesConverter);
        
        return jwtAuthenticationConverter;
    }
}
```

### 8.4 Custom Grant Type Implementation

**Device Code Flow (for IoT/TV devices):**
```java
@Component
public class DeviceCodeGrantAuthenticationProvider implements AuthenticationProvider {
    
    private final DeviceCodeService deviceCodeService;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    
    @Override
    public Authentication authenticate(Authentication authentication) 
            throws AuthenticationException {
        
        DeviceCodeAuthenticationToken deviceCodeAuth = 
            (DeviceCodeAuthenticationToken) authentication;
        
        String deviceCode = deviceCodeAuth.getDeviceCode();
        
        // Validate device code
        DeviceCodeData data = deviceCodeService.validateDeviceCode(deviceCode);
        
        if (data == null || data.isExpired()) {
            throw new OAuth2AuthenticationException("invalid_grant");
        }
        
        if (!data.isUserApproved()) {
            throw new OAuth2AuthenticationException("authorization_pending");
        }
        
        // Generate tokens
        OAuth2AccessToken accessToken = generateAccessToken(data);
        OAuth2RefreshToken refreshToken = generateRefreshToken(data);
        
        return new OAuth2AccessTokenAuthenticationToken(
            data.getRegisteredClient(),
            data.getClientPrincipal(),
            accessToken,
            refreshToken
        );
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return DeviceCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

### 8.5 Dynamic Client Registration

**Client Registration Endpoint:**
```java
@RestController
@RequestMapping("/oauth2/register")
@Slf4j
public class ClientRegistrationController {
    
    private final RegisteredClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    
    @PostMapping
    public ResponseEntity<ClientRegistrationResponse> registerClient(
            @RequestBody ClientRegistrationRequest request) {
        
        // Validate request
        validateRegistrationRequest(request);
        
        // Generate client credentials
        String clientId = "client_" + UUID.randomUUID().toString();
        String clientSecret = generateSecureSecret();
        
        // Create registered client
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(clientId)
            .clientSecret(passwordEncoder.encode(clientSecret))
            .clientName(request.getClientName())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUris(uris -> uris.addAll(request.getRedirectUris()))
            .scopes(scopes -> scopes.addAll(request.getScopes()))
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .requireProofKey(request.isRequirePkce())
                .build())
            .build();
        
        clientRepository.save(client);
        
        log.info("Registered new client: {}", clientId);
        
        return ResponseEntity.ok(new ClientRegistrationResponse(
            clientId,
            clientSecret, // Only returned once!
            request.getRedirectUris(),
            request.getScopes()
        ));
    }
    
    private String generateSecureSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

## 9️⃣ HANDS-ON LAB (NO COST)

### Lab 1: Authorization Server Setup

**Goal:** Set up a complete OAuth 2.0 Authorization Server with Spring.

**Step 1: Create Project**
```bash
curl https://start.spring.io/starter.tgz \
  -d dependencies=oauth2-authorization-server,web,security,data-jpa,postgresql \
  -d type=maven-project \
  -d bootVersion=3.2.0 \
  -d baseDir=auth-server \
  | tar -xzvf -

cd auth-server
```

**Step 2: Configure Application**
```yaml
# application.yml
server:
  port: 9000

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: update

logging:
  level:
    org.springframework.security: DEBUG
```

**Step 3: Add Configuration**
Use the complete `AuthorizationServerConfig` from Section 8.1.

**Step 4: Test Endpoints**
```bash
# Start the server
./mvnw spring-boot:run

# Check well-known configuration
curl http://localhost:9000/.well-known/oauth-authorization-server | jq

# Check JWKS endpoint
curl http://localhost:9000/oauth2/jwks | jq
```

**Expected Output:**
```json
{
  "issuer": "http://localhost:9000",
  "authorization_endpoint": "http://localhost:9000/oauth2/authorize",
  "token_endpoint": "http://localhost:9000/oauth2/token",
  "jwks_uri": "http://localhost:9000/oauth2/jwks",
  "response_types_supported": ["code"],
  "grant_types_supported": ["authorization_code", "client_credentials", "refresh_token"]
}
```

---

### Lab 2: PKCE Flow with React SPA

**Goal:** Implement complete PKCE flow with a React frontend.

**Step 1: Create React App**
```bash
npx create-react-app oauth-spa-client
cd oauth-spa-client
npm install
```

**Step 2: Create Auth Service**
```javascript
// src/services/authService.js
class AuthService {
    constructor() {
        this.authServerUrl = 'http://localhost:9000';
        this.clientId = 'spa-client';
        this.redirectUri = 'http://localhost:3000/callback';
    }

    // Generate code verifier
    generateCodeVerifier() {
        const array = new Uint8Array(32);
        crypto.getRandomValues(array);
        return this.base64URLEncode(array);
    }

    // Generate code challenge
    async generateCodeChallenge(verifier) {
        const encoder = new TextEncoder();
        const data = encoder.encode(verifier);
        const hash = await crypto.subtle.digest('SHA-256', data);
        return this.base64URLEncode(new Uint8Array(hash));
    }

    base64URLEncode(buffer) {
        return btoa(String.fromCharCode(...buffer))
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=/g, '');
    }

    generateState() {
        return this.base64URLEncode(crypto.getRandomValues(new Uint8Array(16)));
    }

    // Start login flow
    async login() {
        const codeVerifier = this.generateCodeVerifier();
        const codeChallenge = await this.generateCodeChallenge(codeVerifier);
        const state = this.generateState();

        // Store for later use
        sessionStorage.setItem('code_verifier', codeVerifier);
        sessionStorage.setItem('state', state);

        // Build authorization URL
        const params = new URLSearchParams({
            response_type: 'code',
            client_id: this.clientId,
            redirect_uri: this.redirectUri,
            scope: 'openid profile email read write',
            code_challenge: codeChallenge,
            code_challenge_method: 'S256',
            state: state
        });

        window.location.href = `${this.authServerUrl}/oauth2/authorize?${params}`;
    }

    // Handle callback
    async handleCallback() {
        const params = new URLSearchParams(window.location.search);
        const code = params.get('code');
        const state = params.get('state');
        const storedState = sessionStorage.getItem('state');

        // Verify state
        if (state !== storedState) {
            throw new Error('Invalid state parameter');
        }

        const codeVerifier = sessionStorage.getItem('code_verifier');

        // Exchange code for token
        const tokenResponse = await fetch(`${this.authServerUrl}/oauth2/token`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                grant_type: 'authorization_code',
                code: code,
                redirect_uri: this.redirectUri,
                client_id: this.clientId,
                code_verifier: codeVerifier
            })
        });

        const tokens = await tokenResponse.json();
        
        // Store tokens
        localStorage.setItem('access_token', tokens.access_token);
        localStorage.setItem('refresh_token', tokens.refresh_token);

        // Clean up
        sessionStorage.removeItem('code_verifier');
        sessionStorage.removeItem('state');

        return tokens;
    }

    // Get user info
    async getUserInfo() {
        const accessToken = localStorage.getItem('access_token');
        
        const response = await fetch(`${this.authServerUrl}/userinfo`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        });

        return await response.json();
    }
}

export default new AuthService();
```

**Step 3: Create Login Component**
```javascript
// src/components/Login.js
import React from 'react';
import authService from '../services/authService';

function Login() {
    const handleLogin = () => {
        authService.login();
    };

    return (
        <div>
            <h1>OAuth 2.0 PKCE Demo</h1>
            <button onClick={handleLogin}>Login with OAuth</button>
        </div>
    );
}

export default Login;
```

**Step 4: Create Callback Component**
```javascript
// src/components/Callback.js
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';

function Callback() {
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        authService.handleCallback()
            .then(() => navigate('/dashboard'))
            .catch(err => setError(err.message));
    }, [navigate]);

    if (error) {
        return <div>Error: {error}</div>;
    }

    return <div>Processing login...</div>;
}

export default Callback;
```

**Step 5: Test the Flow**
```bash
# Start React app
npm start

# Click "Login with OAuth"
# You'll be redirected to Authorization Server
# After login, you're redirected back with tokens
```

---

### Lab 3: Client Credentials for Microservices

**Goal:** Implement service-to-service authentication.

**Step 1: Create Service A (Client)**
```java
@SpringBootApplication
public class ServiceAApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceAApplication.class, args);
    }
}

// Use ServiceAClient from Section 8.3

@RestController
@RequestMapping("/api")
class ServiceAController {
    
    private final ServiceAClient serviceAClient;
    
    @GetMapping("/call-service-b")
    public String callServiceB() {
        return serviceAClient.callServiceB("/api/data");
    }
}
```

**Step 2: Create Service B (Resource Server)**
```java
@SpringBootApplication
public class ServiceBApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceBApplication.class, args);
    }
}

// Use ResourceServerConfig from Section 8.3

@RestController
@RequestMapping("/api")
class ServiceBController {
    
    @GetMapping("/data")
    @PreAuthorize("hasAuthority('SCOPE_service.read')")
    public Map<String, String> getData(Authentication auth) {
        return Map.of(
            "message", "Data from Service B",
            "client", auth.getName(),
            "timestamp", Instant.now().toString()
        );
    }
}
```

**Step 3: Test Service-to-Service Call**
```bash
# Terminal 1: Start Authorization Server
cd auth-server && ./mvnw spring-boot:run

# Terminal 2: Start Service B (port 8081)
cd service-b && ./mvnw spring-boot:run

# Terminal 3: Start Service A (port 8080)
cd service-a && ./mvnw spring-boot:run

# Terminal 4: Test the call
curl http://localhost:8080/api/call-service-b

# Expected: Service A gets token, calls Service B, returns data
```

---

### Lab 4: Token Introspection & Validation

**Goal:** Implement token introspection for opaque tokens.

**Step 1: Add Introspection Endpoint**
```java
@RestController
@RequestMapping("/oauth2")
public class TokenIntrospectionController {
    
    private final OAuth2AuthorizationService authorizationService;
    
    @PostMapping("/introspect")
    public ResponseEntity<Map<String, Object>> introspect(
            @RequestParam("token") String token,
            Authentication clientAuth) {
        
        OAuth2Authorization authorization = 
            authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        
        if (authorization == null) {
            return ResponseEntity.ok(Map.of("active", false));
        }
        
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = 
            authorization.getAccessToken();
        
        if (accessToken.isExpired()) {
            return ResponseEntity.ok(Map.of("active", false));
        }
        
        return ResponseEntity.ok(Map.of(
            "active", true,
            "scope", String.join(" ", authorization.getAuthorizedScopes()),
            "client_id", authorization.getRegisteredClientId(),
            "username", authorization.getPrincipalName(),
            "exp", accessToken.getToken().getExpiresAt().getEpochSecond(),
            "iat", accessToken.getToken().getIssuedAt().getEpochSecond()
        ));
    }
}
```

**Step 2: Test Introspection**
```bash
# Get a token first
TOKEN=$(curl -X POST http://localhost:9000/oauth2/token \
  -u service-client:service-secret \
  -d "grant_type=client_credentials" \
  -d "scope=service.read" \
  | jq -r '.access_token')

# Introspect the token
curl -X POST http://localhost:9000/oauth2/introspect \
  -u service-client:service-secret \
  -d "token=$TOKEN" \
  | jq

# Expected output:
# {
#   "active": true,
#   "scope": "service.read",
#   "client_id": "service-client",
#   "exp": 1735545600,
#   "iat": 1735542000
# }
```

---

### Lab 5: Production Deployment with Docker

**Goal:** Deploy Authorization Server with Docker Compose.

**Step 1: Create Dockerfile**
```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 2: Create Docker Compose**
```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: authdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s

  auth-server:
    build: .
    ports:
      - "9000:9000"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/authdb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on:
      postgres:
        condition: service_healthy

  service-a:
    build: ../service-a
    ports:
      - "8080:8080"
    environment:
      AUTH_SERVER_URL: http://auth-server:9000
    depends_on:
      - auth-server

  service-b:
    build: ../service-b
    ports:
      - "8081:8081"
    environment:
      AUTH_SERVER_JWK_URI: http://auth-server:9000/oauth2/jwks
    depends_on:
      - auth-server

volumes:
  postgres_data:
```

**Step 3: Build and Deploy**
```bash
# Build the application
./mvnw clean package -DskipTests

# Start all services
docker-compose up -d

# Check logs
docker-compose logs -f auth-server

# Test the setup
curl http://localhost:9000/.well-known/oauth-authorization-server | jq
```

**Step 4: Load Testing**
```bash
# Install Apache Bench
brew install httpd

# Get a token
TOKEN=$(curl -X POST http://localhost:9000/oauth2/token \
  -u service-client:service-secret \
  -d "grant_type=client_credentials" \
  | jq -r '.access_token')

# Load test token validation
ab -n 10000 -c 100 \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/data

# Check results
# Requests per second: ~5000-10000 (depending on hardware)
# Time per request: ~10-20ms
```

---

### Troubleshooting

**Problem 1: PKCE validation fails**
```bash
# Check code_verifier length (must be 43-128 characters)
echo -n "YOUR_CODE_VERIFIER" | wc -c

# Verify code_challenge generation
echo -n "YOUR_CODE_VERIFIER" | openssl dgst -sha256 -binary | base64 | tr '+/' '-_' | tr -d '='
```

**Problem 2: Client authentication fails**
```bash
# Verify client secret encoding
# In Spring, use BCryptPasswordEncoder
java -jar bcrypt-cli.jar encode "your-secret"

# Test with curl
curl -v -X POST http://localhost:9000/oauth2/token \
  -u "client-id:client-secret" \
  -d "grant_type=client_credentials"
```

**Problem 3: CORS errors in SPA**
```java
// Add CORS configuration to Authorization Server
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000"));
    configuration.setAllowedMethods(List.of("GET", "POST"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## 🔟 INTERVIEW QUESTIONS

**Beginner:**
Q: Why do we need an Authorization Code? Why not return the Token immediately?
A: Sending the token in the browser URL (redirect) is unsafe—it stays in history/logs. The "Code" is a temporary one-time ticket exchanged securely server-to-server.

**Senior:**
Q: What is PKCE (Pixie)?
A: Proof Key for Code Exchange. It protects public clients (SPAs/Mobile) that cannot keep a `client_secret` safe. It ensures the app that started the flow is the same one finishing it.

**Architect:**
Q: Scenario: We have 50 microservices. Should they all validate tokens against the Auth Server?
A: No. That creates a bottleneck.
Option A) Use JWTs with local signature validation (Zero Trust).
Option B) Use an API Gateway to validate Opaque tokens once, then pass JWTs downstream.

## 1️⃣1️⃣ WHEN TO USE / WHEN NOT TO USE

| Scenario | Decision | Reason |
| :--- | :--- | :--- |
| **Internal Monolith** | ❌ NO | Overkill. Use Session/Cookies. |
| **3rd Party Access** | ✅ YES | The main use case (e.g., "Allow Jira to access Slack"). |
| **Modern Microservices** | ✅ YES | Client Credentials Flow for service-to-service. |
| **SPA / Mobile** | ✅ YES | Use Auth Code + PKCE. |

## 1️⃣2️⃣ SECURITY ATTACKS & DEFENSE (THREAT MODELING)


**Client Credentials Flow (Machine-to-Machine):**

```mermaid
sequenceDiagram
    participant ServiceA
    participant AuthServer
    participant ServiceB

    ServiceA->>AuthServer: POST /token (client_id, client_secret)
    AuthServer-->>ServiceA: Access Token (JWT)
    
    ServiceA->>ServiceB: GET /api/data<br/>Authorization: Bearer <JWT>
    ServiceB->>ServiceB: Validate Signature
    ServiceB-->>ServiceA: 200 OK
```

## 1️⃣2️⃣ SECURITY ATTACKS & DEFENSE (THREAT MODELING)

**STRIDE Analysis for OAuth 2.0:**

| Threat | Attack Vector | Impact | Mitigation |
| :--- | :--- | :--- | :--- |
| **Spoofing** | **Open Redirect.** Attacker tricks Auth Server to redirect code to their site. | Auth Code theft. Attacker gets the user's session. | **Strict Redirect URI Whitelisting.** Exact match only (no wildcards). |
| **Tampering** | Man-in-the-Middle during exchange. | Code/Token substitution. | **TLS Everywhere.** Backend-to-Backend calls must use HTTPS. |
| **Repudiation** | "I didn't authorize that app." | User denies access. | **Consent Screens.** Force user to click "Allow". Audit log the consent. |
| **Information Disclosure** | **Leak via Referrer Header.** | Auth Code leaks to analytics tools. | Use `Referrer-Policy: no-referrer`. |
| **Elevation of Privilege** | **Scope Escalation.** App asks for `read`, gets `write`. | Excessive access. | Auth Server must ignore requested scopes if not pre-approved for that client. |

## 1️⃣3️⃣ MIND MAP

*   **OAuth 2.0**
    *   **Goal**: Delegation (Valet Key)
    *   **Roles**: Owner, Client, AuthServer, RS
    *   **Flows**: Auth Code (User), Client Creds (Machine)
    *   **Security**: PKCE, State, Redirect Allow-lists

---

### 🔟 ARCHITECT DECISION NOTES
> **Implementation**: **OAuth 2.0**
>
> **The Verdict**: **Mandatory for Federated Identity.**
>
> **Reasoning**: Do not build your own login forms or password hashing if you can avoid it. Using OAuth2 allows you to offload identity to Google, Microsoft, or a dedicated provider (Auth0/Keycloak).
>
> **Critial Decision**:
> *   **Public Clients (React/Mobile):** MUST use **PKCE**. Never trust the client with a secret.
> *   **Confidential Clients (Backend):** Use Client Secret or mTLS.
> *   **Token Format**: Use JWT for performance (internal), Opaque for high security (revocable).

---

# 4. JSON Web Tokens (JWT)

## 1️⃣ ELI5 (Explain Like I’m 5)
**The Notarized Document:**
Imagine you have a letter that says "I am allowed to enter the vault".
If you just write it on a napkin, no one believes you.
But if a **Notary Public** stamps it with an un-forgeable wax seal, then the guard believes the letter *without calling the Notary*.
The guard checks:
1. Is the wax seal broken? (Signature)
2. Is the date expired? (Expiration)
3. Does it say "Allowed"? (Claims)

A JWT is just a digital letter with a wax seal from the server.

## 2️⃣ REAL-LIFE ANALOGY
**A Passport or Driver's License:**
- It contains your data (Name, DOB, Class of license).
- It is issued by an Authority.
- It has a holographic strip (Signature) to prove it's real.
- A policeman can verify it *offline* just by looking at the hologram. They don't need to call the DMV every time (Stateless).

## 3️⃣ WHY THIS EXISTS
**Ideally:** We have 100 microservices. We don't want every service calling the Database to check if a user is logged in. That kills the database.
**The Fix:** We give the user a JWT. The JWT contains `role: admin`.
**The Benefit:** Service A, B, and C can all trust the token just by doing a math check (Cpu is cheap; Database is expensive).

## 4️⃣ CORE CONCEPTS
- **Stateless:** The server does not store the token.
- **Compact:** Small enough to fit in a header.
- **Structure:** `Header` . `Payload` . `Signature`
- **Claims:** The key-value pairs inside (e.g., `sub: user123`, `exp: 17000000`).

## 5️⃣ VISUAL DIAGRAMS

**JWT Structure:**
```mermaid
graph LR
    H[Header - Red] -->|Base64| JWT
    P[Payload - Purple] -->|Base64| JWT
    S[Signature - Blue] -->|Hash(H+P+Secret)| JWT
    JWT[JWT String: aaaaa.bbbbb.ccccc]
```

**Microservices Trust Flow:**
```mermaid
sequenceDiagram
    participant Client
    participant Auth as Auth Server
    participant ServiceA
    participant ServiceB

    Client->>Auth: Login
    Auth-->>Client: Return JWT (Signed by Private Key)
    
    Client->>ServiceA: Request + JWT
    ServiceA->>ServiceA: Verify Signature (Public Key)
    ServiceA-->>Client: 200 OK (No DB Call!)

    Client->>ServiceB: Request + JWT
    ServiceB->>ServiceB: Verify Signature (Public Key)
    ServiceB-->>Client: 200 OK (No DB Call!)
```

## 6️⃣ SIMPLE EXAMPLE (BEGINNER)

**The Token string looks like this:**
`eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ`

**Decoded (What's inside):**
**Header:** `{ "alg": "HS256", "typ": "JWT" }`
**Payload:** `{ "sub": "john", "admin": true, "iat": 1516239022 }`
**Signature:** `HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)`

## 7️⃣ DEEP DIVE (ADVANCED)

### JWT vs Opaque Tokens (The Architect's Dilemma)

| Feature | JWT (Stateless) | Opaque (Reference) |
| :--- | :--- | :--- |
| **Format** | JSON Object (Readable) | Random String (UUID) |
| **Size** | Large (contains data) | Tiny (32 chars) |
| **Validation** | Local CPU check (Fast) | DB/Redis Lookup (Slower) |
| **Revocation** | **Hard.** (Must wait for expiry) | **Instant.** (Delete from Redis) |
| **Pell-Mell** | Good for Microservices | Good for Monoliths/High-Security |

### Signing Algorithms
- **HS256 (Symmetric):** Example: Password "secret". Fast. Both sender and receiver need the same password. Risk: If Service A leaks the password, it can forge tokens as Auth Server.
- **RS256 (Asymmetric):** Private Key signs, Public Key verifies. **Best for Microservices.** Auth Server keeps Private Key. Service A only has Public Key (can verify, but cannot forge).

## 8️⃣ SPRING & JAVA IMPLEMENTATION

**Dependencies:** `spring-boot-starter-oauth2-resource-server`

**Code (RS256 Verification):**
```java
@Configuration
@EnableWebSecurity
public class JwtConfig {
    
    @Value("${spring.security.oauth2.resourceserver.jwt.public-key-location}")
    RSAPublicKey key;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> 
                jwt.decoder(NimbusJwtDecoder.withPublicKey(key).build())
            ));
        return http.build();
    }
}
```

## 9️⃣ HANDS-ON LAB (NO COST)

### Lab 1: RSA Key Generation & Rotation

**Goal:** Generate and rotate RSA keys for JWT signing.

**Step 1: Generate Keys (OpenSSL)**
```bash
# Generate 2048-bit private key
openssl genrsa -out private_key.pem 2048

# Extract public key
openssl rsa -in private_key.pem -pubout -out public_key.pem

# Convert to PKCS#8 (for Java)
openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt

# Convert public key to DER (for Java)
openssl rsa -pubin -in public_key.pem -outform DER -out public_key.der
```

**Step 2: Java Key Generation Utility**
```java
public class KeyGenerator {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        
        System.out.println("Private: " + 
            Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
        System.out.println("Public: " + 
            Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
    }
}
```

**Step 3: Rotation Scheduler**
```java
@Component
public class KeyRotationService {
    
    private final Map<String, KeyPair> keyHistory = new ConcurrentHashMap<>();
    private String currentKeyId;
    
    @Scheduled(cron = "0 0 0 1 * ?") // 1st of every month
    public void rotateKeys() {
        String newKeyId = UUID.randomUUID().toString();
        KeyPair newKeyPair = generateKeyPair();
        
        // Add new key
        keyHistory.put(newKeyId, newKeyPair);
        currentKeyId = newKeyId;
        
        // Prune old keys (keep last 3)
        // ...
        
        log.info("Rotated to new key: {}", newKeyId);
    }
}
```

---

### Lab 2: JWKS Endpoint Implementation

**Goal:** Expose public keys via JWKS endpoint for resource servers.

**Step 1: Add Dependencies**
```xml
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
</dependency>
```

**Step 2: Create JWKS Controller**
```java
@RestController
public class JwksController {
    
    private final KeyService keyService;
    
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        List<JWK> jwks = new ArrayList<>();
        
        for (var entry : keyService.getAllKeys().entrySet()) {
            RSAKey jwk = new RSAKey.Builder(
                (RSAPublicKey) entry.getValue().getPublic())
                .keyID(entry.getKey())
                .algorithm(JWSAlgorithm.RS256)
                .use(KeyUse.SIGNATURE)
                .build();
            
            jwks.add(jwk);
        }
        
        return new JWKSet(jwks).toJSONObject();
    }
}
```

**Step 3: Test with curl**
```bash
curl http://localhost:8080/.well-known/jwks.json | jq
```

**Expected Output:**
```json
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "kid": "uuid-1",
      "alg": "RS256",
      "n": "..."
    }
  ]
}
```

---

### Lab 3: JWT Blacklisting with Redis

**Goal:** Implement immediate token revocation capability.

**Step 1: Setup Redis**
```bash
docker run -d --name redis -p 6379:6379 redis:alpine
```

**Step 2: Blacklist Service**
```java
@Service
public class TokenBlacklistService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public void blacklistToken(String token, Duration ttl) {
        String jti = extractJti(token);
        redisTemplate.opsForValue().set(
            "blacklist:" + jti, 
            "revoked", 
            ttl
        );
    }
    
    public boolean isBlacklisted(String token) {
        String jti = extractJti(token);
        return Boolean.TRUE.equals(
            redisTemplate.hasKey("blacklist:" + jti)
        );
    }
}
```

**Step 3: Filter Integration**
```java
@Component
public class JwtBlacklistFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, FilterChain chain) {
        
        String token = extractToken(request);
        if (token != null && blacklistService.isBlacklisted(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                "Token revoked");
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

---

### Lab 4: Custom Claims Validation

**Goal:** Enforce business rules via JWT claims.

**Step 1: Custom Validator**
```java
public class TenantValidator implements OAuth2TokenValidator<Jwt> {
    
    private final String expectedTenant;
    
    public TenantValidator(String expectedTenant) {
        this.expectedTenant = expectedTenant;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        
        if (expectedTenant.equals(tenantId)) {
            return OAuth2TokenValidatorResult.success();
        }
        
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_tenant", 
                "Token belongs to different tenant", null)
        );
    }
}
```

**Step 2: Configure Decoder**
```java
@Bean
public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder
        .withJwkSetUri(jwkSetUri)
        .build();
    
    OAuth2TokenValidator<Jwt> defaultValidators = 
        JwtValidators.createDefaultWithIssuer(issuerUri);
    
    OAuth2TokenValidator<Jwt> tenantValidator = 
        new TenantValidator("tenant-123");
    
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            defaultValidators, tenantValidator)
    );
    
    return decoder;
}
```

---

### Lab 5: Performance Benchmarking

**Goal:** Measure HS256 vs RS256 performance.

**Step 1: Benchmark Code**
```java
public class JwtBenchmark {
    
    @Test
    public void compareAlgorithms() {
        // HS256
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            signHs256();
        }
        long hsTime = System.nanoTime() - start;
        
        // RS256
        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            signRs256();
        }
        long rsTime = System.nanoTime() - start;
        
        System.out.printf("HS256: %.2f ms%n", hsTime / 1_000_000.0);
        System.out.printf("RS256: %.2f ms%n", rsTime / 1_000_000.0);
        System.out.printf("Ratio: %.2fx faster%n", (double)rsTime / hsTime);
    }
}
```

**Typical Results:**
- **HS256**: ~10x faster signing, ~10x faster verification
- **RS256**: Slower but enables key distribution/rotation without sharing secrets
- **ES256**: Compromise (fast verification, slow signing, small keys)

**Lesson:** Use RS256 for public/private key benefits unless latency < 1ms is critical.


## 🔟 INTERVIEW QUESTIONS

**Beginner:**
Q: Can I put the user's password in the JWT?
A: NO! The payload is just Base64 encoded. Anyone can decode it and read the data. It is signed (tamper-proof), not encrypted (hidden).

**Senior:**
Q: How do you handle JWT Revocation?
A: 1. Short Expiry times (5 mins) + Refresh Tokens.
2. Blacklist of JTI (Token IDs) in Redis (but this makes it stateful again).
3. "Logout" in API Gateway invalidates the Refresh Token, so the Access Token dies naturally in 5 mins.

**Architect:**
Q: Why use RS256 over HS256 in Microservices?
A: Key Management. With HS256, every microservice needs the "Private Secret" to verify. If one service is compromised, the attacker can forge tokens for ANY service. RS256 allows services to verify using a Public Key, which is safe to share.

## 1️⃣1️⃣ WHEN TO USE / WHEN NOT TO USE

| Scenario | Decision | Reason |
| :--- | :--- | :--- |
| **Session Mgmt** | ❌ NO | Use server-side sessions for simple web apps. |
| **Microservices** | ✅ YES | Avoids "Chatty" calls to Auth Server. |
| **One-time Links** | ✅ YES | "Password Reset" links are often JWTs. |
| **High Security** | ⚠️ NO | If you need instant ban buttons (e.g., employee fired), Opaque is better. |

## 1️⃣2️⃣ SECURITY ATTACKS & DEFENSE (THREAT MODELING)


### Sequence Diagrams (Architectural Difference)

**A. JWT Flow (Stateless & Fast):**
```mermaid
sequenceDiagram
    participant App
    participant Service
    participant KeyStore (App Memory)

    App->>Service: GET /data + JWT
    Service->>KeyStore: 1. Fetch Public Key (Cached)
    Service->>Service: 2. CPU Math Check (Signature)
    Service->>Service: 3. Check Expiry
    Service-->>App: 200 OK
    Note right of Service: Zero Network Calls!
```

**B. Opaque Token Flow (Stateful & Secure):**
```mermaid
sequenceDiagram
    participant App
    participant Service
    participant Redis/DB

    App->>Service: GET /data + Token (uuid)
    Service->>Redis/DB: 1. Lookup Token ID
    Redis/DB-->>Service: 2. Return User Data (or 404)
    Service-->>App: 200 OK
    Note right of Service: 1 Network Call per Request
```

## 1️⃣2️⃣ SECURITY ATTACKS & DEFENSE (THREAT MODELING)

**STRIDE Analysis for JWT:**

| Threat | Attack Vector | Impact | Mitigation |
| :--- | :--- | :--- | :--- |
| **Tampering** | **Signature Stripping.** Attacker changes `alg` header to `none`. | Signature Bypass. Attacker forges any claim. | **Disable `Null`/`None` algorithms** in library. Enforce `RS256`. |
| **Information Disclosure** | PII in Payload. "social_security_number": "123". | Data Leakage. Frontend can read this. | **Minimal Claims.** Only put `sub` (User ID) and `scope`. |
| **Spoofing** | **Key Confusion.** Attacker signs with Public Key but tells server it's HMAC. | Forgery. | Explicitly check `alg` header matches expected algorithm. |
| **Repudiation** | Token theft without logs. | Impossible to trace who used it. | Log `jti` (Token ID) for key transactions. |

## 1️⃣3️⃣ MIND MAP

*   **JWT**
    *   **Structure**: Header.Payload.Signature
    *   **Benefit**: Stateless Trust
    *   **Crypto**: HS256 vs RS256
    *   **Risk**: Hard to Revoke

---

### 🔟 ARCHITECT DECISION NOTES
> **Implementation**: **JWT (JSON Web Keys)**
>
> **The Verdict**: **The Engine of Modern Auth.**
>
> **Reasoning**: JWT is the de-facto standard for propagating identity across distributed systems.
>
> **Critical Warning**: Do **NOT** use JWTs for session management in a monolithic generic webapp (JSP/Thymeleaf). Cookies + Sessions are more secure (smaller, strictly HttpOnly, revocable).
>
> **Best Practice**: Use **Short-Lived Access Tokens** (5-15 mins) and **Long-Lived Refresh Tokens**. This balances the statelessness of JWT with the ability to revoke access (by killing the Refresh Token).

---

# 5. Single Sign-On (SSO)

## 1️⃣ ELI5 (Explain Like I’m 5)
**The All-Inclusive Wristband:**
Imagine a theme park. It has 50 rides.
**Without SSO:** You have to buy a ticket at the gate of *every single ride*. You carry 50 tickets.
**With SSO:** You buy one wristband at the main entrance. The guard at Ride #1 checks the wristband. The guard at Ride #45 checks the *same* wristband. You only paid (logged in) once.

## 2️⃣ REAL-LIFE ANALOGY
**Office Badge:**
- You swipe your badge to enter the building (Login).
- You use the same badge to open the elevator.
- You use the same badge to open the gym.
- You use the same badge to open the cafeteria.
- The system knows "This is Employee #502" everywhere.

## 3️⃣ WHY THIS EXISTS
**Ideally:** Users hate passwords. If we ask them to create a password for Slack, another for Jira, another for Email, they will set them all to "password123".
**The Fix:** Centralize the login. One strong password verifies you for *everything*.
**The Benefit:** Security + Convenience. If an employee leaves, you disable **one** account, and they lose access to everything instantly.

## 4️⃣ CORE CONCEPTS
- **IdP (Identity Provider):** The "Source of Truth" (Okta, Keycloak, Active Directory).
- **SP (Service Provider):** The App (Slack, Zoom, Your Spring App).
- **Federation:** Establishing trust between the SP and the IdP.
- **Protocols:**
    - **SAML:** (Legacy/Enterprise) XML-based. Heavy but powerful.
    - **OIDC (OpenID Connect):** (Modern) JSON-based layer on top of OAuth 2.0.

## 5️⃣ VISUAL DIAGRAMS

**OIDC Login Flow:**

```mermaid
sequenceDiagram
    participant User
    participant App A (SP)
    participant App B (SP)
    participant IdP (Okta)

    User->>App A: "Login"
    App A->>IdP: Redirect to IdP
    IdP->>User: "Enter Password" (On IdP Domain)
    User->>IdP: Credentials
    IdP->>IdP: Set "Session Cookie"
    IdP-->>App A: Return Token
    App A->>User: Welcome Alice!

    Note right of User: User opens App B later...

    User->>App B: "Login"
    App B->>IdP: Redirect to IdP
    IdP->>IdP: Detects "Session Cookie" (Already logged in!)
    IdP-->>App B: Return Token (INSTANTLY)
    App B->>User: Welcome Alice! (No password asked)
```

## 6️⃣ SIMPLE EXAMPLE (BEGINNER)

**Standard Workflow:**
1.  User visits `yourapp.com`.
2.  App sees no session -> Redirects to `google.com/auth`.
3.  User signs in to Google.
4.  Google redirects back to `yourapp.com/callback?code=123`.
5.  App exchanges code for ID Token (JWT).
6.  App reads ID Token: "email: swiftie@gmail.com".
7.  App creates a local session for "swiftie@gmail.com".

## 7️⃣ DEEP DIVE (ADVANCED)

### SAML vs OIDC (The Architect's Choice)

| Feature | SAML (Security Assertion Markup Language) | OIDC (OpenID Connect) |
| :--- | :--- | :--- |
| **Format** | XML (Verbose) | JSON (Compact) |
| **Transport** | Browser Redirection (POST Binding) | REST/HTTP Friendly |
| **Usage** | Enterprise / Government / Legacy | Modern Web / Mobile / SPAs |
| **Complexity** | High (Signing XML is painful) | Low (Standard JWT libraries) |
| **Mobile** | Poor support | Native support (AppAuth) |

### SLO (Single Logout)
The hardest part of SSO is LOGOUT.
If I sign out of App A, should I generally be signed out of App B?
- **Front-Channel Logout:** Browser loads invisible iframes to kill cookies on IdP + App B. (Unreliable).
- **Back-Channel Logout:** IdP sends a POST request to App B server: "Kill Session for User X". (More reliable).

## 8️⃣ SPRING & JAVA IMPLEMENTATION

**Spring Boot 3 + OIDC:**

```java
// Spring Security defaults handle OIDC Discovery automatically!
// Just add this to application.yml

spring:
  security:
    oauth2:
      client:
        registration:
          okta:
            client-id: ...
            client-secret: ...
            scope: openid, profile, email
        provider:
          okta:
            issuer-uri: https://dev-123456.oktapreview.com
```

**What happens magically:**
Spring calls `<issuer-uri>/.well-known/openid-configuration`, finds the authorization endpoint, token endpoint, and keys URI, and configures everything.

## 9️⃣ HANDS-ON LAB (NO COST)

### Lab 1: Keycloak Docker Setup

**Goal:** Run a self-hosted IdP (Identity Provider) locally.

**Step 1: Docker Compose**
```yaml
version: '3.8'
services:
  keycloak:
    image: quay.io/keycloak/keycloak:23.0.0
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8080:8080"
```

**Step 2: Initialize Realm**
1. Access `http://localhost:8080` (admin/admin)
2. Create Realm: `spring-realm`
3. Create Client: `spring-client`
   - Client authentication: `On`
   - Valid redirect URIs: `http://localhost:8081/login/oauth2/code/keycloak`
4. Create User: `user1` (set password to `password`)

**Step 3: Spring Boot Config**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: spring-client
            client-secret: <CLIENT_SECRET_FROM_KEYCLOAK>
            scope: openid,profile,email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          keycloak:
            issuer-uri: http://localhost:8080/realms/spring-realm
```

---

### Lab 2: SAML Integration

**Goal:** Connect Spring Boot to a SAML IdP.

**Step 1: Add Dependency**
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-saml2-service-provider</artifactId>
</dependency>
```

**Step 2: Configuration**
```yaml
spring:
  security:
    saml2:
      relyingparty:
        registration:
          okta:
            assertingparty:
              metadata-uri: https://dev-123456.oktapreview.com/app/exk123/sso/saml/metadata
            signing:
              credentials:
                - private-key-location: classpath:private.key
                  certificate-location: classpath:public.cer
```

**Step 3: Security Config**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        )
        .saml2Login(Customizer.withDefaults())
        .saml2Logout(Customizer.withDefaults());
    return http.build();
}
```

---

### Lab 3: Single Logout (SLO)

**Goal:** Implement Back-Channel Logout.

**Step 1: Enable Back-Channel Logout in Keycloak**
- In Client Settings:
  - Backchannel Logout URL: `http://localhost:8081/logout/connect/back-channel/keycloak`
  - Backchannel Logout Session Required: `On`

**Step 2: Spring Security Config**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .oauth2Login(Customizer.withDefaults())
        .logout(logout -> logout
            .logoutSuccessUrl("/")
        )
        .oidcLogout(logout -> logout
            .backChannel(Customizer.withDefaults())
        );
    return http.build();
}
```

**Step 3: Verify**
1. Login to App A.
2. Login to App B (auto-login via SSO).
3. Logout from App A.
4. Refresh App B page -> Should remain logged in (session sync delay).
5. Wait 1 min or trigger Keycloak logout -> App B session killed.

---

### Lab 4: Multi-Tenant SSO

**Goal:** Support multiple organizations with different IdPs.

**Step 1: Dynamic Registration Repository**
```java
@Bean
public ClientRegistrationRepository clientRegistrationRepository() {
    return new InMemoryClientRegistrationRepository(
        // Client 1 (Tenant A)
        ClientRegistration.withRegistrationId("tenant-a")
            .clientId("client-a")
            .issuerUri("https://tenant-a.com")
            .build(),
        // Client 2 (Tenant B)
        ClientRegistration.withRegistrationId("tenant-b")
            .clientId("client-b")
            .issuerUri("https://tenant-b.com")
            .build()
    );
}
```

**Step 2: Tenant Resolver**
```java
@GetMapping("/login/{tenantId}")
public String login(@PathVariable String tenantId) {
    return "redirect:/oauth2/authorization/" + tenantId;
}
```

---

### Lab 5: Custom Login Flow

**Goal:** Brand the login experience while keeping SSO.

**Step 1: Custom Login Page**
```html
<!-- login.html -->
<div class="login-container">
    <h1>Welcome to Corp App</h1>
    <a href="/oauth2/authorization/keycloak" class="btn btn-keycloak">
        Login with Keycloak
    </a>
    <a href="/oauth2/authorization/google" class="btn btn-google">
        Login with Google
    </a>
</div>
```

**Step 2: Security Config**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/css/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/login") // Custom page
        );
    return http.build();
}
```

## 🔟 INTERVIEW QUESTIONS

**Beginner:**
Q: What is the IdP?
A: Identity Provider. The centralized server that holds the user database and checks passwords (e.g., Google, Okta).

**Senior:**
Q: Difference between OAuth2 and OIDC?
A: OAuth2 is for **Authorization** (Accessing APIs). OIDC is for **Authentication** (Knowing who the user is). OIDC adds an "ID Token" to the standard OAuth flow.

**Architect:**
Q: Why is Single Logout (SLO) a nightmare?
A: Because distributed sessions are hard to synchronize. If the network fails during Back-Channel logout, a user might remain logged in to "App B" after logging out of "App A", creating a security risk on shared computers.

## 1️⃣1️⃣ WHEN TO USE / WHEN NOT TO USE

| Scenario | Decision | Reason |
| :--- | :--- | :--- |
| **Enterprise B2B** | ✅ YES | Companies demand SSO (SAML/OIDC) to manage employees. |
| **Consumer App** | ✅ YES | "Social Login" (FB/Google) increases conversion rates. |
| **Internal Tools** | ✅ YES | Centralized access control. |
| **The ONLY app** | ❌ NO | If you are building a standalone offline tool, simple DB auth is fine. |

## 1️⃣2️⃣ SECURITY ATTACKS & DEFENSE (THREAT MODELING)


## 1️⃣2️⃣ SECURITY ATTACKS & DEFENSE (THREAT MODELING)

**STRIDE Analysis for SSO:**

| Threat | Attack Vector | Impact | Mitigation |
| :--- | :--- | :--- | :--- |
| **Repudiation** | User claims "I didn't do that". | Lack of accountability. | **Centralized Audit Logging.** Correlate IdP Session ID with SP Access Logs. |
| **Tampering** | **XML Signature Wrapping** (SAML). Attacker moves the signed part of XML to a different location. | Auth Bypass. Attacker logs in as Admin. | Use modern, patched libraries (Spring Security SAML 2). **Never** write custom XML parsers. |
| **Information Disclosure** | **Token Leakage in Browser History.** OAuth fragments (`#access_token=...`) stay in history. | Account Takeover. | Use `response_mode=form_post` to send tokens via POST body, not URL. |
| **Denial of Service** | **Blowing up XML Parsing** (XXE). Sending 1GB XML file. | Server Crash. | Disable External Entity Resolution (XXE) in XML parsers. |

## 1️⃣3️⃣ MIND MAP

*   **SSO**
    *   **Architecture**: Central IdP, Many SPs
    *   **Protocols**: OIDC (Modern), SAML (Legacy)
    *   **Tokens**: ID Token (Who), Access Token (Access)
    *   **Benefit**: reduced attack surface (1 password)

---

### 🔟 ARCHITECT DECISION NOTES
> **Implementation**: **Single Sign-On (SSO)**
>
> **The Verdict**: **The only viable option for Enterprise.**
>
> **Reasoning**: In any company with >50 employees, managing individual logins is a security breach waiting to happen.
>
> **Strategy**:
> *   For Consumers: Support Google/Apple/Facebook login.
> *   For B2B: Support SAML/OIDC to let your customers bring their own Identity (BYOID).
> *   **Do not build an IdP.** Use SaaS (Auth0, Cognito, Okta) or Keycloak (Self-hosted). Building a secure IdP is harder than building your actual product.

---

# 6. Advanced Architecture & Production Standards

> *Mandatory knowledge for Senior/Staff Engineers.*

## 6.1 Authentication vs Authorization (Who vs What)

| Feature | Authentication (AuthN) | Authorization (AuthZ) |
| :--- | :--- | :--- |
| **Question** | "Who are you?" | "What can you do?" |
| **Example** | Login with Password, Fingerprint | "Read Files", "Delete Users" |
| **Protocol** | OIDC, SAML, WebAuthn | OAuth 2.0 scopes, RBAC, ABAC |
| **Failure** | 401 Unauthorized | 403 Forbidden |

### Authorization Models
1.  **RBAC (Role-Based):** Simple. `ROLE_ADMIN` can do everything. `ROLE_USER` can read.
2.  **ABAC (Attribute-Based):** Fine-grained. "Can view document IF `doc.department == user.department` AND `time < 5PM`." Uses **OPA (Open Policy Agent)**.
3.  **LACM (Least Privilege):** Default deny. Explicitly grant only what is needed.

**Spring Security Method Security:**
```java
// Controller
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser() { ... }

// Advanced (SpEL)
@PreAuthorize("#contact.name == authentication.name")
public void editMyProfile(Contact contact) { ... }
```

## 6.2 Microservices, API Gateway & Zero Trust

**The Pattern:**
1.  **Edge (API Gateway):** The "Border Patrol". Terminates SSL, validates Opaque Token (if used), Rate Limits.
2.  **Internal (Microservices):** NO Security? **WRONG.**
    *   **Old Way:** Trusted Intranet (Perimeter Security). If one service is hacked, they all are.
    *   **Zero Trust (Modern):** "Never Trust, Always Verify." Service A must authenticate to Service B (using mTLS or JWT).

**Token Propagation Flow:**
```mermaid
graph LR
    User -->|Bearer JWT| Gateway
    Gateway -->|Forward JWT| ServiceA
    ServiceA -->|Forward JWT| ServiceB
    ServiceB -->|Verify Signature| ServiceB
```


**Spring Cloud Gateway Example (Token Relay):**
```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("user-service", r -> r.path("/users/**")
            .filters(f -> f.tokenRelay()) // Forwards the JWT to downstream service
            .uri("lb://user-service"))
        .build();
}
```

## 6.3 Security Testing

**Anti-Pattern:** Using `Mockito` to jump over security filters.
**Best Practice:** Integration testing with `MockMvc`.

```java
@Test
@WithMockUser(username = "admin", roles = {"ADMIN"}) // Fake Auth Context
void deleteUser_ShouldSucceed_WhenAdmin() {
    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isOk());
}

@Test
@WithAnonymousUser
void deleteUser_ShouldFail_WhenAnon() {
    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isUnauthorized());
}
```

**⚠️ What NOT to test:**
1.  **Do not unit test Spring Security internals.** Asssume `BCryptPasswordEncoder` works. Don't write a test to check if it hashes correctly.
2.  **Do not test OAuth 2.0 redirects.** Trust the library. Test *your* configuration, not the framework.
3.  **Do not Mock `SecurityContextHolder` manually.** Use `@WithMockUser`.

## 6.4 Production Failures & Misconfigurations

**How systems ACTUALLY get hacked:**
1.  **Tokens in LocalStorage:** JS on the page (XSS) reads the token and sends it to the attacker. -> **Use HttpOnly Cookies.**
2.  **Weak Signing Keys:** Using "secret" or "123456" for HMAC signing. -> **Use automated secret injection (Vault/AWS Secrets).**
3.  **Broken Object Level Auth (BOLA):** User A changes URL ID `?id=UserB` and sees User B's data. -> **Always check ownership.**
4.  **Disabled CSRF:** "It was blocking my API calls so I disabled it." -> **Only disable for Stateless (JWT) APIs, never for Session/Cookie APIs.**

## 6.5 Migration & Legacy Modernization

**Migration Strategy: The Strangler Fig Pattern**

1.  **Current State:** Monolith using JSESSIONID.
2.  **Phase 1:** Introduce Auth0/Keycloak. Point the Monolith to valid OIDC tokens *in parallel* with JSESSIONID.
3.  **Phase 2:** Move "User Profile" code to a Microservice.
4.  **Phase 3:** API Gateway checks the Token. Monolith just assumes trust from Gateway.
5.  **Final State:** Kill the Login Form code in the Monolith.

## 6.6 Logging, Auditing & Compliance

**The Golden Rule:** Log *Who*, *When*, and *What*. **NEVER log Credentials or Tokens.**

**Bad Log:** `User login failed with password: hunter2` (FIRED IMMEDIATELY).
**Good Log:** `Login failed for user: alice [IP: 192.168.1.5] - Reason: Bad Creds`.

**Compliance:**
- **SOC2 / ISO 27001:** Requires proof that you review access logs.
- **GDPR:** "Right to be Forgotten". If you delete a user, can you delete their audit trails? (Usually No - Audit logs are exempt, but check legal).


## 6.7 Standards vs Implementations

**An architect must know the difference between a SPECFICIATION and a PRODUCT.**

| Concept | Type | Definition |
| :--- | :--- | :--- |
| **OAuth 2.0** | Specification | The *Framework* for delegation ("Valet Key"). |
| **OpenID Connect (OIDC)** | Specification | The *Identity Layer* on top of OAuth 2.0 ("Id Card"). |
| **JWT (JSON Web Token)** | Format | A way to structure data (Header.Payload.Signature). |
| **Keycloak** | Product | An *Implementation* of OAuth2, OIDC, and SAML. |
| **Auth0 / Okta** | Product | Check-book implementations (SaaS) of these standards. |

**Common Misconception:** "We are using JWT instead of OAuth."
**Correction:** You are likely using *OAuth* to get a *JWT*. They are not mutually exclusive. JWT is the *format* of the ticket; OAuth is the *process* of buying it.



# 7. Production Failures & Misconfigurations

> **Critical Reading for Senior Engineers & Architects**
> 
> This section documents real-world security failures, common misconfigurations, and lessons learned from production incidents.

## 7.1 Real-World Breach Case Studies

### Case Study 1: GitHub OAuth Token Leak (2022)

**What Happened:**
- Attacker gained access to OAuth tokens stored in plaintext logs
- Tokens had excessive scopes (`repo`, `admin:org`)
- No token rotation policy
- Tokens valid for years

**Root Cause:**
```java
// ❌ WRONG: Logging tokens
log.info("User logged in with token: {}", accessToken);

// ✅ CORRECT: Never log tokens
log.info("User {} logged in successfully", userId);
```

**Lessons Learned:**
1. Never log sensitive data
2. Implement token rotation
3. Use short-lived access tokens (15-30 min)
4. Audit token scopes regularly
5. Monitor token usage

### Case Study 2: JWT Secret Key Exposure (2021)

**What Happened:**
- JWT secret committed to GitHub
- Weak secret: `"secret123"`
- HS256 algorithm (symmetric)
- No key rotation

**Impact:**
- Attackers forged any JWT
- Complete authentication bypass
- Privilege escalation

**Fix:**
```java
// ✅ CORRECT: Use environment variables + strong secrets
@Value("${JWT_SECRET}")
private String jwtSecret; // From environment

// ✅ CORRECT: Prefer RS256 (asymmetric)
@Bean
public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(
        "https://auth-server.com/oauth2/jwks"
    ).build();
}
```

**Lessons Learned:**
1. Never commit secrets to Git
2. Use strong secrets (256 bits minimum)
3. Prefer RS256 over HS256
4. Implement key rotation
5. Use secret management tools (Vault, AWS Secrets Manager)

## 7.2 Common Misconfigurations

### 1. Tokens in localStorage
```javascript
// ❌ WRONG: Vulnerable to XSS
localStorage.setItem('access_token', token);

// ✅ CORRECT: httpOnly cookies
ResponseCookie cookie = ResponseCookie.from("access_token", token)
    .httpOnly(true)
    .secure(true)
    .sameSite("Strict")
    .build();
```

### 2. Over-Permissive Scopes
```java
// ❌ WRONG: Granting all scopes
.scope("admin")  // Mobile app doesn't need this!

// ✅ CORRECT: Minimal scopes
.scope("read")
.scope("write")
// Only what's needed
```

### 3. Disabled CSRF
```java
// ❌ WRONG: Disabling without understanding
http.csrf(csrf -> csrf.disable());

// ✅ CORRECT: Disable only for stateless APIs
http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    );
```

### 4. Long-Lived Tokens
```java
// ❌ WRONG: 1 year tokens
.accessTokenTimeToLive(Duration.ofDays(365))

// ✅ CORRECT: Short-lived
.accessTokenTimeToLive(Duration.ofMinutes(15))
.refreshTokenTimeToLive(Duration.ofDays(7))
.reuseRefreshTokens(false) // Rotate
```

### 5. Missing HTTPS
```java
// ✅ CORRECT: Force HTTPS
http.requiresChannel(channel -> channel
    .anyRequest().requiresSecure()
);
```

## 7.3 Prevention Checklist

**Pre-Production:**
- [ ] No secrets in code/Git
- [ ] Access tokens ≤ 30 minutes
- [ ] Refresh token rotation enabled
- [ ] HTTPS enforced
- [ ] CORS configured (no wildcards)
- [ ] CSRF enabled for session-based apps
- [ ] No sensitive data in logs
- [ ] Principle of least privilege (scopes)

## 7.4 Incident Response Playbook

**Phase 1: Detection (0-15 min)**
1. Confirm incident
2. Assess severity
3. Activate response team

**Phase 2: Containment (15-60 min)**
```bash
# Revoke compromised tokens
curl -X POST /admin/revoke-user-tokens -d "username=user"

# Rotate secrets
./rotate-jwt-keys.sh
```

**Phase 3: Eradication (1-4 hours)**
1. Identify root cause
2. Patch vulnerability
3. Deploy fix

**Phase 4: Recovery (4-24 hours)**
1. Restore operations
2. Monitor for recurrence

**Phase 5: Post-Incident (1-2 weeks)**
1. Post-mortem
2. Update policies
3. Team training

---

# 8. Migration & Legacy Modernization

> **For Architects & Senior Engineers**
> 
> Real-world migration strategies for modernizing authentication and authorization in existing systems.

## 8.1 Basic Auth → OAuth 2.0 Migration

### Phase 1: Assessment (Week 1-2)

**Inventory Current State:**
```bash
# Find all Basic Auth endpoints
grep -r "httpBasic" src/
grep -r "Authorization: Basic" src/

# Identify clients
# - Mobile apps
# - Web apps
# - Third-party integrations
# - Internal services
```

**Risk Assessment:**
- Breaking changes for existing clients
- Downtime during migration
- Token storage requirements
- Client update coordination

### Phase 2: Dual-Mode Operation (Week 3-8)

**Support Both Authentication Methods:**
```java
@Configuration
@EnableWebSecurity
public class DualAuthSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/**").authenticated()  // Old API
                .requestMatchers("/api/v2/**").authenticated()  // New API
                .anyRequest().permitAll()
            )
            // Support both Basic Auth and OAuth 2.0
            .httpBasic(Customizer.withDefaults())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        
        return http.build();
    }
}
```

**Version-Based Routing:**
```java
@RestController
public class UserController {
    
    // Old endpoint (Basic Auth)
    @GetMapping("/api/v1/user/profile")
    public UserProfile getProfileV1(Authentication auth) {
        // Works with Basic Auth
        return userService.getProfile(auth.getName());
    }
    
    // New endpoint (OAuth 2.0)
    @GetMapping("/api/v2/user/profile")
    @PreAuthorize("hasAuthority('SCOPE_profile')")
    public UserProfile getProfileV2(Authentication auth) {
        // Works with JWT
        return userService.getProfile(auth.getName());
    }
}
```

### Phase 3: Client Migration (Week 9-16)

**Gradual Rollout:**
```yaml
# Feature flag configuration
features:
  oauth-migration:
    enabled: true
    rollout-percentage: 25  # Start with 25% of users
```

**Migration Tracking:**
```java
@Component
@Slf4j
public class AuthMethodTracker {
    
    @EventListener
    public void onAuthentication(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String method = determineAuthMethod(auth);
        
        // Track usage
        metricsService.incrementCounter("auth.method." + method);
        
        log.info("User {} authenticated via {}", 
            auth.getName(), method);
    }
    
    private String determineAuthMethod(Authentication auth) {
        if (auth instanceof UsernamePasswordAuthenticationToken) {
            return "basic";
        } else if (auth instanceof JwtAuthenticationToken) {
            return "oauth2";
        }
        return "unknown";
    }
}
```

### Phase 4: Deprecation (Week 17-20)

**Deprecation Warnings:**
```java
@GetMapping("/api/v1/user/profile")
@Deprecated
public ResponseEntity<UserProfile> getProfileV1(
        Authentication auth,
        HttpServletResponse response) {
    
    // Add deprecation header
    response.setHeader("X-API-Deprecated", "true");
    response.setHeader("X-API-Sunset", "2024-12-31");
    response.setHeader("X-API-Migration-Guide", 
        "https://docs.example.com/oauth-migration");
    
    return ResponseEntity.ok(userService.getProfile(auth.getName()));
}
```

### Phase 5: Cutover (Week 21+)

**Remove Basic Auth:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        )
        // Only OAuth 2.0 now
        .oauth2ResourceServer(oauth2 -> oauth2.jwt());
    
    return http.build();
}
```

**Rollback Plan:**
```bash
# If issues arise, rollback to dual-mode
kubectl rollout undo deployment/api-server

# Or use feature flag
curl -X POST /admin/feature-flags \
  -d "oauth-migration.enabled=false"
```

---

## 8.2 Session-Based → JWT Migration

### Challenge: Hybrid Approach

**Support Both Sessions and JWTs:**
```java
@Configuration
public class HybridAuthConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            // Session-based for web UI
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
            )
            // JWT for API
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt()
            );
        
        return http.build();
    }
}
```

**Generate JWT from Session:**
```java
@RestController
@RequestMapping("/api/auth")
public class TokenGenerationController {
    
    private final TokenService tokenService;
    
    @PostMapping("/session-to-jwt")
    public TokenResponse convertSessionToJwt(HttpSession session) {
        // Extract user from session
        Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("No active session");
        }
        
        // Generate JWT
        String jwt = tokenService.generateJwt(auth.getName());
        
        return new TokenResponse(jwt, "Bearer", 3600);
    }
}
```

**Client-Side Migration:**
```javascript
// Old: Session-based
fetch('/api/data', {
    credentials: 'include'  // Send session cookie
});

// New: JWT-based
const response = await fetch('/api/auth/session-to-jwt', {
    method: 'POST',
    credentials: 'include'
});
const { access_token } = await response.json();

// Store JWT
localStorage.setItem('jwt', access_token);

// Use JWT for subsequent requests
fetch('/api/data', {
    headers: {
        'Authorization': `Bearer ${access_token}`
    }
});
```

---

## 8.3 Monolith → Microservices Auth

### Pattern 1: API Gateway Authentication

**Architecture:**
```
┌─────────┐      ┌──────────────┐      ┌────────────┐
│ Client  │─────▶│  API Gateway │─────▶│ Service A  │
└─────────┘      │  (Auth Here) │      │ (No Auth)  │
                 └──────────────┘      └────────────┘
                        │
                        ▼
                 ┌────────────┐
                 │ Service B  │
                 │ (No Auth)  │
                 └────────────┘
```

**Spring Cloud Gateway:**
```java
@Configuration
public class GatewaySecurityConfig {
    
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http) {
        
        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/public/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt()
            );
        
        return http.build();
    }
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("service-a", r -> r
                .path("/api/service-a/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-User-Id", "#{principal.name}")
                )
                .uri("http://service-a:8080")
            )
            .build();
    }
}
```

### Pattern 2: Service-to-Service JWT Propagation

**Gateway Adds JWT:**
```java
@Component
public class JwtPropagationFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, 
                             GatewayFilterChain chain) {
        
        return exchange.getPrincipal()
            .flatMap(principal -> {
                // Extract JWT from authentication
                String jwt = extractJwt(principal);
                
                // Add to downstream request
                ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .header("Authorization", "Bearer " + jwt)
                    .build();
                
                return chain.filter(exchange.mutate()
                    .request(request)
                    .build());
            });
    }
}
```

**Services Validate JWT:**
```java
// Each microservice validates JWT
@Configuration
public class ServiceSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwkSetUri("http://auth-server:9000/oauth2/jwks")
                )
            );
        
        return http.build();
    }
}
```

---

## 8.4 Risk Mitigation Strategies

### 1. Feature Flags

```java
@Configuration
public class FeatureFlagConfig {
    
    @Value("${features.oauth-enabled:false}")
    private boolean oauthEnabled;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        if (oauthEnabled) {
            // New OAuth 2.0 configuration
            return configureOAuth(http);
        } else {
            // Old Basic Auth configuration
            return configureBasicAuth(http);
        }
    }
}
```

### 2. Canary Deployments

```yaml
# Kubernetes deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-server-oauth
spec:
  replicas: 2  # Start with 2 pods (10% of traffic)
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-server-basic
spec:
  replicas: 18  # 90% of traffic
```

### 3. Monitoring & Alerts

```java
@Component
public class MigrationMonitor {
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkMigrationHealth() {
        long basicAuthCount = metricsService.getCounter("auth.basic");
        long oauthCount = metricsService.getCounter("auth.oauth");
        
        double oauthPercentage = (double) oauthCount / 
            (basicAuthCount + oauthCount) * 100;
        
        log.info("OAuth adoption: {}%", oauthPercentage);
        
        // Alert if OAuth errors spike
        long oauthErrors = metricsService.getCounter("auth.oauth.errors");
        if (oauthErrors > 100) {
            alertService.send("OAuth errors spiking: " + oauthErrors);
        }
    }
}
```

---

# 9. Logging, Auditing & Compliance

> **Enterprise Requirements**
> 
> Comprehensive logging and auditing for security events, compliance, and forensics.

## 9.1 Authentication Event Logging

### Structured Logging with JSON

```java
@Component
@Slf4j
public class AuthenticationEventLogger {
    
    private final ObjectMapper objectMapper;
    
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        
        Map<String, Object> logData = Map.of(
            "event", "authentication_success",
            "timestamp", Instant.now(),
            "username", auth.getName(),
            "ip_address", getClientIp(),
            "user_agent", getUserAgent(),
            "auth_method", getAuthMethod(auth),
            "session_id", getSessionId()
        );
        
        log.info("{}", toJson(logData));
    }
    
    @EventListener
    public void onAuthenticationFailure(
            AbstractAuthenticationFailureEvent event) {
        
        Map<String, Object> logData = Map.of(
            "event", "authentication_failure",
            "timestamp", Instant.now(),
            "username", event.getAuthentication().getName(),
            "ip_address", getClientIp(),
            "reason", event.getException().getMessage(),
            "auth_method", getAuthMethod(event.getAuthentication())
        );
        
        log.warn("{}", toJson(logData));
    }
    
    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return data.toString();
        }
    }
}
```

### Logback Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON_FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/security-audit.json</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>username</includeMdcKeyName>
            <includeMdcKeyName>ip_address</includeMdcKeyName>
            <includeMdcKeyName>session_id</includeMdcKeyName>
        </encoder>
    </appender>
    
    <logger name="com.example.security" level="INFO">
        <appender-ref ref="JSON_FILE"/>
    </logger>
</configuration>
```

## 9.2 Authorization Decision Logging

```java
@Aspect
@Component
@Slf4j
public class AuthorizationAuditAspect {
    
    @Around("@annotation(preAuthorize)")
    public Object auditAuthorization(
            ProceedingJoinPoint joinPoint,
            PreAuthorize preAuthorize) throws Throwable {
        
        Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        String username = auth != null ? auth.getName() : "anonymous";
        String method = joinPoint.getSignature().toShortString();
        String expression = preAuthorize.value();
        
        try {
            Object result = joinPoint.proceed();
            
            // Log successful authorization
            Map<String, Object> logData = Map.of(
                "event", "authorization_granted",
                "timestamp", Instant.now(),
                "username", username,
                "method", method,
                "expression", expression,
                "granted", true
            );
            
            log.info("{}", toJson(logData));
            
            return result;
            
        } catch (AccessDeniedException e) {
            // Log authorization failure
            Map<String, Object> logData = Map.of(
                "event", "authorization_denied",
                "timestamp", Instant.now(),
                "username", username,
                "method", method,
                "expression", expression,
                "granted", false,
                "reason", e.getMessage()
            );
            
            log.warn("{}", toJson(logData));
            
            throw e;
        }
    }
}
```

## 9.3 Token Lifecycle Logging

```java
@Service
@Slf4j
public class AuditableTokenService extends TokenService {
    
    @Override
    public OpaqueToken generateAccessToken(String username, Set<String> scopes) {
        OpaqueToken token = super.generateAccessToken(username, scopes);
        
        Map<String, Object> logData = Map.of(
            "event", "token_created",
            "timestamp", Instant.now(),
            "username", username,
            "token_id", token.getTokenValue().substring(0, 8) + "...",
            "scopes", scopes,
            "expires_at", token.getExpiresAt()
        );
        
        log.info("{}", toJson(logData));
        
        return token;
    }
    
    @Override
    public void revokeToken(String tokenValue) {
        super.revokeToken(tokenValue);
        
        Map<String, Object> logData = Map.of(
            "event", "token_revoked",
            "timestamp", Instant.now(),
            "token_id", tokenValue.substring(0, 8) + "..."
        );
        
        log.info("{}", toJson(logData));
    }
}
```

## 9.4 Compliance Considerations

### GDPR (General Data Protection Regulation)

**Data Minimization:**
```java
// ❌ WRONG: Logging PII
log.info("User email: {}, phone: {}", email, phone);

// ✅ CORRECT: Log only identifiers
log.info("User ID: {}", userId);
```

**Right to be Forgotten:**
```java
@Service
public class UserDeletionService {
    
    public void deleteUser(String userId) {
        // Delete user data
        userRepository.deleteById(userId);
        
        // Revoke all tokens
        tokenService.revokeAllUserTokens(userId);
        
        // Anonymize audit logs
        auditLogRepository.anonymizeLogsForUser(userId);
        
        log.info("User {} deleted and anonymized", userId);
    }
}
```

### SOC 2 (System and Organization Controls)

**Access Control Logging:**
- Log all authentication attempts
- Log all authorization decisions
- Log all administrative actions
- Retain logs for 1 year minimum

**Audit Trail Requirements:**
```java
@Entity
public class AuditLog {
    @Id
    private String id;
    
    private Instant timestamp;
    private String eventType;
    private String username;
    private String ipAddress;
    private String action;
    private String resource;
    private boolean granted;
    
    // Immutable - never update, only insert
}
```

### PCI DSS (Payment Card Industry)

**Requirements:**
- Log all access to cardholder data
- Secure log storage
- Daily log review
- 90-day retention minimum

```java
@PreAuthorize("hasRole('PCI_ADMIN')")
public void accessCardholderData(String cardId) {
    Map<String, Object> logData = Map.of(
        "event", "pci_data_access",
        "timestamp", Instant.now(),
        "username", getCurrentUsername(),
        "resource", "cardholder_data",
        "card_id_hash", hashCardId(cardId)
    );
    
    log.info("{}", toJson(logData));
}
```

## 9.5 Log Aggregation & Analysis

### ELK Stack Integration

```yaml
# docker-compose.yml
services:
  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
    ports:
      - "9200:9200"
  
  logstash:
    image: logstash:8.11.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    depends_on:
      - elasticsearch
  
  kibana:
    image: kibana:8.11.0
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch
```

### Alerting Rules

```java
@Component
@Scheduled(fixedRate = 300000) // Every 5 minutes
public class SecurityAlertMonitor {
    
    public void checkForAnomalies() {
        // Check for brute force attacks
        long failedLogins = countFailedLoginsLastHour();
        if (failedLogins > 100) {
            alertService.send("Possible brute force attack: " + 
                failedLogins + " failed logins");
        }
        
        // Check for privilege escalation attempts
        long deniedAdminAccess = countDeniedAdminAccessLastHour();
        if (deniedAdminAccess > 50) {
            alertService.send("Possible privilege escalation: " + 
                deniedAdminAccess + " denied admin access attempts");
        }
    }
}
```

---
