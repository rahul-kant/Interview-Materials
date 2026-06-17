# 🚀 Spring Boot — The Complete Deep Dive (From Request to Response)

> _From "What is Spring Boot?" to "How does `java
 -jar app.jar` actually start a Spring application?"_

---

## 📑 Table of Contents

1. [10,000-Foot View: Why Spring Boot?](#1-10000ft-view)
2. [What Exactly Is Spring Boot?](#2-what-is-spring-boot)
3. [Spring vs Spring Boot: The Real Difference](#3-spring-vs-springboot)
4. [The Magic: `java -jar app.jar`](#4-magic-of-jar)
5. [The SpringApplication Lifecycle](#5-springapplication-lifecycle)
6. [Auto-Configuration: The Heart of Spring Boot](#6-auto-configuration)
7. [The @SpringBootApplication Annotation Deep Dive](#7-springbootapplication-annotation)
8. [Externalized Configuration: How `application.properties` Works](#8-externalized-config)
9. [Profiles: Environment-Specific Configuration](#9-profiles)
10. [Spring Boot Starters Explained](#10-starters)
11. [Embedded Server: Tomcat Inside Your JAR](#11-embedded-server)
12. [Full Architecture Diagram](#12-architecture-diagram)
13. [From Request to Response: Complete Flow](#13-request-to-response)
14. [Spring Boot Actuator: Production Features](#14-actuator)
15. [Spring Boot Startup Phases (The Full Timeline)](#15-startup-phases)
16. [Conditional Beans: The `@Conditional` Family](#16-conditional-beans)
17. [Custom Auto-Configuration: Writing Your Own Starter](#17-custom-autoconfig)
18. [FAANG Discussion Points (Top 20)](#18-faang-points)
19. [Top 20 Follow-up Q&A](#19-followup)
20. [Summary: The One-Page Mental Model](#20-summary)

---

<a name="1-10000ft-view"></a>
## 1. 10,000-Foot View: Why Spring Boot?

**Spring Framework** is powerful but **painful to set up**. To run a simple REST API, you needed:
- `web.xml` or Java config
- A Tomcat installation
- Maven WAR packaging
- External property files
- Manual bean wiring
- XML everywhere

**Spring Boot** is **opinionated** — it makes decisions for you, so you write **business code**, not 
infrastructure.

**The four pillars of Spring Boot:**
1. **Auto-Configuration** — automatically configures beans based on classpath
2. **Starter Dependencies** — curated bundles of compatible libraries
3. **Embedded Server** — Tomcat/Jetty/Undertow is part of your app
4. **Externalized Configuration** — `application.properties` / `application.yml` / env vars / CLI args

**In one line:** *Spring Boot is Spring Framework + opinionated defaults + auto-configuration + production-ready 
features.*

**FAANG Discussion Point:**  
*"Why did Pivotal build Spring Boot when Spring already worked?"* — Because **developer productivity** matters. 
Netflix's Adrian Cockcroft famously said: *"The cost of moving from Spring to Spring Boot was 10x less than the 
cost of running Spring without it."* The 2014 release was a direct response to **Dropwizard** and **Play 
Framework** which offered faster setup. Spring Boot turned out to be so successful it became **the default** for 
new Java backend projects worldwide.

---

<a name="2-what-is-spring-boot"></a>
## 2. What Exactly Is Spring Boot?

Spring Boot is a **layer on top of Spring Framework**. It's not a replacement — it's a **productivity enhancer**.

**Conceptual stack:**
```
┌────────────────────────────────────────────────┐
│  Your Code (@RestController, @Service, etc.)  │
├────────────────────────────────────────────────┤
│  Spring Boot (Auto-Config, Starters, Actuator)│
├────────────────────────────────────────────────┤
│  Spring Framework (DI, MVC, AOP, Data, etc.)  │
├────────────────────────────────────────────────┤
│  Java/Jakarta EE (Servlet API, JPA, etc.)     │
└────────────────────────────────────────────────┘
```

**What Spring Boot provides:**

| Feature | What It Does |
|---|---|
| **Auto-Configuration** | Configures beans automatically based on classpath |
| **Starter Dependencies** | Curated dependency bundles (e.g., `spring-boot-starter-web`) |
| **Embedded Server** | Tomcat/Jetty/Undertow runs inside your JAR |
| **Externalized Config** | `application.properties`, YAML, env vars, CLI args |
| **Production Features** | Health, metrics, traces via Spring Boot Actuator |
| **CLI** | `spring run app.groovy` (Groovy scripting) |
| **DevTools** | Auto-restart, LiveReload for development |

**FAANG Discussion Point:**  
*"Is Spring Boot just Spring with less XML?"* — That's a common misconception. Spring Boot is **way more**:
1. **Auto-Configuration** uses `@Conditional` to wire beans intelligently (no XML ever needed).
2. **Embedded server** means deployable JARs (12-factor app compliant).
3. **Actuator** provides production-grade observability out of the box.

---

<a name="3-spring-vs-springboot"></a>
## 3. Spring vs Spring Boot: The Real Difference

| Aspect | Spring Framework | Spring Boot |
|---|---|---|
| **Configuration** | Manual (XML/Java) | Auto-configured |
| **Server** | External Tomcat | Embedded |
| **Packaging** | WAR | Executable JAR |
| **Startup** | Deploy WAR to container | `java -jar app.jar` |
| **Bean wiring** | `@ComponentScan` + explicit | Auto-config + conditional |
| **Production tools** | None built-in | Actuator (health, metrics) |
| **Decision making** | You decide everything | Spring Boot decides (with overrides) |

**Example: Same app, two ways**

**Pure Spring (old style):**
```java
@Configuration
@EnableWebMvc
@ComponentScan("com.example")
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
    }
}
```
Plus `web.xml`, plus external Tomcat, plus...

**Spring Boot:**
```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```
That's it. No `web.xml`. No XML. No manual Jackson config. No server install.

**FAANG Discussion Point:**  
*"Can you use Spring Boot without Spring?"* — **No.** Spring Boot is a **Spring Framework extension**. It uses 
Spring's core features (IoC, AOP, MVC, Data) and adds auto-configuration on top.

---

<a name="4-magic-of-jar"></a>
## 4. The Magic: `java -jar app.jar`

When you run `java -jar myapp.jar`, here's what actually happens:

**The executable JAR structure:**
```
myapp.jar
├── BOOT-INF/
│   ├── classes/              ← your .class files
│   ├── lib/                  ← third-party dependencies (Tomcat, Jackson, etc.)
│   │   ├── spring-boot-3.2.0.jar
│   │   ├── spring-web-6.1.0.jar
│   │   ├── tomcat-embed-core-10.1.0.jar
│   │   └── jackson-databind-2.16.0.jar
│   └── classpath.idx         ← classpath index for fast loading
├── META-INF/
│   ├── MANIFEST.MF           ← contains Main-Class
│   └── maven/                ← build info
└── org/springframework/boot/loader/  ← custom class loader
    ├── launch/               ← Spring Boot launcher classes
    └── JarLauncher.class
```

**The MANIFEST.MF (key part):**
```
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: com.example.MyApp
```

**The launch sequence:**
1. JVM reads `MANIFEST.MF` → loads `JarLauncher`
2. `JarLauncher` creates a custom `LaunchedURLClassLoader` that knows how to load classes from nested JARs
3. It finds the `Start-Class` (`com.example.MyApp`) in the manifest
4. It invokes `MyApp.main(args)` using reflection
5. Your `main` method calls `SpringApplication.run(...)`

**Why a custom class loader?**  
Standard Java JARs can't have nested JARs. Spring Boot packages all dependencies as **nested JARs** inside the 
executable JAR. The custom class loader reads nested JAR entries directly from the master JAR file.

**FAANG Discussion Point:**  
*"Why not just use `mvn package` and deploy to external Tomcat?"* — The executable JAR is a **12-factor app** 
(Heroku, Cloud Foundry, Kubernetes-friendly). One artifact, runs anywhere with a JDK. No "works on my machine" 
because of Tomcat version mismatch.

---

<a name="5-springapplication-lifecycle"></a>
## 5. The SpringApplication Lifecycle

`SpringApplication.run()` is the entry point. It does **a lot** more than most developers realize.

**The high-level flow:**
```java
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```

**Behind the scenes, `SpringApplication.run()` executes:**

```java
public ConfigurableApplicationContext run(String... args) {
    
    // 1. Bootstrap — create BootstrapRegistry
    BootstrapRegistry bootstrapRegistry = createBootstrapRegistry();
    
    // 2. Prepare environment
    ConfigurableEnvironment environment = prepareEnvironment(bootstrapRegistry, args);
    
    // 3. Print banner (Spring logo)
    Banner printedBanner = printBanner(environment);
    
    // 4. Create ApplicationContext (web or non-web)
    context = createApplicationContext();
    
    // 5. Prepare context — apply initializers
    prepareContext(bootstrapRegistry, context, environment, listeners, applicationArguments, printedBanner);
    
    // 6. Refresh context — bean factory, all beans created
    refreshContext(context);
    
    // 7. After refresh — call runners
    afterRefresh(context, applicationArguments);
    
    // 8. Return the running context
    return context;
}
```

**What happens at each step:**

**Step 1: Bootstrap**
- Creates `BootstrapRegistry` for early bean creation (before context exists).
- Used by `EnvironmentPostProcessor` and `FailureAnalyzers`.

**Step 2: Prepare Environment**
- Creates `ConfigurableEnvironment` (StandardEnvironment for non-web, StandardServletEnvironment for web).
- Adds **Property Sources** in this order:
  1. Devtools global settings (if active)
  2. `@TestPropertySource` (in tests)
  3. `application.yml` / `application.properties` (from classpath)
  4. `application-{profile}.yml`
  5. Command line args (`--server.port=9090`)
  6. Environment variables
  7. System properties (`-Dserver.port=9090`)

**Step 3: Banner**
- Prints the Spring ASCII art banner (or your custom banner.txt).
- Disabled via `spring.main.banner-mode=off`.

**Step 4: Create ApplicationContext**
- Web app → `AnnotationConfigServletWebServerApplicationContext`
- Reactive app → `AnnotationConfigReactiveWebServerApplicationContext`
- Non-web → `AnnotationConfigApplicationContext`

**Step 5: Prepare Context**
- Applies `ApplicationContextInitializer`s (from `META-INF/spring.factories` or 
`META-INF/spring/org.springframework.boot.context.initializer...`).
- Triggers `ApplicationPreparedEvent`.

**Step 6: Refresh Context**
- This is **Spring's core lifecycle** (from Spring Framework).
- Creates BeanFactory.
- Performs component scanning.
- Applies auto-configuration.
- Creates all singleton beans.
- Starts the embedded web server (if web).
- Triggers `ApplicationStartedEvent`.

**Step 7: After Refresh**
- Calls `ApplicationRunner` and `CommandLineRunner` beans in order.
- Triggers `ApplicationReadyEvent`.

**Step 8: Return Context**
- The application is now ready to accept requests.

**FAANG Discussion Point:**  
*"What's the difference between SpringApplication.run() and new SpringApplication().run()?"*  
The latter gives you a builder to configure `SpringApplication` before running:
```java
SpringApplication app = new SpringApplication(MyApp.class);
app.setBannerMode(Banner.Mode.OFF);
app.setLogStartupInfo(false);
app.run(args);
```

---

<a name="6-auto-configuration"></a>
## 6. Auto-Configuration: The Heart of Spring Boot

This is **the** defining feature of Spring Boot. It's how Spring Boot configures 80% of your app without you 
writing config.

**The mechanism:**
1. Spring Boot scans `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring 
Boot 3+) or `META-INF/spring.factories` (older versions).
2. Each entry is a `@Configuration` class with conditional bean definitions.
3. Spring Boot evaluates `@Conditional` annotations to decide whether to apply.

**Example: `DataSourceAutoConfiguration` (simplified):**
```java
@AutoConfiguration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(DataSource.class)
    static class EmbeddedDatabaseConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public DataSource dataSource(DataSourceProperties properties) {
            return properties.initializeDataSourceBuilder().build();
        }
    }
}
```

**What it means:**
- If `DataSource` class is on the classpath → consider this config.
- If no `DataSource` bean is defined yet → create one from `spring.datasource.url`, `spring.datasource.username`, 
etc.

**The Spring Boot auto-configurations include (some highlights):**
- `DispatcherServletAutoConfiguration` — registers `DispatcherServlet`
- `JacksonAutoConfiguration` — configures Jackson `ObjectMapper`
- `DataSourceAutoConfiguration` — sets up `DataSource` from properties
- `JpaRepositoriesAutoConfiguration` — enables JPA repositories
- `RedisAutoConfiguration` — sets up `RedisConnectionFactory`
- `KafkaAutoConfiguration` — Kafka producers/consumers
- `SecurityAutoConfiguration` — Spring Security defaults
- `ActuatorAutoConfiguration` — `/actuator/*` endpoints
- `WebMvcAutoConfiguration` — Spring MVC defaults

**FAANG Discussion Point:**  
*"How does auto-configuration NOT conflict with my @Configuration?"*  
By using `@ConditionalOnMissingBean`. Spring Boot's auto-config runs **first**, but only creates a bean if **you 
haven't already defined one**. This is the **"convention over configuration"** pattern: Spring Boot provides 
defaults, you override.

**Example override:**
```java
@Configuration
public class MyConfig {
    @Bean
    public DataSource dataSource() {
        // My custom DataSource
    }
}
```
The `EmbeddedDatabaseConfiguration` sees `DataSource` already exists → skips its bean. Yours wins.

---

<a name="7-springbootapplication-annotation"></a>
## 7. The @SpringBootApplication Annotation Deep Dive

`@SpringBootApplication` is a **meta-annotation** that combines three annotations:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = { ... })
public @interface SpringBootApplication {
    // attributes...
}
```

**What each inner annotation does:**

### `@SpringBootConfiguration`
- Marker that says "this is a Spring configuration class".
- Equivalent to `@Configuration` (in fact, it's itself annotated with `@Configuration`).
- Spring Boot uses it to find the main config class.

### `@EnableAutoConfiguration`
- **Triggers auto-configuration**.
- Internally imports `AutoConfigurationImportSelector`.
- Reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Applies all auto-config classes with conditional evaluation.

### `@ComponentScan`
- Scans the package of the annotated class and all sub-packages.
- **Excludes** auto-config classes (which are in `org.springframework.boot.autoconfigure.*`).
- The exclude filter uses `TypeExcludeFilter` to skip config classes.

**What `@SpringBootApplication` does NOT do (common misconception):**
- ❌ It does NOT enable Spring MVC (that's `WebMvcAutoConfiguration`).
- ❌ It does NOT enable JPA (that's `JpaRepositoriesAutoConfiguration`).
- ❌ It does NOT enable transactions (that's `TransactionAutoConfiguration`).
- ✅ It ONLY enables component scanning + auto-configuration + marks the main class.

**The main class requirement:**
- `@SpringBootApplication` must be on a class with a `main` method.
- This class is the "starting point" for component scanning.
- The package of this class is the **base package** for scanning.

**Example:**
```java
package com.example.myapp;  // ← base package

@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}
```
Scans `com.example.myapp` and all sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller`, etc.

**FAANG Discussion Point:**  
*"What if my main class is in the 'wrong' package?"*  
If `com.example.MyApp` is the main class, it scans `com.example` (and sub-packages). If your controllers are in 
`com.acme.controllers`, they're **NOT** in the scan path. Solution: pass `scanBasePackages`:
```java
@SpringBootApplication(scanBasePackages = "com.acme")
public class MyApp { }
```

---

<a name="8-externalized-config"></a>
## 8. Externalized Configuration: How application.properties Works

Spring Boot has a **12-factor configuration** model. Configuration comes from many sources, in a strict precedence 
order.

**The PropertySource order (highest to lowest precedence):**

```
1. Devtools global settings (when Devtools is active)
2. @TestPropertySource (in tests)
3. Command line arguments: --server.port=9090
4. SPRING_APPLICATION_JSON (inline JSON env var)
5. ServletConfig init parameters
6. ServletContext init parameters
7. JNDI attributes
8. Java System properties: -Dserver.port=9090
9. OS environment variables: SERVER_PORT=9090
10. RandomValuePropertySource (random.*)
11. application-{profile}.properties (outside packaged JAR)
12. application.properties (outside packaged JAR)
13. application-{profile}.properties (inside packaged JAR)
14. application.properties (inside packaged JAR)
15. @PropertySource annotations on @Configuration classes
16. Default properties (SpringApplication.setDefaultProperties)
```

**Example application.yml:**
```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

logging:
  level:
    com.example.myapp: DEBUG
    org.springframework.web: INFO
```

**Binding to POJOs with @ConfigurationProperties:**
```java
@ConfigurationProperties(prefix = "myapp.mail")
public class MailProperties {
    private String host;
    private int port = 587;
    private Duration timeout = Duration.ofSeconds(30);
    // getters and setters
}
```
Spring Boot auto-binds `myapp.mail.*` properties to this class.

**Type-safe vs @Value:**
- `@Value("${myapp.mail.host}")` — string-based, less safe.
- `@ConfigurationProperties` — type-safe, supports complex types, validation.

**FAANG Discussion Point:**  
*"Why is command-line higher precedence than application.properties?"* — **12-factor app** principle: 
deployment-specific config (env vars, CLI) wins over packaged defaults. This lets you ship one JAR to 
dev/staging/prod and override per environment.

---

<a name="9-profiles"></a>
## 9. Profiles: Environment-Specific Configuration

**Profiles** let you define environment-specific config (dev, test, prod).

**application-dev.yml:**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
logging:
  level:
    com.example.myapp: DEBUG
```

**application-prod.yml:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/mydb
    username: ${DB_USER}
    password: ${DB_PASSWORD}
logging:
  level:
    com.example.myapp: INFO
```

**Activating a profile:**

1. **application.properties:** `spring.profiles.active=dev`
2. **Command line:** `java -jar app.jar --spring.profiles.active=prod`
3. **Environment variable:** `SPRING_PROFILES_ACTIVE=prod`
4. **Programmatic:** `--spring.profiles.active=dev,debug`

**Profile-specific beans:**
```java
@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public DataSource devDataSource() {
        return new EmbeddedDatabaseBuilder().build();
    }
}

@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    public DataSource prodDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://prod/mydb");
        return ds;
    }
}
```

**Default profile:**
If no profile is active, Spring Boot uses the `default` profile.

**FAANG Discussion Point:**  
*"How do you handle secrets in profiles?"* — Never put passwords in `application-prod.yml` committed to Git. Use:
- Environment variables (Kubernetes secrets, AWS Secrets Manager).
- HashiCorp Vault (Spring Cloud Vault).
- Spring Cloud Config Server (centralized config).

---

<a name="10-starters"></a>
## 10. Spring Boot Starters Explained

A **Starter** is a **Maven/Gradle dependency** that bundles all the libraries you need for a specific feature. 
It's a curated set of compatible dependencies.

**Common starters:**

| Starter | What It Pulls In |
|---|---|
| `spring-boot-starter-web` | Spring MVC, Jackson, Tomcat, Validation |
| `spring-boot-starter-data-jpa` | Spring Data JPA, Hibernate, JDBC |
| `spring-boot-starter-security` | Spring Security |
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ, Spring Test |
| `spring-boot-starter-actuator` | Health, metrics, info endpoints |
| `spring-boot-starter-data-redis` | Spring Data Redis, Lettuce |
| `spring-boot-starter-amqp` | Spring AMQP, RabbitMQ client |
| `spring-boot-starter-mail` | JavaMail, Spring email support |
| `spring-boot-starter-cache` | Spring Cache abstraction |
| `spring-boot-starter-validation` | Hibernate Validator (Bean Validation) |
| `spring-boot-starter-thymeleaf` | Thymeleaf templating |
| `spring-boot-starter-webflux` | Spring WebFlux (reactive) |

**Example: what's in spring-boot-starter-web:**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-json</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-tomcat</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.web</groupId>
        <artifactId>spring-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.webmvc</groupId>
        <artifactId>spring-webmvc</artifactId>
    </dependency>
</dependencies>
```

**Why starters?**
- **No version conflicts** — Spring Boot's BOM (Bill of Materials) pins compatible versions.
- **Less XML** — one starter pulls 20+ libraries.
- **Fast onboarding** — new developers don't need to know the "right" combination.

**FAANG Discussion Point:**  
*"What's the difference between spring-boot-starter and spring-boot-starter-web?"*  
- `spring-boot-starter` — the **base** (logging, auto-config, properties). Always included transitively.
- `spring-boot-starter-web` — adds web-specific deps. Includes the base + Jackson + Tomcat + Spring MVC.

---

<a name="11-embedded-server"></a>
## 11. Embedded Server: Tomcat Inside Your JAR

Spring Boot's killer feature — **no external server needed**. The Tomcat (or Jetty/Undertow) is **embedded** in 
your JAR.

**How it works (high level):**

```java
// SpringApplication.run() eventually calls this:
ServletWebServerApplicationContext.finishRefresh() {
    // 1. Create a Servlet container
    Tomcat tomcat = new Tomcat();
    
    // 2. Configure it
    tomcat.setPort(8080);
    tomcat.setContextPath("/");
    
    // 3. Add the DispatcherServlet
    tomcat.addServlet("/", "dispatcher", new DispatcherServlet());
    tomcat.getServlet("/").addMapping("/*");
    
    // 4. Start it
    tomcat.start();
}
```

**The auto-config bean: `ServletWebServerFactoryAutoConfiguration`:**
```java
@AutoConfiguration
@ConditionalOnClass(ServletRequest.class)
@EnableConfigurationProperties(ServerProperties.class)
public class ServletWebServerFactoryAutoConfiguration {
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean
    static class EmbeddedTomcat {
        @Bean
        public TomcatServletWebServerFactory tomcatServletWebServerFactory(
                TomcatContextCustomizer... customizers) {
            TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
            factory.setTomcatContextCustomizers(Arrays.asList(customizers));
            return factory;
        }
    }
}
```

**Switching servers:** Exclude Tomcat, include Jetty:
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

**FAANG Discussion Point:**  
*"What are the tradeoffs of embedded server vs external server?"*  

**Embedded (Spring Boot):**
- ✅ One artifact (JAR), runs anywhere.
- ✅ Faster deployment (no separate server).
- ✅ Easier to test (real server in tests).
- ❌ Larger JAR (Tomcat bundled).
- ❌ Can't run multiple apps per server instance.

**External (traditional):**
- ✅ Smaller WAR files.
- ✅ Server-level optimizations (shared thread pools).
- ❌ Deployment complexity.
- ❌ "Works on my Tomcat version" issues.

---

<a name="12-architecture-diagram"></a>
## 12. Full Architecture Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                     BROWSER / CLIENT                          │
└─────────────────────────────┬────────────────────────────────┘
                              │ HTTP Request
                              ▼
┌──────────────────────────────────────────────────────────────┐
│        EMBEDDED TOMCAT (started by Spring Boot)               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  NIO Connector (port from server.port)                 │  │
│  └────────────────────────┬───────────────────────────────┘  │
└───────────────────────────┼──────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│              [SERVLET FILTERS]                                │
│  - Spring Security Filter Chain                              │
│  - CORS, Logging, Request ID                                 │
└─────────────────────────────┬────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│         DispatcherServlet (auto-configured)                   │
│  - 9 strategy beans from MVC auto-config                     │
└─────────────────────────────┬────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│         HandlerMapping → HandlerAdapter                      │
│         ↓                                                   │
│   @Controller / @RestController method                       │
└─────────────────────────────┬────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│         @Service / @Component / @Repository                  │
│         ↓                                                   │
│         JPA / JDBC / External API                            │
└─────────────────────────────┬────────────────────────────────┘
                              │
                              ▼
                       JSON / HTTP Response
```

**Spring Boot Internal Architecture:**
```
┌─────────────────────────────────────────────────────────────┐
│                  Your @SpringBootApplication                │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  @SpringBootConfiguration                              │ │
│  │  @EnableAutoConfiguration  ← THE MAGIC                 │ │
│  │  @ComponentScan                                         │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              SpringApplication.run()                        │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  1. Bootstrap                                           │ │
│  │  2. Prepare Environment                                │ │
│  │  3. Print Banner                                       │ │
│  │  4. Create ApplicationContext                          │ │
│  │  5. Prepare Context                                    │ │
│  │  6. Refresh Context  ← Spring Core Lifecycle           │ │
│  │  7. After Refresh (Runners)                            │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│         Auto-Configuration Imports                           │
│  (read from META-INF/spring/...AutoConfiguration.imports)   │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  DispatcherServletAutoConfiguration                    │ │
│  │  JacksonAutoConfiguration                               │ │
│  │  DataSourceAutoConfiguration                            │ │
│  │  JpaRepositoriesAutoConfiguration                       │ │
│  │  WebMvcAutoConfiguration                               │ │
│  │  ActuatorAutoConfiguration                             │ │
│  │  ...                                                   │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│         Embedded Web Server (Tomcat/Jetty/Undertow)         │
│         Started during context refresh                      │
└─────────────────────────────────────────────────────────────┘
```

---

<a name="13-request-to-response"></a>
## 13. From Request to Response: Complete Flow

Let me walk you through the **complete journey** of an HTTP request in a Spring Boot application.

**Scenario:** `GET http://localhost:8080/api/users/42` returns `{"id":42,"name":"John"}`.

**Step 1: JVM starts**
```
java -jar myapp.jar
    ↓
JVM reads MANIFEST.MF → loads JarLauncher
    ↓
JarLauncher loads Start-Class (com.example.MyApp)
    ↓
MyApp.main() is invoked
```

**Step 2: SpringApplication bootstraps**
```java
public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(MyApp.class, args);
}
```

**Step 3: SpringApplication.run() lifecycle**

```
┌──────────────────────────────────────────────────────────┐
│ 1. BOOTSTRAP                                              │
│    - Create BootstrapRegistry                             │
│    - Load META-INF/spring.factories (listeners)           │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ 2. PREPARE ENVIRONMENT                                    │
│    - Determine web environment (servlet)                  │
│    - Load application.properties + YAML                  │
│    - Resolve active profiles                              │
│    - Create StandardServletEnvironment                    │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ 3. PRINT BANNER                                           │
│    - Print Spring logo                                    │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ 4. CREATE APPLICATIONCONTEXT                              │
│    - Web app: AnnotationConfigServletWebServerAppCtx      │
│    - Reads @ComponentScan, finds @SpringBootApplication │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ 5. PREPARE CONTEXT                                        │
│    - Apply ApplicationContextInitializers                │
│    - Fire ApplicationPreparedEvent                        │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ 6. REFRESH CONTEXT (Spring's core lifecycle)              │
│    a. BeanFactory created                                  │
│    b. BeanDefinitions loaded (scan + auto-config)         │
│    c. Beans instantiated (singletons eager)               │
│    d. BeanPostProcessors run (AOP, @Autowired)            │
│    e. Embedded Tomcat started                             │
│    f. DispatcherServlet registered to Tomcat              │
│    g. Fire ApplicationStartedEvent                        │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ 7. AFTER REFRESH                                          │
│    - Run ApplicationRunner, CommandLineRunner            │
│    - Fire ApplicationReadyEvent                           │
└─────────────────────────┬────────────────────────────────┘
                          ↓
        APPLICATION READY — Listening on :8080
```

**Step 4: HTTP request arrives at Tomcat**
```
Browser sends: GET /api/users/42 HTTP/1.1
    ↓
Tomcat NIO connector accepts connection
    ↓
Coyote parses HTTP → creates Request/Response
    ↓
Mapper finds DispatcherServlet (mapped to /)
    ↓
Tomcat allocates a thread from the pool
    ↓
Filter chain runs (Spring Security, CORS, etc.)
    ↓
DispatcherServlet.service() called
```

**Step 5: DispatcherServlet processes request**
```
DispatcherServlet.doDispatch(request, response):
    ↓
1. Multipart check (not multipart, skip)
    ↓
2. HandlerMapping.getHandler() → returns UserController#getUser
    ↓
3. HandlerAdapter.getHandlerAdapter() → RequestMappingHandlerAdapter
    ↓
4. Interceptor.preHandle() (if any)
    ↓
5. HandlerAdapter.handle():
   ├─ Resolve @PathVariable("id") = 42
   ├─ Invoke method via reflection: userService.findById(42L)
   └─ Return User{id=42, name="John"}
    ↓
6. HandlerMethodReturnValueHandler:
   ├─ Sees @ResponseBody (or @RestController)
   ├─ Uses MappingJackson2HttpMessageConverter
   └─ Serializes User → {"id":42,"name":"John"}
    ↓
7. Interceptor.postHandle() (if any)
    ↓
8. Response written: HTTP 200 OK, Content-Type: application/json
    ↓
9. Interceptor.afterCompletion() (cleanup, metrics)
    ↓
Thread returned to Tomcat pool
```

**Step 6: Browser receives response**
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 27

{"id":42,"name":"John"}
```

**FAANG Discussion Point:**  
*"Where does the time go in a request?"* — For a typical REST endpoint:
- ~5% in Spring's `DispatcherServlet` dispatch
- ~10% in Jackson serialization
- ~5% in filter chain (Security)
- ~80% in business logic + DB query

This is why **connection pool tuning**, **JPA query optimization**, and **caching** are usually the highest-impact 
performance improvements.

---

<a name="14-actuator"></a>
## 14. Spring Boot Actuator: Production Features

**Actuator** is Spring Boot's production-readiness module. It adds HTTP endpoints (and JMX) for monitoring and 
managing your app.

**Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Built-in endpoints:**

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | App + component health (DB, disk, etc.) |
| `/actuator/info` | Build info, git info, custom |
| `/actuator/metrics` | Prometheus-format metrics |
| `/actuator/env` | Environment properties (sanitized) |
| `/actuator/loggers` | View/change log levels at runtime |
| `/actuator/threaddump` | JVM thread dump |
| `/actuator/heapdump` | JVM heap dump (hprof) |
| `/actuator/mappings` | All `@RequestMapping` URLs |
| `/actuator/beans` | All Spring beans |
| `/actuator/configprops` | All `@ConfigurationProperties` |

**Configuration (application.yml):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

**Custom health indicator:**
```java
@Component
public class KafkaHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        if (kafkaClient.isConnected()) {
            return Health.up().withDetail("broker", "kafka-1").build();
        }
        return Health.down().withDetail("error", "Disconnected").build();
    }
}
```

**FAANG Discussion Point:**  
*"What's the difference between liveness and readiness probes?"*  
- **Liveness** — "Am I alive?" If failing, Kubernetes **restarts** the pod.
- **Readiness** — "Am I ready to serve traffic?" If failing, Kubernetes **removes the pod from the load balancer** 
but doesn't restart.
Use liveness for unrecoverable failures; readiness for "temporarily not ready" (e.g., warming up caches).

---

<a name="15-startup-phases"></a>
## 15. Spring Boot Startup Phases (The Full Timeline)

Here's the **complete startup sequence** with all event hooks:

```
T=0ms: java -jar myapp.jar
    ↓
JVM initialization
    ↓
JarLauncher loads Start-Class
    ↓
main() invoked
    ↓
SpringApplication.run() begins
    ↓
┌──────────────────────────────────────────────────────────┐
│ Phase 1: BOOTSTRAP                                        │
│                                                          │
│ - Create BootstrapRegistry                              │
│ - Load META-INF/spring.factories                        │
│   (listeners, initializers, EnvironmentPostProcessors)  │
│ - Fire ApplicationStartingEvent                         │
│   ← Listeners: log startup, set MDC context            │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ Phase 2: ENVIRONMENT                                     │
│                                                          │
│ - Create StandardEnvironment                            │
│ - Add PropertySources:                                  │
│   • application.properties                              │
│   • application-{profile}.properties                    │
│   • environment variables                               │
│   • command-line args                                   │
│ - Run EnvironmentPostProcessors                         │
│   (e.g., load .env files, decrypt secrets)              │
│ - Fire ApplicationEnvironmentPreparedEvent              │
│   ← Listeners: log environment, validate               │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ Phase 3: CONTEXT PREP                                    │
│                                                          │
│ - Print banner                                          │
│ - Create ApplicationContext (servlet or reactive)       │
│ - Apply ApplicationContextInitializers                  │
│ - Fire ApplicationPreparedEvent                         │
│   ← Listeners: add early beans                         │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ Phase 4: CONTEXT REFRESH  ← Most beans created here     │
│                                                          │
│ - BeanFactory created                                   │
│ - BeanDefinitions loaded:                               │
│   • @ComponentScan results                              │
│   • @Configuration classes                              │
│   • Auto-configuration classes                          │
│   • @Bean methods                                       │
│                                                          │
│ - BeanPostProcessors instantiated EARLY                │
│   (AutowiredAnnotationBeanPostProcessor, etc.)         │
│                                                          │
│ - All singleton beans instantiated                      │
│   (in dependency order)                                 │
│                                                          │
│ - BeanPostProcessors run for each bean                  │
│   (resolve @Autowired, create AOP proxies)              │
│                                                          │
│ - SmartInitializingSingleton.afterSingletonsInstantiated │
│                                                          │
│ - EMBEDDED WEB SERVER STARTED ← Tomcat on :8080        │
│   • TomcatServletWebServerFactory creates Tomcat        │
│   • DispatcherServlet registered                        │
│   • Connector opens port                                │
│                                                          │
│ - Fire ApplicationStartedEvent                          │
│   ← Listeners: log "Started in 2.345 seconds"          │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│ Phase 5: AFTER REFRESH                                   │
│                                                          │
│ - ApplicationRunner.run()                               │
│ - CommandLineRunner.run()                               │
│   (in @Order)                                           │
│                                                          │
│ - Fire ApplicationReadyEvent                            │
│   ← Listeners: notify "app is serving traffic"        │
└─────────────────────────┬────────────────────────────────┘
                          ↓
                  APPLICATION READY
```

**Total startup time** is typically 1-5 seconds for a small Spring Boot app. Can be 10+ seconds for large apps 
with heavy auto-config.

**FAANG Discussion Point:**  
*"How do you speed up Spring Boot startup?"* — Common techniques:
1. **Lazy initialization:** `spring.main.lazy-initialization=true` (creates beans on first use).
2. **Component scanning:** narrow base packages (`scanBasePackages`).
3. **Exclude unused auto-config:** `@SpringBootApplication(exclude = { ... })`.
4. **Slim JARs:** use `spring-boot-thin-launcher` or `spring-boot-loader-tools`.
5. **Native compilation:** Spring Native + GraalVM (sub-100ms startup).
6. **AOT (Ahead-of-Time) processing:** generates pre-computed bean definitions.

---

<a name="16-conditional-beans"></a>
## 16. Conditional Beans: The @Conditional Family

Auto-configuration uses `@Conditional` annotations to decide **whether** a bean should be created.

**The @Conditional family:**

| Annotation | Condition |
|---|---|
| `@ConditionalOnClass` | Class is on the classpath |
| `@ConditionalOnMissingClass` | Class is NOT on the classpath |
| `@ConditionalOnBean` | Bean of type exists |
| `@ConditionalOnMissingBean` | Bean of type does NOT exist |
| `@ConditionalOnProperty` | Property matches value |
| `@ConditionalOnResource` | Resource (file) exists on classpath |
| `@ConditionalOnWebApplication` | App is a web app |
| `@ConditionalOnNotWebApplication` | App is NOT a web app |
| `@ConditionalOnExpression` | SpEL expression evaluates to true |
| `@ConditionalOnJava` | JVM version matches |
| `@ConditionalOnSingleCandidate` | Exactly one bean of type |
| `@ConditionalOnCloudPlatform` | Running on K8s/Cloud Foundry |

**Example: DataSourceAutoConfiguration logic:**
```java
@AutoConfiguration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnMissingBean(type = "io.r2dbc.spi.ConnectionFactory")
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(DataSource.class)
    static class EmbeddedDatabaseConfiguration {
        // creates embedded DataSource from properties
    }
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "spring.datasource.type")
    static class PooledDataSourceConfiguration {
        // creates pooled DataSource if type is specified
    }
}
```

**FAANG Discussion Point:**  
*"How do you debug why an auto-config didn't apply?"* — Use the **conditions report**:
1. Run with `--debug` flag.
2. Check `/actuator/conditions` endpoint.
3. Or programmatically: `applicationContext.getBean(ReportableApplicationContext.class).getBeanFactory()...`

---

<a name="17-custom-autoconfig"></a>
## 17. Custom Auto-Configuration: Writing Your Own Starter

You can write your own **Starter** + **Auto-Configuration** to package reusable Spring configuration for your team 
or the community.

**The two modules:**
1. **Autoconfigure module** — the `@AutoConfiguration` class + `META-INF/spring/...AutoConfiguration.imports` 
file.
2. **Starter module** — empty POM that pulls in the autoconfigure module + required deps.

**Step 1: Create the auto-configuration class:**
```java
@AutoConfiguration
@ConditionalOnClass(MyServiceClient.class)
@EnableConfigurationProperties(MyServiceProperties.class)
public class MyServiceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyServiceClient myServiceClient(MyServiceProperties properties) {
        return new MyServiceClient(properties.getEndpoint(), properties.getApiKey());
    }
}
```

**Step 2: Create the properties class:**
```java
@ConfigurationProperties(prefix = "myservice")
public class MyServiceProperties {
    private String endpoint = "https://api.myservice.com";
    private String apiKey;
    // getters and setters
}
```

**Step 3: Register in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:**
```
com.example.myservice.autoconfigure.MyServiceAutoConfiguration
```

**Step 4: Create the starter POM (in a separate module):**
```xml
<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>myservice-autoconfigure</artifactId>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>myservice-client</artifactId>
    </dependency>
</dependencies>
```

**Step 5: Users add the starter:**
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>myservice-spring-boot-starter</artifactId>
</dependency>
```

**FAANG Discussion Point:**  
*"Why two modules (autoconfigure + starter)?"* — Because users may want to use the library **without** 
auto-configuration (e.g., in a non-Spring app). The autoconfigure module contains the Spring-specific code. The 
starter is just a "convenience" entry point.

---

<a name="18-faang-points"></a>
## 18. FAANG Discussion Points (Top 20)

### 1. **Why Auto-Configuration doesn't conflict with user config**
`@ConditionalOnMissingBean` ensures Spring Boot only creates a bean if the user hasn't already.

### 2. **Externalized config precedence**
CLI args > env vars > application.properties. This enables 12-factor deployment.

### 3. **Embedded server vs external**
Spring Boot's embedded server enables 12-factor app compliance and faster CI/CD.

### 4. **`@SpringBootApplication` is not magic**
It's just `@Configuration + @EnableAutoConfiguration + @ComponentScan` combined.

### 5. **Startup time optimization**
Lazy init, narrow scanning, exclude auto-config, AOT/native.

### 6. **Actuator endpoints security**
Don't expose `/actuator/env` publicly — it leaks secrets. Use Spring Security to restrict.

### 7. **Profile-specific properties**
`application-{profile}.properties` overlays `application.properties`. Useful for env-specific config.

### 8. **Property binding to POJOs**
`@ConfigurationProperties` is type-safe. Use it over `@Value` for complex config.

### 9. **Conditional auto-config**
Spring Boot uses `@ConditionalOn*` to make smart defaults. Your overrides always win.

### 10. **Runners (ApplicationRunner, CommandLineRunner)**
Use for post-startup tasks: warm up caches, send startup notifications.

### 11. **Spring Boot vs Spring MVC**
Spring Boot is a meta-framework that includes Spring MVC. They're not in opposition.

### 12. **Liveness vs Readiness probes**
Liveness → restart pod. Readiness → remove from LB. Different signals for different needs.

### 13. **Custom auto-configuration**
You can write your own starter and publish it to Maven Central.

### 14. **Bean lifecycle in Spring Boot**
`refresh()` is the heart. BeanPostProcessors run during refresh.

### 15. **Why `@ConfigurationProperties` is preferred over `@Value`**
Type safety, validation, complex types, lists, maps, durations.

### 16. **Spring Boot's class loader**
Custom `LaunchedURLClassLoader` reads nested JARs from the executable JAR.

### 17. **`spring-boot-starter` vs `spring-boot-starter-web`**
Base starter (logging, properties) vs web starter (MVC, Tomcat, Jackson).

### 18. **Auto-config reporting**
`/actuator/conditions` shows why each auto-config matched or didn't.

### 19. **Bean validation in properties**
`@Validated` on `@ConfigurationProperties` class enables JSR-380 validation.

### 20. **Spring Boot 3 changes**
- Jakarta EE 9+ (jakarta.* packages).
- Native compilation via Spring Native / GraalVM.
- ProblemDetails (RFC 7807) for error responses.

---

<a name="19-followup"></a>
## 19. Top 20 Follow-up Q&A

### **Q1: How does Spring Boot differ from Spring Cloud?**
**A:** Spring Boot is for **single apps**. Spring Cloud is for **distributed systems** (config server, service 
discovery, circuit breakers, distributed tracing). Spring Cloud builds on Spring Boot.

### **Q2: What is the difference between `application.yml` and `application.properties`?**
**A:** Same data, different format. YAML supports hierarchies; properties is flat. YAML is more readable for 
nested config. Both are loaded by Spring Boot.

### **Q3: Can you use Spring Boot without Spring MVC?**
**A:** Yes. Exclude `spring-boot-starter-web` and use `spring-boot-starter-webflux` (reactive) or no web starter 
at all (CLI/batch apps).

### **Q4: What is the difference between `mvn spring-boot:run` and `java -jar`?**
**A:** `spring-boot:run` uses the Maven classpath (faster dev cycle). `java -jar` uses the packaged executable JAR 
(production).

### **Q5: How do you enable HTTPS in Spring Boot?**
**A:** Configure `server.ssl.*` properties:
```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: myapp
```

### **Q6: What is `spring-boot-devtools`?**
**A:** DevTools auto-restarts your app when classpath changes. It uses two classloaders (base + restart) to reload 
only your code, not third-party JARs. Also enables LiveReload (browser auto-refresh).

### **Q7: Can you deploy a Spring Boot WAR to external Tomcat?**
**A:** Yes — extend `SpringBootServletInitializer`:
```java
@SpringBootApplication
public class MyApp extends SpringBootServletInitializer { }
```
Set packaging to WAR in `pom.xml`. The embedded Tomcat won't activate; the external server runs it.

### **Q8: What is the `@Conditional` annotation and why is it important?**
**A:** `@Conditional` (and its subclasses) let beans be created only if certain conditions are met (classpath, 
properties, existing beans). It's how Spring Boot makes smart auto-config decisions.

### **Q9: How do you handle graceful shutdown in Spring Boot?**
**A:** Spring Boot 2.3+ supports graceful shutdown:
```yaml
server:
  shutdown: graceful
```
Spring stops accepting new requests, waits for in-flight requests to complete (default 30s), then closes the 
context.

### **Q10: What is the difference between `@Component` and `@Bean`?**
**A:** `@Component` is on a class (auto-discovered). `@Bean` is on a method inside `@Configuration` (explicit 
factory). `@Bean` is used for third-party classes or complex construction.

### **Q11: Can you use Spring Boot with Kotlin?**
**A:** Yes! `spring-boot-starter` works with Kotlin. There's also `kotlin-spring` compiler plugin to make Kotlin 
classes open (needed for CGLIB proxying).

### **Q12: How do you do health checks for Kubernetes?**
**A:** Enable Actuator's probes:
```yaml
management:
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```
Spring exposes `/actuator/health/liveness` and `/actuator/health/readiness`. Configure these in your K8s 
deployment.

### **Q13: What is the `@Profile` annotation's relationship with auto-config?**
**A:** Auto-configurations can use `@ConditionalOnProperty(name="spring.profiles.active", havingValue="prod")` to 
apply only in specific profiles.

### **Q14: How does Spring Boot handle log levels?**
**A:** Configure in `application.yml`:
```yaml
logging:
  level:
    com.example: DEBUG
    org.springframework: INFO
```
Or use `/actuator/loggers` to change at runtime.

### **Q15: What is the Spring Boot CLI?**
**A:** A command-line tool for running Spring Boot Groovy scripts. Useful for quick prototyping:
```
$ spring run app.groovy
```

### **Q16: How do you disable a specific auto-configuration?**
**A:** Use `@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })` or in `application.yml`:
```yaml
spring:
  autoconfigure:
    exclude: org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

### **Q17: What is the `@SpringBootTest` annotation?**
**A:** Loads the full application context for integration tests. Use `@WebMvcTest(MyController.class)` for slice 
tests that load only the web layer.

### **Q18: How does Spring Boot 3 differ from Spring Boot 2?**
**A:** Major changes:
- Jakarta EE 9+ (packages: `jakarta.*`).
- Java 17+ baseline.
- Spring Framework 6.
- Native compilation (Spring Native / GraalVM).
- ProblemDetails (RFC 7807) for error responses.
- `META-INF/spring.factories` → 
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### **Q19: What is Spring Boot's AOT (Ahead-of-Time) compilation?**
**A:** AOT generates pre-computed bean definitions, reflection metadata, etc., at build time. Used for **native 
images** (GraalVM). Startup: 1ms vs 1000ms.

### **Q20: How do you package a Spring Boot app as a Docker image?**
**A:** Two approaches:
1. **Dockerfile** with `eclipse-temurin:17-jre` base image, copy the executable JAR.
2. **Cloud Native Buildpacks** (`mvn spring-boot:build-image`) — creates an optimized image automatically.

---

<a name="20-summary"></a>
## 20. Summary: The One-Page Mental Model

```
┌──────────────────────────────────────────────────────────┐
│         EXECUTABLE JAR (myapp.jar)                       │
│  ┌──────────────────────────────────────────────────┐   │
│  │  MANIFEST.MF → Main-Class: JarLauncher           │   │
│  │  → loads Start-Class (MyApp)                     │   │
│  │  → SpringApplication.run()                       │   │
│  └──────────────────────────────────────────────────┘   │
└──────────────────────────┬───────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────┐
│         SPRING BOOT STARTUP (5 phases)                   │
│  1. Bootstrap → Environment → Context Prep →             │
│  2. Refresh (beans + auto-config + embedded Tomcat) →   │
│  3. Runners → ApplicationReadyEvent                      │
└──────────────────────────┬───────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────┐
│         EMBEDDED TOMCAT (port 8080)                      │
│  - NIO Connector, thread pool, request parsing          │
└──────────────────────────┬───────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────┐
│         HTTP REQUEST → RESPONSE                          │
│  1. Tomcat NIO acceptor → thread allocated              │
│  2. Filter chain (Security, CORS)                       │
│  3. DispatcherServlet (front controller)                │
│     - HandlerMapping → HandlerAdapter → @Controller     │
│     - HttpMessageConverter (Jackson) → JSON             │
│  4. Response written, thread returned to pool           │
└──────────────────────────────────────────────────────────┘
```

**The one-line summary:** *Spring Boot is Spring + auto-configuration + starters + embedded server + production 
features, designed to get you from `main()` to "Hello World" in 5 minutes, and to production in 5 days.*

---