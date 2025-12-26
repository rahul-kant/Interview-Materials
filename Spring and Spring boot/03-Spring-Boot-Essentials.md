# Spring Boot Essentials - Deep Dive

## Table of Contents
1. [Spring Boot Fundamentals](#spring-boot-fundamentals)
2. [Auto-Configuration](#auto-configuration)
3. [Spring Boot Starters](#spring-boot-starters)
4. [Configuration Management](#configuration-management)
5. [Spring Boot Actuator](#spring-boot-actuator)
6. [Profiles](#profiles)
7. [Embedded Servers](#embedded-servers)
8. [Spring Boot Application Structure](#spring-boot-application-structure)
9. [Auto-Configuration Internals (Spring Boot 3)](#auto-configuration-internals-spring-boot-3)
10. [Custom Starters & Configuration Processor](#custom-starters--configuration-processor)
11. [Graceful Shutdown](#graceful-shutdown)
12. [Spring Boot 3 Migration (Jakarta EE)](#spring-boot-3-migration-jakarta-ee)
13. [Observability with Micrometer](#observability-with-micrometer)

---

## Spring Boot Fundamentals

### 🧠 ELI5: The "Combo Meal"

Imagine you are at a fast-food restaurant.

*   **Spring Framework:** Is like buying everything separately. You have to order the bun, the patty, the lettuce, the tomato, and the fries. Then you have to assemble the burger yourself. It's flexible, but it takes a lot of time and decisions.
*   **Spring Boot:** Is like ordering a **Combo Meal**. You just say "Number 1," and you get a pre-assembled burger, fries, and a drink. It's "opinionated" because the chef decided what goes best together, but it's much faster and easier.

### 🗺️ Mindmap: Spring Boot Overview

## 🗺️ Spring Boot Architecture

> [!TIP]
> **Interview Pro-Tip: "What is the difference between @Value and @ConfigurationProperties?"**
> - **@Value**: Good for single values. Doesn't support "Relaxed Binding" (e.g., `my-prop` vs `myProp`). Harder to group related properties.
> - **@ConfigurationProperties**: Best for groups of related properties. Supports **Relaxed Binding**, validation (JSR-303), and is more type-safe. **Use this for most production configs.**

### 🔍 Deep Dive: The SpringApplication.run() Process
When you call `SpringApplication.run()`, several things happen:
1. **Start StopWatch**: To measure startup time.
2. **Prepare Environment**: Load properties and profiles.
3. **Create ApplicationContext**: Usually an `AnnotationConfigServletWebServerApplicationContext`.
4. **Refresh Context**: This is where all beans are created and auto-configuration is applied.
5. **Call Runners**: Execute any `CommandLineRunner` or `ApplicationRunner` beans.

### 🛠️ Complex Example: Custom @Conditional Annotation
```java
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnUnixCondition.class)
public @interface ConditionalOnUnix {}

public class OnUnixCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return System.getProperty("os.name").toLowerCase().contains("nix") ||
               System.getProperty("os.name").toLowerCase().contains("nux");
    }
}

@Configuration
@ConditionalOnUnix
public class UnixSpecificConfig {
    @Bean
    public MyService unixService() { return new UnixServiceImpl(); }
}
```

## Spring Boot Fundamentals

### What is Spring Boot?

**Definition**: Spring Boot is an opinionated framework built on top of Spring Framework that simplifies the creation of production-ready applications with minimal configuration.

### Key Features

1. **Auto-Configuration**: Automatically configures beans based on classpath
2. **Standalone**: Creates standalone applications with embedded servers
3. **Production-Ready**: Built-in actuator for monitoring and health checks
4. **Opinionated Defaults**: Sensible defaults that can be overridden
5. **No Code Generation**: No code generation or XML configuration required

### Spring vs Spring Boot

| Aspect | Spring Framework | Spring Boot |
|--------|------------------|-------------|
| Configuration | Manual, verbose | Auto-configured |
| Setup | Complex | Simple |
| Dependency Management | Manual | Starters |
| Embedded Server | No | Yes |
| Production Features | Add manually | Built-in (Actuator) |
| Learning Curve | Steep | Gentle |

### Basic Spring Boot Application

```java
@SpringBootApplication
public class MyApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### @SpringBootApplication Annotation

This is a convenience annotation that combines:

```java
@SpringBootApplication = 
    @Configuration +           // Marks class as source of bean definitions
    @EnableAutoConfiguration + // Enable auto-configuration
    @ComponentScan            // Enable component scanning
```

**Expanded Version**:
```java
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.example")
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

---

## Auto-Configuration

### 🧠 ELI5: The "Magic Smart Home"

Imagine you move into a **Smart Home**.

1.  **Detection**: The house senses you've walked into the kitchen (it sees `spring-boot-starter-web` on the classpath).
2.  **Action**: It automatically turns on the lights and starts the coffee maker (it configures Tomcat and Spring MVC).
3.  **Override**: If you manually turn off the coffee maker and start your own French Press (you define your own `Bean`), the house says, "Oh, I see you've got this covered," and stays out of your way.

### 🗺️ Mindmap: Auto-Configuration

## 🏗️ Architecture Diagram

## Auto-Configuration

### How Auto-Configuration Works

Spring Boot examines:
1. **Classpath**: What dependencies are present
2. **Beans**: What beans you've defined
3. **Properties**: Configuration properties

Then automatically configures beans accordingly.

### Auto-Configuration Classes

Located in `spring-boot-autoconfigure.jar`:
- `DataSourceAutoConfiguration`
- `WebMvcAutoConfiguration`
- `JpaRepositoriesAutoConfiguration`
- `SecurityAutoConfiguration`
- And 100+ more...

### Example: DataSource Auto-Configuration

**What Spring Boot Does**:
```java
// If H2, HikariCP on classpath and no DataSource bean defined,
// Spring Boot automatically creates a DataSource bean

@Configuration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnMissingBean(DataSource.class)
public class DataSourceAutoConfiguration {
    
    @Bean
    public DataSource dataSource() {
        // Create and configure DataSource
        return new HikariDataSource();
    }
}
```

### Conditional Annotations

These control when auto-configuration applies:

```java
@ConditionalOnClass(DataSource.class)          // If class exists on classpath
@ConditionalOnMissingClass("com.example.Foo")  // If class doesn't exist
@ConditionalOnBean(DataSource.class)           // If bean of type exists
@ConditionalOnMissingBean(DataSource.class)    // If bean doesn't exist
@ConditionalOnProperty(                         // If property matches
    name = "spring.datasource.type",
    havingValue = "hikari"
)
@ConditionalOnResource(resources = "classpath:data.sql") // If resource exists
@ConditionalOnWebApplication                    // If web application
@ConditionalOnNotWebApplication                 // If not web application
@ConditionalOnExpression("${enabled:false}")    // If SpEL expression true
```

### Creating Custom Auto-Configuration

**Step 1: Create Auto-Configuration Class**
```java
@Configuration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyServiceProperties.class)
public class MyServiceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyServiceProperties properties) {
        return new MyService(properties.getApiKey());
    }
}
```

**Step 2: Create Properties Class**
```java
@ConfigurationProperties(prefix = "myservice")
public class MyServiceProperties {
    private String apiKey;
    private int timeout = 30;
    
    // Getters and setters
}
```

**Step 3: Register Auto-Configuration**
Create `META-INF/spring.factories`:
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.MyServiceAutoConfiguration
```

### Disabling Auto-Configuration

**Method 1: In @SpringBootApplication**
```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
public class MyApplication {
}
```

**Method 2: In application.properties**
```properties
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

### Debugging Auto-Configuration

**Enable Debug Logging**:
```properties
# application.properties
debug=true
```

Or run with:
```bash
java -jar myapp.jar --debug
```

**Output Shows**:
- ✅ Positive matches (auto-configurations that were applied)
- ❌ Negative matches (why configurations were not applied)
- 🚫 Exclusions
- 🔧 Unconditional classes

---

## Spring Boot Starters

### 🧠 ELI5: The "Toolbox"

Imagine you want to do some **Gardening**.

*   **Without Starters:** You have to go to the store and buy a shovel, a rake, seeds, soil, and gloves individually. You might forget something important.
*   **With Starters:** You buy the **"Gardening Starter Kit."** It comes with everything you need in one box. If you want to do **"Painting,"** you just grab the "Painting Starter Kit." You don't have to worry about the individual tools; the kit has you covered.

### 🗺️ Mindmap: Starters

## 📦 Spring Boot Starters

## Spring Boot Starters

### What are Starters?

Starters are dependency descriptors that bring all required dependencies for a feature.

### Common Starters

#### 1. spring-boot-starter-web
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**Includes**:
- Spring MVC
- Tomcat (embedded server)
- Jackson (JSON processing)
- Validation
- Logging

#### 2. spring-boot-starter-data-jpa
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**Includes**:
- Hibernate
- Spring Data JPA
- Spring ORM
- Transaction support

#### 3. spring-boot-starter-security
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

#### 4. spring-boot-starter-test
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Includes**:
- JUnit 5
- Mockito
- AssertJ
- Hamcrest
- Spring Test

### Full List of Important Starters

| Starter | Purpose |
|---------|---------|
| spring-boot-starter | Core starter with auto-config |
| spring-boot-starter-web | Web applications, REST APIs |
| spring-boot-starter-webflux | Reactive web applications |
| spring-boot-starter-data-jpa | JPA with Hibernate |
| spring-boot-starter-data-mongodb | MongoDB |
| spring-boot-starter-data-redis | Redis |
| spring-boot-starter-security | Spring Security |
| spring-boot-starter-oauth2-client | OAuth2 client |
| spring-boot-starter-validation | Bean Validation |
| spring-boot-starter-actuator | Production monitoring |
| spring-boot-starter-test | Testing |
| spring-boot-starter-cache | Caching abstraction |
| spring-boot-starter-amqp | RabbitMQ |
| spring-boot-starter-kafka | Apache Kafka |
| spring-boot-starter-mail | Email support |
| spring-boot-starter-batch | Spring Batch |

### Creating Custom Starter

**Project Structure**:
```
my-custom-starter/
├── pom.xml
├── src/main/java/
│   └── com/example/
│       ├── MyServiceAutoConfiguration.java
│       ├── MyServiceProperties.java
│       └── MyService.java
└── src/main/resources/
    └── META-INF/
        └── spring.factories
```

**pom.xml**:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## Configuration Management

### application.properties vs application.yml

**application.properties**:
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=update
```

**application.yml** (Preferred):
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
  jpa:
    hibernate:
      ddl-auto: update
```

### @ConfigurationProperties

**Strongly Typed Configuration**:

```java
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private String name;
    private Security security = new Security();
    private List<String> servers = new ArrayList<>();
    
    // Getters and setters
    
    public static class Security {
        private String username;
        private String password;
        private List<String> roles;
        
        // Getters and setters
    }
}
```

**application.yml**:
```yaml
app:
  name: MyApplication
  security:
    username: admin
    password: secret
    roles:
      - ADMIN
      - USER
  servers:
    - server1.example.com
    - server2.example.com
```

**Usage**:
```java
@Service
public class MyService {
    
    private final AppProperties appProperties;
    
    public MyService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }
    
    public void printConfig() {
        System.out.println("App: " + appProperties.getName());
        System.out.println("User: " + appProperties.getSecurity().getUsername());
    }
}
```

### @Value Annotation

```java
@Component
public class MyComponent {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.timeout:30}") // Default value
    private int timeout;
    
    @Value("${app.enabled:true}")
    private boolean enabled;
    
    @Value("#{${app.servers}}") // SpEL for lists
    private List<String> servers;
}
```

### Configuration Priority (Highest to Lowest)

1. Command line arguments
2. SPRING_APPLICATION_JSON properties
3. ServletConfig init parameters
4. ServletContext init parameters
5. JNDI attributes
6. Java System properties
7. OS environment variables
8. Profile-specific properties (application-{profile}.yml)
9. Application properties (application.yml)
10. @PropertySource
11. Default properties

**Example**:
```bash
# Override via command line
java -jar myapp.jar --server.port=9090

# Override via environment variable
export SERVER_PORT=9090
java -jar myapp.jar

# Override via system property
java -Dserver.port=9090 -jar myapp.jar
```

### Externalized Configuration

**Config File Locations (Priority)**:
1. `/config` subdirectory of current directory
2. Current directory
3. `classpath:/config`
4. `classpath:/`

```bash
myapp/
├── config/
│   └── application.yml        # Highest priority
├── application.yml             # Second priority
├── myapp.jar
│   ├── config/
│   │   └── application.yml    # Third priority
│   └── application.yml         # Lowest priority
```

### Custom Property Files

```java
@Configuration
@PropertySource("classpath:custom.properties")
@PropertySource("classpath:database.properties")
public class AppConfig {
}
```

---

## Spring Boot Actuator

### 🧠 ELI5: The "Car Dashboard"

When you drive a car, you don't need to look under the hood every minute to see if the engine is okay. You just look at the **Dashboard**.

*   **Health Check (`/health`):** Is like the "Check Engine" light. If it's green, you're good.
*   **Metrics (`/metrics`):** Is like the Speedometer or Fuel Gauge. It tells you how fast you're going and how much "gas" (memory/CPU) you have left.
*   **Info (`/info`):** Is like the Car's Manual or VIN number. It tells you what version of the car you're driving.

### 🗺️ Mindmap: Actuator

## 🏗️ Architecture Diagram

## Spring Boot Actuator

### What is Actuator?

Production-ready features for monitoring and managing applications.

### Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Common Endpoints

| Endpoint | Purpose |
|----------|---------|
| /actuator/health | Application health information |
| /actuator/info | Application information |
| /actuator/metrics | Application metrics |
| /actuator/env | Environment properties |
| /actuator/loggers | Logger configuration |
| /actuator/httptrace | HTTP trace information |
| /actuator/beans | All Spring beans |
| /actuator/mappings | All @RequestMapping paths |
| /actuator/configprops | All @ConfigurationProperties |
| /actuator/threaddump | Thread dump |
| /actuator/heapdump | Heap dump |
| /actuator/shutdown | Shutdown application (disabled by default) |

### Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env  # Expose specific endpoints
        # include: "*"  # Expose all endpoints
      base-path: /actuator  # Base path for actuator endpoints
  endpoint:
    health:
      show-details: always  # Show detailed health information
    shutdown:
      enabled: true  # Enable shutdown endpoint
  metrics:
    tags:
      application: ${spring.application.name}
```

### Custom Health Indicator

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check some condition
        boolean error = check();
        
        if (error) {
            return Health.down()
                .withDetail("Error Code", 500)
                .withDetail("Description", "Service unavailable")
                .build();
        }
        
        return Health.up()
            .withDetail("Status", "Service is running")
            .withDetail("Version", "1.0.0")
            .build();
    }
    
    private boolean check() {
        // Implement health check logic
        return false;
    }
}
```

### Custom Metrics

```java
@Service
public class OrderService {
    
    private final MeterRegistry meterRegistry;
    private final Counter orderCounter;
    
    public OrderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.orderCounter = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("type", "online")
            .register(meterRegistry);
    }
    
    public void createOrder(Order order) {
        // Business logic
        orderCounter.increment();
        
        // Timer example
        meterRegistry.timer("order.processing.time").record(() -> {
            // Process order
        });
    }
}
```

### Custom Info Endpoint

**application.yml**:
```yaml
info:
  app:
    name: My Application
    description: This is my Spring Boot application
    version: 1.0.0
  developer:
    name: John Doe
    email: john@example.com
```

**Custom InfoContributor**:
```java
@Component
public class CustomInfoContributor implements InfoContributor {
    
    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("custom-info", 
            Map.of(
                "key1", "value1",
                "key2", "value2"
            )
        );
    }
}
```

---

## Profiles

## 🏗️ Architecture Diagram

## Profiles

### What are Profiles?

Profiles provide a way to segregate parts of your application configuration and make it available only in certain environments.

### Defining Profiles

**Profile-Specific Configuration Files**:
```
application.yml              # Default
application-dev.yml          # Development profile
application-test.yml         # Test profile
application-prod.yml         # Production profile
```

**application-dev.yml**:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop

logging:
  level:
    root: DEBUG
```

**application-prod.yml**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

logging:
  level:
    root: WARN
```

### Activating Profiles

**Method 1: application.yml**
```yaml
spring:
  profiles:
    active: dev
```

**Method 2: Command Line**
```bash
java -jar myapp.jar --spring.profiles.active=prod
```

**Method 3: Environment Variable**
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar myapp.jar
```

**Method 4: Programmatically**
```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MyApplication.class);
        app.setAdditionalProfiles("dev");
        app.run(args);
    }
}
```

### Profile-Specific Beans

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
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://prod-server:3306/mydb");
        return dataSource;
    }
    
    @Bean
    @Profile({"dev", "test"})  // Active for multiple profiles
    public DevService devService() {
        return new DevService();
    }
    
    @Bean
    @Profile("!prod")  // Active for all profiles except prod
    public MockService mockService() {
        return new MockService();
    }
}
```

### Multi-Document YAML Files

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

---

## Embedded Servers

### Supported Embedded Servers

1. **Tomcat** (Default)
2. **Jetty**
3. **Undertow**
4. **Netty** (for WebFlux)

### Switching Embedded Servers

**From Tomcat to Jetty**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

**From Tomcat to Undertow**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

### Server Configuration

```yaml
server:
  port: 8080
  address: 0.0.0.0
  servlet:
    context-path: /api
    session:
      timeout: 30m
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,application/json
  http2:
    enabled: true
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: secret
    key-store-type: PKCS12
  tomcat:
    max-threads: 200
    max-connections: 10000
    accept-count: 100
    connection-timeout: 20000
```

### Customizing Embedded Server

```java
@Configuration
public class ServerConfig {
    
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> 
            tomcatCustomizer() {
        return factory -> {
            factory.setPort(9090);
            factory.setContextPath("/myapp");
            factory.addConnectorCustomizers(connector -> {
                connector.setMaxPostSize(10000000);
            });
        };
    }
}
```

---

## Spring Boot Application Structure

### Recommended Package Structure

```
com.example.myapp/
├── MyApplication.java              # Main application class
├── config/                         # Configuration classes
│   ├── SecurityConfig.java
│   ├── DatabaseConfig.java
│   └── CacheConfig.java
├── controller/                     # REST controllers
│   ├── UserController.java
│   └── ProductController.java
├── service/                        # Business logic
│   ├── UserService.java
│   ├── UserServiceImpl.java
│   └── ProductService.java
├── repository/                     # Data access layer
│   ├── UserRepository.java
│   └── ProductRepository.java
├── model/                          # Domain models/entities
│   ├── User.java
│   └── Product.java
├── dto/                            # Data Transfer Objects
│   ├── UserDTO.java
│   └── ProductDTO.java
├── exception/                      # Custom exceptions
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
├── util/                           # Utility classes
│   └── DateUtils.java
└── security/                       # Security components
    ├── JwtTokenProvider.java
    └── UserDetailsServiceImpl.java
```

### Main Application Class

```java
@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks
@EnableAsync       // Enable async processing
@EnableCaching     // Enable caching
public class MyApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner demo() {
        return args -> {
            System.out.println("Application started!");
        };
    }
}
```

### Spring Boot DevTools

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

**Features**:
- Automatic restart on code changes
- LiveReload support
- Property defaults for development
- Remote debugging support

**Configuration**:
```yaml
spring:
  devtools:
    restart:
      enabled: true
      exclude: static/**,public/**
    livereload:
      enabled: true
```

---

## Spring Boot CLI

The Spring Boot CLI (Command Line Interface) is a command line tool that you can use to quickly prototype with Spring. It allows you to run Groovy scripts, which means you don't have to write much boilerplate code.

### Installation
```bash
# Mac (Homebrew)
brew tap spring-io/tap
brew install spring-boot
```

### Usage Example
Create a file named `app.groovy`:

```groovy
@RestController
class ThisWillActuallyRun {

    @RequestMapping("/")
    String home() {
        "Hello World!"
    }

}
```

Run it:
```bash
spring run app.groovy
```

### Key Features
- **Automatic Dependency Resolution**: Automatically adds dependencies based on code usage (e.g., adds `spring-boot-starter-web` if `@RestController` is found).
- **No Build System Required**: No Maven or Gradle needed for simple scripts.
- **Groovy Support**: Concise syntax.

---

## Key Interview Questions

### Q1: Explain the difference between @Component and @Bean

**Answer**:
- **@Component**: Applied at class level, auto-detected by component scanning
- **@Bean**: Applied at method level in @Configuration classes, gives you programmatic control

```java
// @Component approach
@Component
public class MyService { }

// @Bean approach
@Configuration
public class AppConfig {
    @Bean
    public MyService myService() {
        return new MyService();
    }
}
```

### Q2: How does Spring Boot Auto-Configuration work?

**Answer**: 
Spring Boot uses `@EnableAutoConfiguration` which:
1. Scans classpath for libraries
2. Reads `META-INF/spring.factories` from JARs
3. Applies conditional auto-configuration classes
4. Uses `@Conditional` annotations to determine what to configure
5. Can be overridden by custom beans

### Q3: What's the difference between application.properties and application.yml?

**Answer**:
- Both serve the same purpose
- YAML is more readable for hierarchical data
- YAML supports multi-document files (multiple profiles in one file)
- Properties is simpler for flat configuration

### Q4: How do you secure Actuator endpoints?

**Answer**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info  # Only expose specific endpoints

# With Spring Security
spring:
  security:
    user:
      name: admin
      password: secret
```

```java
@Configuration
public class ActuatorSecurity extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests()
            .anyRequest().hasRole("ADMIN");
    }
}
```

---

## Auto-Configuration Internals (Spring Boot 3)

### The New Registration Mechanism
In Spring Boot 3, the way auto-configurations are registered has changed.

- **Old Way (Spring Boot 2.x)**: Registered in `META-INF/spring.factories` under the key `org.springframework.boot.autoconfigure.EnableAutoConfiguration`.
- **New Way (Spring Boot 3.x)**: Registered in a new file: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

**Format of `.imports` file**:
Each line is a fully qualified class name of an auto-configuration class.
```text
com.example.MyAutoConfiguration
com.example.AnotherAutoConfiguration
```

### AutoConfigurationImportSelector
This is the core class that:
1.  Scans the `.imports` files.
2.  Filters out configurations based on `@Conditional` annotations.
3.  Determines the order using `@AutoConfigureAfter`, `@AutoConfigureBefore`, and `@AutoConfigureOrder`.

---

## Custom Starters & Configuration Processor

### Why use `spring-boot-configuration-processor`?
When you create a custom starter, you want IDE support (auto-completion) for your properties in `application.yml`.

**How it works**:
1.  Add the dependency to your starter:
    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
    ```
2.  During compilation, it generates a `META-INF/spring-configuration-metadata.json` file.
3.  IDEs read this file to provide tooltips and auto-completion.

---

## Graceful Shutdown

### What is Graceful Shutdown?
It allows the application to finish processing active requests before shutting down, instead of killing them immediately.

### Configuration
```yaml
server:
  shutdown: graceful # Options: immediate (default), graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s # Time to wait for requests to finish
```

**How it works**:
1.  The server stops accepting new requests.
2.  The server waits for a "grace period" for active requests to complete.
3.  If requests don't finish within the timeout, the server kills them and shuts down.

---

## Spring Boot 3 Migration (Jakarta EE)

### The Namespace Change
The biggest change in Spring Boot 3 is the migration from **Java EE** to **Jakarta EE**.
- All `javax.*` imports for web and persistence are now `jakarta.*`.
- Example: `javax.persistence.Entity` → `jakarta.persistence.Entity`.
- Example: `javax.servlet.http.HttpServlet` → `jakarta.servlet.http.HttpServlet`.

### Key Migration Steps
1.  **Java 17+**: Upgrade your JDK.
2.  **Dependency Updates**: Many libraries changed their group IDs or versions to support Jakarta EE (e.g., Hibernate 6, Jetty 11).
3.  **Property Changes**: Some properties were renamed (e.g., `spring.redis.*` → `spring.data.redis.*`).

---

## Observability with Micrometer

### Beyond Actuator
Spring Boot 3 introduces a new "Observability" API built on **Micrometer**. It combines metrics and tracing into a single abstraction.

### Key Components
1.  **ObservationRegistry**: The central point for recording observations.
2.  **ObservationHandler**: Decides what to do with an observation (e.g., send to Zipkin, log it, or update a counter).

### Setup for Tracing
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
```

**Benefits**:
- Unified API for metrics and traces.
- Better performance than the old Spring Cloud Sleuth.
- Native support for OpenTelemetry.

---

## Best Practices

1. ✅ **Use YAML over properties** for hierarchical configuration
2. ✅ **Use @ConfigurationProperties** over @Value for complex configurations
3. ✅ **Enable Actuator** in production for monitoring
4. ✅ **Use profiles** for environment-specific configuration
5. ✅ **Externalize sensitive data** (passwords, API keys)
6. ✅ **Use DevTools** during development
7. ✅ **Follow recommended package structure**
8. ✅ **Disable auto-configurations** you don't need
9. ❌ **Don't hardcode** configuration values
10. ❌ **Don't commit** production credentials

---

## Practice Exercises

1. Create a custom Spring Boot starter
2. Implement custom auto-configuration with conditions
3. Set up multiple profiles (dev, test, prod)
4. Create custom Actuator endpoints
5. Implement custom health indicators
6. Configure SSL for embedded Tomcat
7. Switch between different embedded servers
