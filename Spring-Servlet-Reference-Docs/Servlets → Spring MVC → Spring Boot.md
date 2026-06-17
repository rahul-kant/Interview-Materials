# 🚀 Servlets → Spring MVC → Spring Boot: The Complete Correlated Deep Dive

> _From "What is a Servlet?" to "How does Spring Boot embed Tomcat, register `DispatcherServlet`, and serve a REST 
request — from `main()` to the HTTP response?"_

---

## 📑 Table of Contents

1. [The 10,000-Foot View: How It All Fits Together](#1-10000ft)
2. [The Core Analogy: Restaurant Kitchen](#2-analogy)
3. [J2EE Servlets: The Foundation](#3-servlet-foundation)
4. [How Spring Sits on Top of Servlets](#4-spring-on-servlets)
5. [How Spring Boot Sits on Top of Spring](#5-springboot-on-spring)
6. [The Three-Layer Architecture: Servlet → Spring MVC → Spring Boot](#6-three-layer)
7. [End-to-End Request Flow: Correlated](#7-end-to-end)
8. [How Spring Boot Embeds Tomcat and Wires DispatcherServlet](#8-embed-tomcat)
9. [The 9-Step Spring MVC Flow, Correlated with Servlet Concepts](#9-9-step-correlated)
10. [Auto-Configuration: How Spring Boot Wires the Servlet World](#10-autoconfig)
11. [Filters vs Interceptors: The Correlated View](#11-filters-vs-interceptors)
12. [Session, Async, and Concurrency: All Three Layers](#12-session-async)
13. [Architecture & Flow Diagrams (10+ ASCII)](#13-diagrams)
14. [FAANG Discussion Points (Top 20)](#14-faang)
15. [Top 20 Follow-up Q&A](#15-followup)
16. [Staff-Engineer Level: Tuning, Pitfalls, Production](#16-staff)

---

<a name="1-10000ft"></a>
## 1. The 10,000-Foot View: How It All Fits Together

Here's the **single mental model** that ties everything:

```
┌──────────────────────────────────────────────────────────────┐
│              YOUR CODE (@RestController, @Service)           │
├──────────────────────────────────────────────────────────────┤
│  SPRING BOOT  (auto-config, starters, embedded server)      │
├──────────────────────────────────────────────────────────────┤
│  SPRING MVC  (DispatcherServlet, HandlerMapping, @Controller)│
├──────────────────────────────────────────────────────────────┤
│  SERVLET API  (HttpServlet, Filter, Listener, Session)      │
├──────────────────────────────────────────────────────────────┤
│  SERVLET CONTAINER  (Tomcat: thread pool, HTTP parsing)     │
├──────────────────────────────────────────────────────────────┤
│  JVM / OS  (TCP sockets, file system, OS threads)            │
└──────────────────────────────────────────────────────────────┘
```

**The key insight:** Spring Boot is **NOT a replacement** for Servlets. It's a **productivity layer** that:
- **Embeds** a Servlet container (Tomcat) inside your JAR.
- **Auto-registers** `DispatcherServlet` and its 9 strategy beans.
- **Auto-wires** filters, exception handlers, message converters, static resources.
- **Configures** everything you'd otherwise set up in `web.xml`.

**The chain of "who does what" for a single HTTP request:**

| Step | Layer | What Happens |
|---|---|---|
| 1 | **JVM** | `java -jar app.jar` starts |
| 2 | **Spring Boot** | `SpringApplication.run()` bootstraps |
| 3 | **Spring Boot** | Auto-config starts embedded Tomcat on port 8080 |
| 4 | **Spring Boot** | Auto-config registers `DispatcherServlet` to `/` |
| 5 | **Tomcat** | Accepts TCP connection, allocates worker thread |
| 6 | **Servlet Filters** | Security, CORS, logging run |
| 7 | **DispatcherServlet** | Receives the request (it IS a Servlet!) |
| 8 | **Spring MVC** | HandlerMapping finds `@Controller` method |
| 9 | **Spring MVC** | HandlerAdapter invokes method via reflection |
| 10 | **Your Code** | Business logic runs |
| 11 | **Spring MVC** | HttpMessageConverter (Jackson) serializes to JSON |
| 12 | **Tomcat** | Writes HTTP response back to socket |

**FAANG Discussion Point:**  
*"If Spring Boot auto-configures everything, do I still need to know Servlets?"* — **Absolutely yes.** When things 
go wrong (memory leaks, thread pool exhaustion, filter ordering, async issues), you need to understand the Servlet 
foundation. Spring Boot hides the complexity but doesn't eliminate it.

---

<a name="2-analogy"></a>
## 2. The Core Analogy: Restaurant Kitchen

To make this stick, let's use a **restaurant analogy** end-to-end:

```
┌──────────────────────────────────────────────────────────────┐
│                      THE RESTAURANT                          │
│                                                              │
│  Customer (Browser)                                          │
│      ↓ "I'd like the user with ID 42"                        │
│  Host Stand (Tomcat Connector)                               │
│      ↓ Greets, assigns a table                               │
│  Waiter (Tomcat Worker Thread)                               │
│      ↓ Takes order to the kitchen                            │
│  Expediter (DispatcherServlet)                               │
│      ↓ Routes to the right chef                              │
│  Head Chef (HandlerMapping + HandlerAdapter)                 │
│      ↓ Decides which station handles it                      │
│  Station Chef (@Controller method)                           │
│      ↓ Cooks the dish                                        │
│  Sous Chef (Service layer, @Service)                         │
│      ↓ Preps ingredients                                     │
│  Pantry (Database, @Repository)                              │
│      ↓ Provides raw materials                                │
│  Plating (HttpMessageConverter / Jackson)                    │
│      ↓ Plates the food (JSON)                                │
│  Waiter returns the dish to the customer                     │
└──────────────────────────────────────────────────────────────┘
```

**The correlation:**

| Restaurant | Servlet/Spring World |
|---|---|
| Customer | Browser (HTTP client) |
| Host stand | Tomcat Connector (accepts TCP) |
| Table assignment | Thread pool (worker thread) |
| Waiter | Tomcat worker thread |
| Order ticket | HttpServletRequest |
| Expediter | **DispatcherServlet** (front controller) |
| Recipe book | **HandlerMapping** (URL → method) |
| Head chef | **HandlerAdapter** (invokes the right chef) |
| Station chef | **@Controller method** |
| Sous chef | **@Service / @Component** |
| Pantry | **@Repository** (database) |
| Plating station | **HttpMessageConverter** (Jackson) |
| Dish | HTTP response (JSON) |
| Health inspector | **Filter chain** (security, logging) |
| Kitchen manager | **ApplicationContext** (Spring container) |
| Restaurant chain | **Spring Boot auto-config** (standardizes kitchens) |

---

<a name="3-servlet-foundation"></a>
## 3. J2EE Servlets: The Foundation

Let's start from the **bottom** and build up.

### What Is a Servlet?

A **Servlet** is a Java class that handles HTTP requests. The API defines a contract between **your code** and 
**any web server**.

```java
public interface Servlet {
    void init(ServletConfig config);
    void service(ServletRequest req, ServletResponse res);
    void destroy();
}
```

In practice, you extend `HttpServlet`:

```java
public class HelloServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.getWriter().write("Hello!");
    }
}
```

### The Servlet Container (Tomcat)

The container is a **Java program** that:
- Listens on a TCP port (e.g., 8080).
- Parses HTTP bytes.
- Maps URLs to Servlets.
- Manages the Servlet lifecycle (init → service → destroy).
- Provides threading, sessions, and resource pooling.

### Servlet Lifecycle

```
1. Constructor (no-arg, public)
    ↓
2. init(ServletConfig)  ← called ONCE
    ↓
3. service(req, res)    ← called for EVERY request
    ↓
4. destroy()            ← called ONCE on shutdown
```

**Critical:** A single Servlet instance handles **many concurrent requests** via the thread pool. The Servlet is 
**not thread-safe** — use local variables, not instance fields.

---

<a name="4-spring-on-servlets"></a>
## 4. How Spring Sits on Top of Servlets

Spring MVC is **built ON TOP of** the Servlet API. It doesn't replace Servlets — it **wraps** them.

### The Key Idea: DispatcherServlet IS a Servlet

```java
public class DispatcherServlet extends FrameworkServlet {
    // ...
}

public abstract class FrameworkServlet extends HttpServletBean {
    // ...
}

public abstract class HttpServletBean extends HttpServlet implements EnvironmentAware {
    // ...
}
```

**`DispatcherServlet` is literally a `HttpServlet` subclass.** It overrides `doService()` to implement Spring's 
MVC flow.

### What Spring MVC Adds on Top of Servlets

| Servlet API Feature | Spring MVC Enhancement |
|---|---|
| `HttpServlet` with `doGet`/`doPost` | `@GetMapping` / `@PostMapping` annotations |
| Manual URL mapping in `web.xml` | `RequestMappingHandlerMapping` (annotation-based) |
| Manual `RequestDispatcher.forward()` | `ViewResolver` (string → view) |
| Manual `getWriter().write(json)` | `HttpMessageConverter` (Jackson auto-serialization) |
| Manual exception try/catch | `@ExceptionHandler` / `@ControllerAdvice` |
| Manual filter chain in `web.xml` | `HandlerInterceptor` (Spring-aware) |
| `web.xml` configuration | Java config (`@Configuration`) + annotations |

### How Spring MVC Wires the Servlet World (Pre-Boot)

Before Spring Boot, you had to manually configure everything:

**`web.xml`:**
```xml
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:spring-mvc.xml</param-value>
    </init-param>
</servlet>
<servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

**`spring-mvc.xml`:**
```xml
<context:component-scan base-package="com.example"/>
<mvc:annotation-driven/>
<bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    <property name="prefix" value="/WEB-INF/views/"/>
    <property name="suffix" value=".jsp"/>
</bean>
```

**That's a LOT of XML.** Spring Boot's auto-configuration eliminates all of it.

---

<a name="5-springboot-on-spring"></a>
## 5. How Spring Boot Sits on Top of Spring

Spring Boot is **a layer on top of Spring Framework**. It uses Spring's core features (IoC, AOP, MVC) and adds 
**auto-configuration** + **convention over configuration**.

### The Magic: Auto-Configuration

Spring Boot scans `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and applies 
**conditional bean definitions**.

**Example: `DispatcherServletAutoConfiguration`**
```java
@AutoConfiguration
@ConditionalOnClass DispatcherServlet.class
public class DispatcherServletAutoConfiguration {
    
    @Configuration(proxyBeanMethods = false)
    @Conditional(DefaultDispatcherServletCondition.class)
    @ConditionalOnClass(ServletRegistration.class)
    @EnableConfigurationProperties(WebMvcProperties.class)
    static class DispatcherServletConfiguration {
        
        @Bean
        public DispatcherServlet dispatcherServlet(WebMvcProperties properties) {
            DispatcherServlet ds = new DispatcherServlet();
            ds.setDispatchOptionsRequest(properties.isDispatchOptionsRequest());
            // ... more config
            return ds;
        }
    }
    
    @Configuration(proxyBeanMethods = false)
    @Conditional(DispatcherServletRegistrationCondition.class)
    @ConditionalOnClass(ServletRegistration.class)
    @EnableConfigurationProperties(DispatcherServletRegistrationProperties.class)
    static class DispatcherServletRegistrationConfiguration {
        
        @Bean
        public DispatcherServletRegistrationBean<DispatcherServlet> 
                dispatcherServletRegistration(DispatcherServlet dispatcherServlet) {
            DispatcherServletRegistrationBean<DispatcherServlet> registration = 
                new DispatcherServletRegistrationBean<>(dispatcherServlet);
            registration.setLoadOnStartup(1);  // ← eager init!
            registration.addUrlMappings("/");  // ← map to all URLs!
            return registration;
        }
    }
}
```

**What this does automatically:**
1. Creates a `DispatcherServlet` bean.
2. Registers it to Tomcat with `loadOnStartup=1` (eager).
3. Maps it to `/` (catches all requests).
4. All of this happens **just by adding `spring-boot-starter-web`** to your classpath.

### The Full Spring Boot Stack

```
┌────────────────────────────────────────────────────────────┐
│  Your @SpringBootApplication                              │
│  (triggers @ComponentScan + @EnableAutoConfiguration)     │
└────────────────────┬───────────────────────────────────────┘
                     ↓
┌────────────────────────────────────────────────────────────┐
│  SpringApplication.run()                                  │
│  1. Bootstrap                                             │
│  2. Prepare Environment (application.properties)          │
│  3. Create ApplicationContext                             │
│  4. Apply Auto-Configurations:                            │
│     - DispatcherServletAutoConfiguration ← registers DS  │
│     - ServletWebServerFactoryAutoConfiguration ← Tomcat  │
│     - JacksonAutoConfiguration ← JSON                     │
│     - WebMvcAutoConfiguration ← MVC defaults              │
│     - DataSourceAutoConfiguration ← DB connection pool   │
│  5. Refresh Context (all beans created)                   │
│  6. Start embedded Tomcat                                 │
│  7. Application Ready                                     │
└────────────────────────────────────────────────────────────┘
```

---

<a name="6-three-layer"></a>
## 6. The Three-Layer Architecture: Servlet → Spring MVC → Spring Boot

Here's the **complete picture** showing how each layer adds value:

```
┌──────────────────────────────────────────────────────────────┐
│  LAYER 3: SPRING BOOT                                        │
│  - Auto-configures everything below                         │
│  - Embedded Tomcat (no external server)                      │
│  - application.properties for externalized config            │
│  - Production features (Actuator, health checks)             │
│  - Starters (curated dependency bundles)                     │
├──────────────────────────────────────────────────────────────┤
│  LAYER 2: SPRING MVC                                         │
│  - DispatcherServlet (front controller)                      │
│  - @Controller, @RestController, @RequestMapping             │
│  - HandlerMapping, HandlerAdapter, ViewResolver              │
│  - HttpMessageConverter (Jackson for JSON)                   │
│  - HandlerInterceptor (pre/post processing)                  │
│  - @ExceptionHandler, @ControllerAdvice                     │
├──────────────────────────────────────────────────────────────┤
│  LAYER 1: SERVLET API                                        │
│  - HttpServlet (base class for DispatcherServlet)            │
│  - HttpServletRequest, HttpServletResponse                  │
│  - Filter (pre/post processing — runs BEFORE DispatcherServlet)│
│  - ServletContext, ServletConfig                            │
│  - HttpSession (server-side state)                           │
│  - @WebServlet, @WebFilter, @WebListener (annotations)      │
├──────────────────────────────────────────────────────────────┤
│  LAYER 0: SERVLET CONTAINER (Tomcat)                         │
│  - NIO Connector (TCP/HTTP)                                 │
│  - Thread pool (default 200)                                 │
│  - Request parsing (Coyote)                                 │
│  - Container hierarchy (Server → Service → Context → Wrapper)│
│  - Session management                                       │
└──────────────────────────────────────────────────────────────┘
```

### How Each Layer Talks to the Next

| From | To | Mechanism |
|---|---|---|
| Browser | Tomcat | TCP/HTTP bytes |
| Tomcat | Filter | `Filter.doFilter(req, res, chain)` |
| Filter | DispatcherServlet | `chain.doFilter(req, res)` |
| DispatcherServlet | HandlerMapping | `getHandler(request)` |
| HandlerMapping | HandlerAdapter | `getHandlerAdapter(handler)` |
| HandlerAdapter | @Controller | Java reflection (`method.invoke()`) |
| @Controller | @Service | `@Autowired` (Spring IoC) |
| @Service | Database | JDBC/JPA (connection pool) |
| Return value | HttpMessageConverter | `@ResponseBody` triggers Jackson |
| Response | Tomcat | `response.getWriter().write()` |
| Tomcat | Browser | HTTP response bytes |

---

<a name="7-end-to-end"></a>
## 7. End-to-End Request Flow: Correlated

Let's trace **one request** through all three layers, correlating each step.

**Scenario:** `GET http://localhost:8080/api/users/42` returns `{"id":42,"name":"John"}`.

### Phase 1: Application Startup (happens ONCE)

```
Step 1: JVM starts
   └─ java -jar myapp.jar
   └─ Loads MANIFEST.MF → Main-Class: JarLauncher
   
Step 2: Spring Boot bootstraps
   └─ SpringApplication.run() begins
   └─ Reads application.properties
   └─ Creates ApplicationContext
   
Step 3: Auto-configuration fires
   └─ ServletWebServerFactoryAutoConfiguration
      → Creates TomcatServletWebServerFactory
   └─ DispatcherServletAutoConfiguration
      → Creates DispatcherServlet bean
      → Registers it to Tomcat with URL mapping "/"
   
Step 4: Spring MVC initializes
   └─ Component scan finds @RestController classes
   └─ RequestMappingHandlerMapping registers all @GetMapping URLs
   └─ RequestMappingHandlerAdapter is created
   └─ Jackson HttpMessageConverter is registered
   
Step 5: Tomcat starts
   └─ Opens port 8080
   └─ ApplicationReadyEvent fires
   └─ App is ready to accept requests
```

### Phase 2: Request Processing (happens for EVERY request)

```
┌──────────────────────────────────────────────────────────────┐
│ STEP 1: TCP CONNECTION                                       │
│ Layer 0: Tomcat                                              │
│ - Acceptor thread accepts TCP connection                     │
│ - Poller thread detects HTTP bytes                           │
│ - Worker thread allocated from pool (of 200)                 │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 2: HTTP PARSING                                         │
│ Layer 0: Tomcat (Coyote)                                     │
│ - Parses HTTP request line, headers, body                    │
│ - Creates Coyote Request/Response objects                    │
│ - Maps URL "/api/users/42" to Wrapper (DispatcherServlet)    │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 3: FILTER CHAIN                                         │
│ Layer 1: Servlet Filters                                     │
│ - Spring Security Filter (authentication)                    │
│ - CORS Filter                                               │
│ - Logging Filter                                            │
│ - Each filter can abort the chain (return from doFilter)     │
│ - Eventually calls chain.doFilter() → DispatcherServlet      │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 4: DISPATCHERSERVLET RECEIVES REQUEST                   │
│ Layer 2: Spring MVC                                         │
│ - DispatcherServlet.service() is called                      │
│ - It IS a Servlet! (extends HttpServlet)                     │
│ - Delegates to doDispatch(request, response)                 │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 5: HANDLER LOOKUP                                       │
│ Layer 2: Spring MVC                                         │
│ - HandlerMapping.getHandler() called                        │
│ - RequestMappingHandlerMapping finds:                       │
│   "GET /api/users/{id}" → UserController.getUser(Long id)    │
│ - Returns HandlerExecutionChain (handler + interceptors)    │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 6: INTERCEPTOR PRE-HANDLE                               │
│ Layer 2: Spring MVC                                         │
│ - HandlerInterceptor.preHandle() runs                       │
│ - Can abort (return false)                                   │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 7: HANDLER INVOCATION                                   │
│ Layer 2: Spring MVC                                         │
│ - HandlerAdapter.handle() called                            │
│ - Resolves @PathVariable("id") = 42                         │
│ - Invokes method via reflection:                            │
│   userController.getUser(42L)                               │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 8: BUSINESS LOGIC                                       │
│ Layer 3: Your Code                                          │
│ - UserController calls userService.findById(42)             │
│ - UserService calls userRepository.findById(42)              │
│ - UserRepository executes SQL: SELECT * FROM users WHERE... │
│ - Returns User{id=42, name="John"}                          │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 9: RETURN VALUE HANDLING                                │
│ Layer 2: Spring MVC                                         │
│ - HandlerAdapter sees @ResponseBody (or @RestController)    │
│ - Uses HttpMessageConverter (Jackson)                       │
│ - Serializes User → JSON: {"id":42,"name":"John"}           │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 10: INTERCEPTOR POST-HANDLE                             │
│ Layer 2: Spring MVC                                         │
│ - HandlerInterceptor.postHandle() runs                      │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 11: RESPONSE WRITTEN                                    │
│ Layer 1: Servlet API                                        │
│ - response.getWriter().write(json)                          │
│ - Response committed to socket                              │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 12: INTERCEPTOR AFTER-COMPLETION                        │
│ Layer 2: Spring MVC                                         │
│ - HandlerInterceptor.afterCompletion() runs                 │
│ - Cleanup, metrics (even on exception)                      │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 13: FILTER CHAIN UNWINDS                                │
│ Layer 1: Servlet Filters                                    │
│ - Post-processing in each filter                            │
│ - Logging: "Request completed in 45ms"                      │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ STEP 14: TCP RESPONSE                                        │
│ Layer 0: Tomcat                                             │
│ - HTTP response bytes written to socket                     │
│ - Worker thread returned to pool                            │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
                    Browser receives JSON
```

**Total time:** ~5-50ms for a typical request (depending on DB query).

---

<a name="8-embed-tomcat"></a>
## 8. How Spring Boot Embeds Tomcat and Wires DispatcherServlet

This is where Spring Boot's **magic** happens. Let's break it down.

### The Embedded Server Startup

When `SpringApplication.run()` finishes the **refresh** phase, it calls 
`ServletWebServerApplicationContext.finishRefresh()`:

```java
// Simplified from Spring Boot source
protected void finishRefresh() {
    super.finishRefresh(); // Spring's lifecycle
    
    // Start the embedded web server
    WebServer webServer = startWebServer();
    
    // Publish the event
    publishEvent(new ServletWebServerInitializedEvent(...));
    
    // Call runners
    callRunners(...);
}

private WebServer startWebServer() {
    // Get the ServletWebServerFactory bean (auto-configured)
    ServletWebServerFactory factory = getBean(ServletWebServerFactory.class);
    
    // Create the web server (Tomcat, Jetty, or Undertow)
    return factory.getWebServer(new ServletContextInitializer[] {
        // This lambda registers DispatcherServlet + all filters
        this::selfInitialize
    });
}
```

### The `selfInitialize` Method

This is where **`DispatcherServlet` gets registered to Tomcat**:

```java
// Simplified
private void selfInitialize(ServletContext servletContext) throws ServletException {
    
    // 1. Find the DispatcherServlet bean
    DispatcherServlet ds = getBean(DispatcherServlet.class);
    
    // 2. Register it with the ServletContext (Tomcat)
    ServletRegistration.Dynamic registration = 
        servletContext.addServlet("dispatcherServlet", ds);
    
    // 3. Configure it
    registration.setLoadOnStartup(1);  // eager init
    registration.addMapping("/");       // map to all URLs
    registration.setAsyncSupported(true); // enable async
    
    // 4. Register all Filter beans
    for (Filter filter : getBeansOfType(Filter.class)) {
        servletContext.addFilter(filter.getName(), filter)
            .addMappingForUrlPatterns(null, false, "/*");
    }
    
    // 5. Register all Servlet beans
    for (Servlet servlet : getBeansOfType(Servlet.class)) {
        // ... similar
    }
    
    // 6. Register all Listener beans
    for (SmartInitializingSingleton listener : getBeansOfType(...)) {
        // ...
    }
}
```

### The Tomcat Factory: How Tomcat Is Created

```java
// Simplified from TomcatServletWebServerFactory
public WebServer getWebServer(ServletContextInitializer... initializers) {
    
    // 1. Create a Tomcat instance
    Tomcat tomcat = new Tomcat();
    
    // 2. Configure the port
    int port = getPort(); // from server.port property
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setPort(port);
    tomcat.setConnector(connector);
    
    // 3. Create a web app context
    Context context = tomcat.addContext("/", System.getProperty("java.io.tmpdir"));
    
    // 4. Register all initializers (the lambdas that register DispatcherServlet)
    TomcatServletWebServerFactory.prepareContext(context, initializers);
    
    // 5. Start Tomcat
    tomcat.start();
    
    // 6. Return a handle
    return new TomcatWebServer(tomcat);
}
```

### The Complete Startup Sequence

```
java -jar app.jar
    ↓
SpringApplication.run()
    ↓
Auto-Configuration reads META-INF/spring/...AutoConfiguration.imports
    ↓
ServletWebServerFactoryAutoConfiguration creates TomcatServletWebServerFactory bean
    ↓
DispatcherServletAutoConfiguration creates DispatcherServlet bean
    ↓
ApplicationContext.refresh()
    ├─ BeanFactory created
    ├─ All @Component / @Service / @Repository / @Controller beans created
    ├─ RequestMappingHandlerMapping registers all @GetMapping methods
    ├─ RequestMappingHandlerAdapter created
    └─ finishRefresh() called
         ↓
         TomcatServletWebServerFactory creates Tomcat instance
         ↓
         Tomcat starts, opens port 8080
         ↓
         DispatcherServlet registered to "/" with loadOnStartup=1
         ↓
         All Filter beans registered to "/*"
         ↓
         ApplicationReadyEvent fired
         ↓
         APP IS READY
```

**FAANG Discussion Point:**  
*"Why does Spring Boot register DispatcherServlet to '/' instead of '*.do' (like old Struts)?"* — Modern REST APIs 
need to handle **all** HTTP methods (GET, POST, PUT, DELETE, PATCH) and **all** URL patterns. Mapping to `/` 
catches everything. The `DispatcherServlet` then uses `HandlerMapping` to route to the right method based on URL + 
method + headers.

---

<a name="9-9-step-correlated"></a>
## 9. The 9-Step Spring MVC Flow, Correlated with Servlet Concepts

Here's the `DispatcherServlet.doDispatch()` flow, correlated with the Servlet API:

| Step | Spring MVC | Servlet API Correlation |
|---|---|---|
| 1 | Multipart check | Uses `HttpServletRequest.getParts()` (Servlet 3.0+) |
| 2 | Handler lookup | `HandlerMapping` is a Spring-specific concept |
| 3 | Adapter lookup | `HandlerAdapter` is a Spring-specific concept |
| 4 | `preHandle()` | `HandlerInterceptor` is Spring-specific (not Servlet) |
| 5 | Method invocation | Java reflection, but uses `HttpServletRequest` params |
| 6 | Default view name | Spring-specific (for view resolution) |
| 7 | `postHandle()` | `HandlerInterceptor` post-processing |
| 8 | Dispatch result | View rendering OR `HttpMessageConverter` writing to `HttpServletResponse` |
| 9 | `afterCompletion()` | Cleanup, metrics |

### The Key Correlation Points

**Servlet → Spring MVC boundary (Steps 1-4):**
- Tomcat calls `DispatcherServlet.service()` (Servlet API).
- `DispatcherServlet` is a `HttpServlet` — it **inherits** all Servlet behavior.
- It then uses Spring-specific abstractions (`HandlerMapping`, `HandlerInterceptor`).

**Spring MVC → Your Code boundary (Step 5):**
- `HandlerAdapter` invokes your `@Controller` method via **Java reflection**.
- Method parameters are resolved from `HttpServletRequest` (e.g., `@PathVariable` from URL, `@RequestParam` from 
query string, `@RequestBody` from request body).

**Your Code → Response boundary (Step 8):**
- Return value is written to `HttpServletResponse` via `HttpMessageConverter` (Jackson).
- OR view is rendered and written to `HttpServletResponse`.

**FAANG Discussion Point:**  
*"At what point does the Servlet API end and Spring MVC begin?"* — At `DispatcherServlet.service()`. Everything 
**before** that (filter chain, HTTP parsing, thread allocation) is pure Servlet API. Everything **after** (handler 
mapping, argument resolution, view resolution) is Spring MVC.

---

<a name="10-autoconfig"></a>
## 10. Auto-Configuration: How Spring Boot Wires the Servlet World

Spring Boot's auto-configurations are **conditional `@Configuration` classes** that wire up the Servlet world for 
you.

### The Key Auto-Configurations for Web

| Auto-Configuration | What It Wires |
|---|---|
| `ServletWebServerFactoryAutoConfiguration` | Creates `TomcatServletWebServerFactory` (or Jetty/Undertow) |
| `DispatcherServletAutoConfiguration` | Creates and registers `DispatcherServlet` to `/` |
| `WebMvcAutoConfiguration` | Enables Spring MVC, registers `HandlerAdapter`, static resources |
| `JacksonAutoConfiguration` | Registers Jackson `ObjectMapper` for JSON |
| `HttpMessageConvertersAutoConfiguration` | Registers `MappingJackson2HttpMessageConverter` |
| `ErrorMvcAutoConfiguration` | Handles errors (404, 500) with `/error` endpoint |
| `WebMvcEndpointManagementContextConfiguration` | Exposes Actuator endpoints |
| `SecurityAutoConfiguration` | Enables Spring Security (if on classpath) |

### How `@Conditional` Makes It Smart

Each auto-config uses `@Conditional*` annotations to decide **whether** to apply:

```java
// Example: JacksonAutoConfiguration
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)  // Jackson is on classpath
public class JacksonAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean  // User hasn't defined their own ObjectMapper
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

**The magic:** Spring Boot only creates a bean if:
1. The required class is on the classpath (`@ConditionalOnClass`).
2. The user hasn't already defined one (`@ConditionalOnMissingBean`).

This is **convention over configuration**: Spring Boot provides defaults, you override.

### The Override Pattern

```java
// Spring Boot's default
@Bean
@ConditionalOnMissingBean
public ObjectMapper objectMapper() {
    return new ObjectMapper();
}

// Your override
@Configuration
public class MyConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule()); // your custom config
        return om;
    }
}
```
Your `@Bean` wins. Spring Boot's default is skipped.

---

<a name="11-filters-vs-interceptors"></a>
## 11. Filters vs Interceptors: The Correlated View

This is one of the **most-asked** Spring/Servlet questions.

### Execution Order

```
HTTP Request
    ↓
┌────────────────────────────────────────┐
│ SERVLET FILTERS (Layer 1)              │
│ - Spring Security                      │
│ - CORS                                │
│ - Logging                             │
│ - Runs BEFORE DispatcherServlet       │
└────────────────┬───────────────────────┘
                 ↓ chain.doFilter()
┌────────────────────────────────────────┐
│ DISPATCHERSERVLET (Layer 2)           │
└────────────────┬───────────────────────┘
                 ↓
┌────────────────────────────────────────┐
│ HANDLER INTERCEPTORS (Layer 2)        │
│ - preHandle                           │
│ - Runs AROUND the controller method   │
└────────────────┬───────────────────────┘
                 ↓
┌────────────────────────────────────────┐
│ @CONTROLLER METHOD (Layer 3)          │
└────────────────┬───────────────────────┘
                 ↑ (return path)
┌────────────────────────────────────────┐
│ HANDLER INTERCEPTORS (Layer 2)        │
│ - postHandle                          │
│ - afterCompletion                     │
└────────────────┬───────────────────────┘
                 ↑ (return path)
┌────────────────────────────────────────┐
│ SERVLET FILTERS (Layer 1)              │
│ - Post-processing                     │
└────────────────────────────────────────┘
                 ↓
HTTP Response
```

### Comparison Table

| Aspect | Servlet Filter | Handler Interceptor |
|---|---|---|
| **Layer** | Servlet API (Layer 1) | Spring MVC (Layer 2) |
| **Interface** | `jakarta.servlet.Filter` | `org.springframework.web.servlet.HandlerInterceptor` |
| **Runs** | Before/after DispatcherServlet | Around the controller method |
| **Sees** | Raw `HttpServletRequest` | Handler info (method, model) |
| **Spring DI** | ❌ (needs `@Component` or `DelegatingFilterProxy`) | ✅ Full Spring DI |
| **Access to handler** | ❌ No | ✅ Yes (`HandlerMethod`) |
| **Use case** | Security, CORS, encoding, compression | Logging, performance metrics, auth checks |
| **Registration** | `web.xml` or `@WebFilter` or `FilterRegistrationBean` | `WebMvcConfigurer.addInterceptors()` 
|
| **Order control** | `setOrder(int)` or `FilterRegistrationBean.setOrder()` | `setOrder(int)` or `@Order` |
| **Spring Security uses** | ✅ Yes (Filter chain) | ❌ No |

### Why Both Exist

- **Filters** are **general-purpose** (work with any Servlet, not just Spring MVC).
- **Interceptors** are **Spring MVC-specific** (have access to the matched handler method).

**FAANG Discussion Point:**  
*"Where would you put authentication — Filter or Interceptor?"* — **Filter** (Spring Security uses 
`FilterChainProxy`). Authentication must happen **before** the request enters MVC, before any URL-to-handler 
resolution. Filters see the raw HTTP; Interceptors see Spring-resolved handlers.

---

<a name="12-session-async"></a>
## 12. Session, Async, and Concurrency: All Three Layers

### Sessions

| Layer | Mechanism |
|---|---|
| **Servlet API** | `HttpSession` (JSESSIONID cookie) |
| **Spring MVC** | `@SessionAttribute` (type-safe session access) |
| **Spring Boot** | `spring-session-data-redis` (external session store) |

### Async Processing

| Layer | Mechanism |
|---|---|
| **Servlet API** | `AsyncContext` (Servlet 3.0+) |
| **Spring MVC** | `Callable<T>`, `DeferredResult<T>`, `SseEmitter` |
| **Spring Boot** | Auto-configures async support on `DispatcherServlet` |

**How Spring MVC async works (correlated with Servlet async):**

```java
@GetMapping("/data")
public DeferredResult<String> getData() {
    DeferredResult<String> result = new DeferredResult<>();
    
    // The worker thread is RELEASED BACK TO THE POOL
    CompletableFuture.supplyAsync(() -> {
        // Long-running work
        return "Data ready";
    }).thenAccept(result::setResult);
    
    // The response will be written by a DIFFERENT thread
    return result;
}
```

Internally, Spring uses `AsyncContext` from the Servlet API to release the worker thread and resume the response 
on a different thread.

### Threading

| Layer | Thread Pool |
|---|---|
| **Tomcat** | Worker thread pool (default 200) |
| **Spring MVC** | Uses Tomcat's pool (by default) |
| **Spring Boot** | Configurable via `server.tomcat.threads.*` |

---

<a name="13-diagrams"></a>
## 13. Architecture & Flow Diagrams

### 13.1 — The Three-Layer Correlation

```
┌──────────────────────────────────────────────────────────────┐
│  LAYER 3: SPRING BOOT                                        │
│  - @SpringBootApplication                                    │
│  - Auto-Configuration                                        │
│  - Embedded Tomcat                                           │
│  - application.properties                                    │
├──────────────────────────────────────────────────────────────┤
│  LAYER 2: SPRING MVC                                         │
│  - DispatcherServlet (extends HttpServlet)                   │
│  - @Controller, @RestController                              │
│  - HandlerMapping, HandlerAdapter                            │
│  - HttpMessageConverter (Jackson)                            │
│  - HandlerInterceptor                                        │
├──────────────────────────────────────────────────────────────┤
│  LAYER 1: SERVLET API                                        │
│  - HttpServlet (base for DispatcherServlet)                  │
│  - HttpServletRequest, HttpServletResponse                  │
│  - Filter, Listener                                          │
│  - HttpSession                                               │
├──────────────────────────────────────────────────────────────┤
│  LAYER 0: SERVLET CONTAINER (Tomcat)                         │
│  - NIO Connector, Thread Pool                                │
│  - Container Hierarchy (Server → Context → Wrapper)         │
│  - HTTP Parsing (Coyote)                                     │
└──────────────────────────────────────────────────────────────┘
```

### 13.2 — The Restaurant Kitchen (Full Correlation)

```
Customer (Browser)
    ↓ "GET /api/users/42"
Host Stand (Tomcat Connector)
    ↓ Accepts TCP, allocates worker thread
Waiter (Tomcat Worker Thread)
    ↓ Takes "order" (HttpServletRequest)
Health Inspector (Servlet Filter Chain)
    ↓ Checks credentials, logs
Expediter (DispatcherServlet)
    ↓ "This order goes to the User station"
Recipe Book (HandlerMapping)
    ↓ "GET /api/users/{id} → UserController.getUser"
Head Chef (HandlerAdapter)
    ↓ Resolves @PathVariable("id") = 42
Station Chef (@Controller method)
    ↓ Calls sous chef
Sous Chef (@Service)
    ↓ Calls pantry
Pantry (@Repository → Database)
    ↓ Returns User{id=42, name="John"}
Back through the chain
    ↓
Plating Station (HttpMessageConverter / Jackson)
    ↓ Serializes User → {"id":42,"name":"John"}
Waiter delivers the "dish" (HTTP response)
    ↓
Customer receives JSON
```

### 13.3 — Spring Boot Startup → First Request

```
java -jar app.jar
    ↓
JVM starts, loads MANIFEST.MF
    ↓
JarLauncher loads Start-Class (com.example.MyApp)
    ↓
SpringApplication.run() begins
    ├─ 1. Bootstrap
    ├─ 2. Environment (application.properties)
    ├─ 3. Banner
    ├─ 4. ApplicationContext created
    ├─ 5. Auto-Configuration applied:
    │   - DispatcherServletAutoConfiguration → creates DispatcherServlet
    │   - ServletWebServerFactoryAutoConfiguration → creates Tomcat
    │   - WebMvcAutoConfiguration → enables MVC
    │   - JacksonAutoConfiguration → enables JSON
    ├─ 6. Refresh Context:
    │   - @ComponentScan finds @RestController classes
    │   - RequestMappingHandlerMapping registers @GetMapping URLs
    │   - All singleton beans created
    ├─ 7. finishRefresh():
    │   - TomcatServletWebServerFactory creates Tomcat instance
    │   - DispatcherServlet registered to "/" with loadOnStartup=1
    │   - All Filter beans registered to "/*"
    │   - Tomcat.start() → opens port 8080
    ├─ 8. ApplicationReadyEvent fired
    ↓
APP IS READY — listening on :8080
    ↓
Browser sends GET /api/users/42
    ↓
(Full 14-step request flow from Section 7)
    ↓
Browser receives {"id":42,"name":"John"}
```

### 13.4 — How Spring Boot Registers DispatcherServlet to Tomcat

```
Spring Boot Startup
    ↓
ServletWebServerApplicationContext.finishRefresh()
    ↓
ServletWebServerFactory (Tomcat) created
    ↓
factory.getWebServer(initializers) called
    ↓
Tomcat instance created
    ↓
Connector configured (port 8080, NIO protocol)
    ↓
Context created (Tomcat's web app)
    ↓
ServletContextInitializer lambdas executed:
    ├─ Lambda 1: Register DispatcherServlet
    │   - servletContext.addServlet("dispatcherServlet", ds)
    │   - registration.setLoadOnStartup(1)
    │   - registration.addMapping("/")
    ├─ Lambda 2: Register Filter beans
    │   - servletContext.addFilter("securityFilter", filter)
    │   - addMappingForUrlPatterns("/*")
    └─ Lambda 3: Register other Servlets
    ↓
Tomcat.start()
    ↓
Port 8080 is now listening
    ↓
First HTTP request arrives
    ↓
Tomcat routes to DispatcherServlet (because URL mapping is "/")
    ↓
DispatcherServlet.service() called
    ↓
(Spring MVC 9-step flow)
```

### 13.5 — Filter vs Interceptor Execution

```
HTTP Request
    ↓
┌──────────────────────────────────────┐
│ Filter1.doFilter()                   │
│   - Security check                   │
│   - chain.doFilter() ─────┐          │
└───────────────────────────┼──────────┘
                            ↓
┌──────────────────────────────────────┐
│ Filter2.doFilter()                   │
│   - CORS check                       │
│   - chain.doFilter() ─────┐          │
└───────────────────────────┼──────────┘
                            ↓
┌──────────────────────────────────────┐
│ DISPATCHERSERVLET                    │
│   - doDispatch()                     │
└───────────────────────────┬──────────┘
                            ↓
┌──────────────────────────────────────┐
│ Interceptor1.preHandle()             │
│   - Logging: "Request started"       │
│   - return true ────────┐            │
└──────────────────────────┼───────────┘
                           ↓
┌──────────────────────────────────────┐
│ @Controller method                   │
│   - Business logic                   │
└──────────────────────────┬───────────┘
                           ↑ (return)
┌──────────────────────────────────────┐
│ Interceptor1.postHandle()            │
│   - Logging: "Response ready"        │
└──────────────────────────┬───────────┘
                           ↑ (return)
┌──────────────────────────────────────┐
│ HttpMessageConverter (Jackson)       │
│   - Serialize User → JSON           │
└──────────────────────────┬───────────┘
                           ↑ (return)
┌──────────────────────────────────────┐
│ Interceptor1.afterCompletion()       │
│   - Metrics: "Request took 45ms"    │
└──────────────────────────┬───────────┘
                           ↑ (return)
┌──────────────────────────────────────┐
│ Filter2.post-processing              │
│   - Add response headers             │
└──────────────────────────┬───────────┘
                           ↑ (return)
┌──────────────────────────────────────┐
│ Filter1.post-processing              │
│   - Final logging                    │
└──────────────────────────┬───────────┘
                           ↓
HTTP Response sent
```

### 13.6 — The Complete Spring Boot Request Lifecycle

```
1. STARTUP (once)
   java -jar → JarLauncher → SpringApplication.run()
   → Auto-Config → Embedded Tomcat → DispatcherServlet registered
   → Application Ready

2. REQUEST (per request)
   TCP bytes → Tomcat NIO → Worker thread
   → Filter chain → DispatcherServlet
   → HandlerMapping → HandlerInterceptor.preHandle()
   → HandlerAdapter → @Controller method
   → @Service → @Repository → Database
   → Return value → HttpMessageConverter (Jackson) → JSON
   → HandlerInterceptor.postHandle() + afterCompletion()
   → Filter post-processing → Response written
   → Worker thread returned to pool

3. SHUTDOWN (once)
   SIGTERM → Graceful shutdown
   → In-flight requests complete
   → @PreDestroy callbacks
   → Context closed
   → Tomcat.stop()
   → JVM exits
```

### 13.7 — Bean Creation Order in Spring Boot

```
BeanFactory created
    ↓
BeanPostProcessors instantiated EARLY
    ↓
@Configuration classes processed
    ↓
@ComponentScan finds @Component, @Service, @Repository, @Controller
    ↓
Auto-Configuration beans created:
    ├─ DispatcherServlet (via DispatcherServletAutoConfiguration)
    ├─ TomcatServletWebServerFactory (via ServletWebServerFactoryAutoConfiguration)
    ├─ ObjectMapper (via JacksonAutoConfiguration)
    ├─ RequestMappingHandlerMapping (via WebMvcAutoConfiguration)
    └─ RequestMappingHandlerAdapter (via WebMvcAutoConfiguration)
    ↓
User beans created (your @Service, @Repository, etc.)
    ↓
ServletContextInitializer lambdas execute
    ↓
DispatcherServlet registered to Tomcat
    ↓
Tomcat.start() → port 8080 open
    ↓
ApplicationReadyEvent fired
```

### 13.8 — How the Three Layers Handle a POST Request

```
Browser sends: POST /api/users with body {"name":"John","email":"john@x.com"}
    ↓
┌──────────────────────────────────────────────────────────────┐
│ TOMCAT (Layer 0)                                             │
│ - Accepts TCP connection                                     │
│ - Parses HTTP POST, reads body bytes                         │
│ - Creates HttpServletRequest with body                       │
│ - Routes to DispatcherServlet (mapped to "/")                │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ SERVLET FILTERS (Layer 1)                                    │
│ - Spring Security: checks CSRF token, authenticates          │
│ - CORS: checks Origin header                                │
│ - Logging: logs "POST /api/users"                            │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ DISPATCHERSERVLET (Layer 2)                                  │
│ - doDispatch() begins                                        │
│ - HandlerMapping: "POST /api/users" → UserController.create()│
│ - HandlerInterceptor.preHandle()                             │
│ - HandlerAdapter:                                            │
│   - Resolves @RequestBody: reads request body, deserializes  │
│     JSON to UserDTO using Jackson                            │
│   - Invokes: userController.create(userDTO)                  │
│ - @Controller method:                                        │
│   - Validates @Valid UserDTO                                 │
│   - Calls userService.create(dto)                            │
│   - @Service calls userRepository.save(dto)                  │
│   - @Repository: INSERT INTO users (...)                     │
│   - Returns User{id=43, name="John", email="john@x.com"}     │
│ - HandlerAdapter:                                            │
│   - Sees @ResponseBody                                       │
│   - Uses HttpMessageConverter (Jackson)                      │
│   - Serializes User → {"id":43,"name":"John",...}            │
│ - HandlerInterceptor.postHandle() + afterCompletion()        │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ SERVLET FILTERS (Layer 1) - post-processing                  │
│ - Add response headers (X-Request-Id, etc.)                  │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ TOMCAT (Layer 0)                                             │
│ - HTTP 201 Created                                           │
│ - Location: /api/users/43                                    │
│ - Body: {"id":43,"name":"John",...}                          │
│ - Worker thread returned to pool                             │
└─────────────────────────────┬────────────────────────────────┘
                              ↓
Browser receives 201 Created
```

### 13.9 — The Spring Boot Auto-Configuration Chain for Web

```
spring-boot-starter-web on classpath
    ↓
META-INF/spring/...AutoConfiguration.imports lists:
    ├─ ServletWebServerFactoryAutoConfiguration
    ├─ DispatcherServletAutoConfiguration
    ├─ WebMvcAutoConfiguration
    ├─ JacksonAutoConfiguration
    ├─ HttpMessageConvertersAutoConfiguration
    └─ ErrorMvcAutoConfiguration
    ↓
Spring Boot evaluates each @Conditional:
    ├─ Tomcat class on classpath? → Use Tomcat
    ├─ DispatcherServlet class? → Register it
    ├─ Jackson on classpath? → Configure ObjectMapper
    └─ User has no SecurityFilterChain bean? → Add default security
    ↓
All beans created in correct order
    ↓
Application ready
```

### 13.10 — The One-Page Mental Model

```
┌──────────────────────────────────────────────────────────────┐
│                   YOUR CODE                                  │
│  @RestController → @Service → @Repository → Database         │
├──────────────────────────────────────────────────────────────┤
│  SPRING BOOT                                                 │
│  - @SpringBootApplication (component scan + auto-config)     │
│  - SpringApplication.run() (bootstrap + refresh)             │
│  - Embedded Tomcat (no external server)                      │
│  - Auto-wires DispatcherServlet, filters, Jackson            │
├──────────────────────────────────────────────────────────────┤
│  SPRING MVC                                                  │
│  - DispatcherServlet (front controller, IS a HttpServlet)    │
│  - HandlerMapping (URL → method)                             │
│  - HandlerAdapter (invokes method)                           │
│  - HttpMessageConverter (Jackson for JSON)                   │
│  - HandlerInterceptor (pre/post processing)                  │
├──────────────────────────────────────────────────────────────┤
│  SERVLET API                                                 │
│  - HttpServlet (base for DispatcherServlet)                  │
│  - HttpServletRequest/Response (HTTP abstraction)            │
│  - Filter (runs BEFORE DispatcherServlet)                    │
│  - HttpSession (server-side state)                           │
│  - Listener (lifecycle hooks)                                │
├──────────────────────────────────────────────────────────────┤
│  SERVLET CONTAINER (Tomcat)                                  │
│  - NIO Connector (TCP/HTTP)                                  │
│  - Thread pool (default 200)                                 │
│  - HTTP parsing (Coyote)                                     │
│  - Container hierarchy (Server → Context → Wrapper)         │
└──────────────────────────────────────────────────────────────┘
```

---

<a name="14-faang"></a>
## 14. FAANG Discussion Points (Top 20)

### 1. **The Correlation: Servlet → Spring MVC → Spring Boot**
> *"How does Spring Boot relate to Spring MVC and Servlets?"*

Spring Boot is a **layer on top** of Spring MVC, which is a **layer on top** of the Servlet API. Spring Boot 
doesn't replace anything — it **auto-configures** and **embeds** the existing layers.

### 2. **DispatcherServlet IS a Servlet**
> *"Is DispatcherServlet a Servlet?"*

**Yes.** It extends `FrameworkServlet` → `HttpServletBean` → `HttpServlet`. It **inherits** all Servlet behavior 
(lifecycle, request/response, session) and **adds** Spring MVC's 9-step dispatch flow.

### 3. **Filters vs Interceptors**
> *"Where would you put authentication — Filter or Interceptor?"*

**Filter** (Spring Security uses `FilterChainProxy`). Authentication must happen **before** the request enters 
MVC. Filters see raw HTTP; Interceptors see Spring-resolved handlers.

### 4. **Auto-Configuration Doesn't Conflict**
> *"How does my @Configuration override Spring Boot's defaults?"*

`@ConditionalOnMissingBean` ensures Spring Boot only creates a bean if you haven't already defined one. Your 
`@Bean` wins.

### 5. **Threading: Thread-Per-Request**
> *"How does Tomcat handle 10K concurrent requests with 200 threads?"*

NIO connector: one Acceptor accepts connections, Poller threads monitor many sockets, Worker thread pool runs the 
actual Servlet code. Only active connections consume worker threads.

### 6. **Embedded Server vs External**
> *"Why does Spring Boot embed Tomcat?"*

12-factor app compliance: one artifact (JAR), runs anywhere with a JDK. No "works on my Tomcat version" issues. 
Faster CI/CD.

### 7. **The 9 Strategy Beans**
> *"What are the 9 special beans in DispatcherServlet?"*

MultipartResolver, LocaleResolver, ThemeResolver, HandlerMapping, HandlerAdapter, HandlerExceptionResolver, 
RequestToViewNameTranslator, ViewResolver, FlashMapManager.

### 8. **Externalized Config Precedence**
> *"What's the precedence of configuration sources?"*

CLI args > env vars > application.properties > @PropertySource > defaults. This enables 12-factor deployment.

### 9. **Servlet Lifecycle**
> *"How many times is init() called? service()?"*

`init()` — once. `service()` — once per request (concurrently, multiple threads).

### 10. **Session Management**
> *"How do you scale sessions across multiple nodes?"*

Sticky sessions, external store (Redis via `spring-session-data-redis`), or stateless tokens (JWT).

### 11. **Async Processing**
> *"How does Spring MVC async work with Servlets?"*

`Callable<T>` or `DeferredResult<T>` returns immediately. Internally, Spring uses `AsyncContext` to release the 
worker thread and resume the response on a different thread.

### 12. **HandlerExceptionResolver Chain**
> *"How are exceptions handled?"*

Three resolvers in order: `ExceptionHandlerExceptionResolver` (your `@ExceptionHandler`), 
`ResponseStatusExceptionResolver` (`@ResponseStatus`), `DefaultHandlerExceptionResolver` (Spring's built-ins).

### 13. **HttpMessageConverter**
> *"How does Spring decide JSON vs XML serialization?"*

By the `Accept` header. `MappingJackson2HttpMessageConverter` for JSON, `Jaxb2RootElementHttpMessageConverter` for 
XML.

### 14. **The @SpringBootApplication Myth**
> *"What does @SpringBootApplication actually do?"*

It's just `@Configuration + @EnableAutoConfiguration + @ComponentScan`. It does NOT enable MVC, JPA, or 
transactions — those are enabled by their own auto-configurations.

### 15. **NIO vs BIO**
> *"Why is Tomcat's NIO connector better?"*

NIO uses one thread per many connections (selector-based). BIO uses one thread per connection. NIO scales to 10K+ 
connections with 200 threads.

### 16. **MultipartResolver**
> *"How do you handle file uploads > 1GB?"*

Default MultipartResolver loads to memory/disk. For huge files: streaming or pre-signed S3 URLs (client uploads 
directly to S3, bypassing Spring).

### 17. **ServletContext vs ServletConfig**
> *"What's the difference?"*

`ServletConfig` is per-Servlet (init params for one Servlet). `ServletContext` is per-web-app (shared across all 
Servlets).

### 18. **Why DispatcherServlet Maps to "/"**
> *"Why not map to '*.do' like old Struts?"*

Modern REST APIs need all HTTP methods and all URL patterns. Mapping to `/` catches everything; `HandlerMapping` 
routes based on URL + method + headers.

### 19. **The 12-Factor App Connection**
> *"How does this fit into 12-factor app methodology?"*

Embedded server (process), externalized config (config), stateless sessions (backing services), port binding (the 
embedded Tomcat binds to 8080).

### 20. **Common Production Pitfalls**
> *"What are the most common Spring Boot production issues?"*

Thread pool exhaustion (slow DB queries), memory leaks (ThreadLocal not cleaned up), large session objects, 
blocking in async controllers, holding DB connections too long.

---

<a name="15-followup"></a>
## 15. Top 20 Follow-up Q&A

### **Q1: Is DispatcherServlet thread-safe?**
**A:** **Yes.** It stores only references to strategy beans (which are thread-safe singletons). Request/response 
objects are per-request (stored in method parameters, not instance fields).

### **Q2: Can I have multiple DispatcherServlets in one app?**
**A:** Yes, with different URL mappings. Each has its own `WebApplicationContext`. Rare in practice — used for 
isolated web modules (e.g., admin UI vs public site).

### **Q3: What's the difference between WAR and JAR deployment?**
**A:** **WAR** — deployed to external Tomcat/WildFly. Has `WEB-INF/web.xml`. **JAR** (Spring Boot executable) — 
embedded Tomcat, `java -jar app.jar`. Modern Spring Boot prefers JAR for microservices.

### **Q4: How does Spring Boot find the main class?**
**A:** The class with `@SpringBootApplication` and a `main` method. The manifest's `Start-Class` attribute points 
to it. The package of this class is the base package for component scanning.

### **Q5: What is the difference between @Controller and @RestController?**
**A:** `@RestController` = `@Controller` + `@ResponseBody` on every method. Use for JSON APIs; use `@Controller` 
for HTML templates.

### **Q6: How do you handle exceptions thrown in a Filter?**
**A:** Filters are outside Spring MVC, so `@ControllerAdvice` doesn't catch them. Options: catch and write 
response in the filter, or use Spring Security's exception handlers.

### **Q7: What is the difference between Servlet 3.0 and 3.1?**
**A:** **3.0** added async, annotations, programmatic config. **3.1** added non-blocking I/O. **4.0** added 
HTTP/2, server push.

### **Q8: How does Spring handle HTTP HEAD and OPTIONS?**
**A:** **HEAD** — auto-handled, invokes GET method, discards body. **OPTIONS** — handled by DispatcherServlet, 
returns `Allow` header.

### **Q9: Can Spring MVC work without web.xml?**
**A:** Yes! Since Servlet 3.0, use `AbstractAnnotationConfigDispatcherServletInitializer` or Spring Boot's 
auto-config.

### **Q10: What is graceful shutdown?**
**A:** Spring Boot 2.3+ supports `server.shutdown=graceful`. Stops accepting new requests, waits for in-flight 
requests to complete (default 30s), then closes the context.

### **Q11: How do you enable HTTPS in Spring Boot?**
**A:** Configure `server.ssl.*` properties (key-store, key-store-password, key-alias). Or use a reverse proxy 
(nginx, AWS ALB) for TLS termination.

### **Q12: What is the @Conditional annotation family?**
**A:** `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`, etc. They let beans be 
created only if certain conditions are met. It's how Spring Boot makes smart auto-config decisions.

### **Q13: Can you use Spring Boot with Kotlin?**
**A:** Yes! `spring-boot-starter` works with Kotlin. There's also `kotlin-spring` compiler plugin to make Kotlin 
classes open (needed for CGLIB proxying).

### **Q14: How do you do health checks for Kubernetes?**
**A:** Enable Actuator's liveness/readiness probes. Spring exposes `/actuator/health/liveness` and 
`/actuator/health/readiness`. Configure these in your K8s deployment.

### **Q15: What is the Spring Boot CLI?**
**A:** A command-line tool for running Spring Boot Groovy scripts. Useful for quick prototyping: `spring run 
app.groovy`.

### **Q16: How do you disable a specific auto-configuration?**
**A:** `@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })` or in `application.yml`: 
`spring.autoconfigure.exclude=...`

### **Q17: What is @SpringBootTest?**
**A:** Loads the full application context for integration tests. Use `@WebMvcTest(MyController.class)` for slice 
tests that load only the web layer.

### **Q18: How does Spring Boot 3 differ from Spring Boot 2?**
**A:** Jakarta EE 9+ (jakarta.* packages), Java 17+ baseline, Spring Framework 6, native compilation (Spring 
Native / GraalVM), ProblemDetails (RFC 7807) for error responses.

### **Q19: What is Spring Boot's AOT compilation?**
**A:** AOT (Ahead-of-Time) generates pre-computed bean definitions, reflection metadata at build time. Used for 
native images (GraalVM). Startup: 1ms vs 1000ms.

### **Q20: How do you package a Spring Boot app as a Docker image?**
**A:** Two approaches: Dockerfile with `eclipse-temurin:17-jre` base, or Cloud Native Buildpacks (`mvn 
spring-boot:build-image`) for an optimized image.

---

<a name="16-staff"></a>
## 16. Staff-Engineer Level: Tuning, Pitfalls, Production

### 16.1 — The 5 Most Common Production Pitfalls

| Pitfall | Symptom | Fix |
|---|---|---|
| Thread pool exhaustion | `503 Service Unavailable` | Tune `server.tomcat.threads.max`, profile slow queries |
| Memory leak via ThreadLocal | Heap grows over time | Always `.remove()` in `finally` |
| Large session objects | OOM under load | Use external session store (Redis) |
| Synchronized controller | Throughput capped at 1 req/s | Use lock-free data structures |
| Holding DB connections | Connection pool exhausted | Use async or message queues for long tasks |

### 16.2 — Thread Pool Sizing Formula

```
maxThreads = (target RPS) × (avg request duration in seconds) × 1.2 (headroom)
```

**Example:** 1000 RPS × 0.2s × 1.2 = **240 threads**

**Memory cost:** 240 threads × 1MB stack = 240MB just for stacks.

### 16.3 — When to Use What

| Scenario | Solution |
|---|---|
| High throughput, blocking DB | Spring MVC + thread tuning |
| High concurrency, many idle connections | Spring WebFlux + Netty |
| Long-running tasks | Async Servlets + `Callable` or message queues |
| Real-time bidirectional | WebSocket |
| Server-Sent Events | `SseEmitter` |

### 16.4 — Monitoring Checklist

- `/actuator/metrics/tomcat.threads.busy` — busy threads
- `/actuator/metrics/jvm.threads.live` — total threads
- `/actuator/metrics/http.server.requests` — latency percentiles
- `/actuator/health` — DB, disk, custom checks
- JMX: `Catalina:type=ThreadPool,name="http-nio-8080"`

---

## 🎯 The One-Page Summary

```
┌──────────────────────────────────────────────────────────────┐
│  HTTP Request                                                │
│      ↓                                                       │
│  Tomcat (Layer 0): NIO → thread pool → HTTP parsing          │
│      ↓                                                       │
│  Servlet Filters (Layer 1): Security, CORS, Logging          │
│      ↓                                                       │
│  DispatcherServlet (Layer 2): Front controller               │
│      ├─ HandlerMapping: finds @Controller method             │
│      ├─ HandlerInterceptor.preHandle()                       │
│      ├─ HandlerAdapter: invokes method via reflection        │
│      ├─ @Controller method → @Service → @Repository → DB     │
│      ├─ Return value → HttpMessageConverter (Jackson) → JSON │
│      ├─ HandlerInterceptor.postHandle() + afterCompletion()  │
│      ↓                                                       │
│  Filter post-processing                                      │
│      ↓                                                       │
│  Tomcat writes response                                      │
│      ↓                                                       │
│  HTTP Response                                               │
└──────────────────────────────────────────────────────────────┘
```

**In one sentence:** *Spring Boot embeds Tomcat (the Servlet container), auto-registers `DispatcherServlet` (which 
IS a Servlet), and uses Spring MVC's 9-step dispatch flow to route HTTP requests to your `@Controller` methods — 
all wired together by auto-configuration that reads `application.properties` and conditional bean definitions.* 🌍

---