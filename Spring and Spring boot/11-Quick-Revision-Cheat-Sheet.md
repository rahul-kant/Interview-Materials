# Spring & Spring Boot - Quick Revision Cheat Sheet

## 🗺️ Quick Revision Roadmap

> [!TIP]
> **Interview Pro-Tip: "The 5 Most Common Spring Questions"**
> 1. **Explain IoC and DI.** (The "What" and "How").
> 2. **What is the Bean Lifecycle?** (The "When").
> 3. **Spring MVC Flow.** (The "Where" - DispatcherServlet).
> 4. **How does @Transactional work?** (The "Magic" - AOP Proxies).
> 5. **Spring Boot Auto-Configuration.** (The "Why" - Opinionated defaults).

### 🔍 Deep Dive: Last-Minute Prep Strategy
If you have only 10 minutes:
1. **Review Annotations**: Know the difference between `@Component`, `@Service`, `@Repository`, and `@Controller`.
2. **Review Scopes**: Singleton vs Prototype.
3. **Review Boot Essentials**: Starters, Actuator, and Auto-config.
4. **Review JPA Pitfalls**: N+1 problem and `@Transactional` rollback rules.

### 🛠️ Complex Example: The "Master Config"
```java
@Configuration
@EnableAsync
@EnableCaching
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class MasterConfig {
    // This class enables the 5 most important Spring features in one place!
    
    @Bean
    @Primary
    public DataSource primaryDataSource() { ... }
    
    @Bean
    @Qualifier("secondary")
    public DataSource secondaryDataSource() { ... }
}
```

## 🚀 Core Spring
- **IoC (Inversion of Control)**: Spring manages object creation and lifecycle.
- **DI (Dependency Injection)**: Objects get dependencies from outside.
  - **Constructor** (Best): For mandatory dependencies.
  - **Setter**: For optional dependencies.
  - **Field**: Avoid (hard to test).
- **Bean Scopes**:
  - `Singleton` (Default): One instance per container.
  - `Prototype`: New instance every time.
  - `Request/Session`: Web-only.
- **Bean Lifecycle**:
  - `Constructor` -> `DI` -> `@PostConstruct` -> `Ready` -> `@PreDestroy`.

## 🍃 Spring Boot
- **Auto-Configuration**: Scans classpath & configures beans automatically (`@EnableAutoConfiguration`).
- **Starters**: Bundled dependencies (e.g., `spring-boot-starter-web`).
- **Actuator**: Monitoring endpoints (`/health`, `/metrics`).
- **Embedded Server**: Tomcat (default), Jetty, Undertow.
- **Properties**: `application.properties` or `application.yml`.

## 🌐 REST APIs
- **@RestController**: `@Controller` + `@ResponseBody`.
- **Methods**: `GET` (Read), `POST` (Create), `PUT` (Replace), `PATCH` (Update), `DELETE` (Remove).
- **Status Codes**:
  - `200 OK`, `201 Created`
  - `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`
  - `500 Server Error`
- **Exception Handling**: `@RestControllerAdvice` + `@ExceptionHandler`.

## 💾 Spring Data JPA
- **Interfaces**: `JpaRepository`, `CrudRepository`.
- **N+1 Problem**: Fetching N entities triggers N+1 queries. Fix: `JOIN FETCH`.
- **Transactions**: `@Transactional` (Atomicity).
  - **Propagation**: `REQUIRED` (Default), `REQUIRES_NEW`.
  - **Isolation**: `READ_COMMITTED`, `REPEATABLE_READ`.

## 🔒 Spring Security
- **Authentication**: Who are you? (Login).
- **Authorization**: What can you do? (Permissions).
- **Filters**: Chain of filters intercepts requests.
- **JWT**: Stateless auth token (Header.Payload.Signature).
- **CSRF**: Attack forcing unwanted actions. Enabled by default.

## ☁️ Microservices
- **Service Discovery**: Eureka (Phonebook for services).
- **API Gateway**: Single entry point, routing, auth.
- **Circuit Breaker**: Resilience4j (Stop cascading failures).
- **Config Server**: Centralized configuration.
- **SAGA**: Distributed transactions (Choreography vs Orchestration).

## ⚡ Advanced & Best Practices
- **AOP**: Cross-cutting concerns (Logging, Tx). `@Aspect`, `@Before`, `@Around`.
- **Profiles**: `@Profile("dev")` for environment specific config.
- **Testing**: `@SpringBootTest` (Integration), `@WebMvcTest` (Controller only), `@MockBean`.
- **Design Patterns**: Singleton (Beans), Factory (BeanFactory), Proxy (AOP), Template (JdbcTemplate).

---
*Use this for last-minute revision before entering the interview room!*
