# 🚀 Servlets, J2EE, and DispatcherServlet — The Complete Deep Dive

From "What is a Servlet?" to "How does the container allocate threads and process 10K requests per second?"


## 📑 Table of Contents

1. [10,000-Foot View: What Are We Building?](#1-10000-foot-view)
2. [The Servlet API: Foundation of J2EE Web](#2-servlet-api-basics)
3. [The Servlet Container (Tomcat) — Internals](#3-servlet-container-internals)
4. [Servlet Lifecycle — The Full Story](#4-servlet-lifecycle)
5. [How a Request Travels: HTTP → Response](#5-request-lifecycle)
6. [Filters — Pre/Post Processing](#6-filters)
7. [Listeners — Event-Driven Hooks](#7-listeners)
8. [Threading Model: One Thread Per Request](#8-threading-model)
9. [Session Management](#9-session-management)
10. [Dispatching & Forwards vs Redirects](#10-dispatch-vs-redirect)
11. [Spring's DispatcherServlet: The Front Controller](#11-dispatcher-servlet)
12. [DispatcherServlet Internals: Bootstrapping](#12-dispatcher-bootstrap)
13. [The 9-Step Request Flow in Spring MVC](#13-nine-step-flow)
14. [Key Components: HandlerMapping, Adapter, ViewResolver](#14-key-components)
15. [Architecture & Flow Diagrams](#15-architecture-diagrams)
16. [FAANG Discussion Points](#16-faang-points)
17. [Top 20 Follow-up Q&A](#17-followup)


<a name="1-10000-foot-view"></a>
## 1. 10,000-Foot View: What Are We Building?

Imagine a **restaurant**:
- **Customer** = Browser (sends an HTTP request)
- **Waiter** = **Servlet Container** (Tomcat, Jetty, WildFly) — receives the order, picks the right chef
- **Chef** = **Servlet** (your business logic)
- **Kitchen helper** = **Filter** (prep work like authentication, logging)
- **Manager** = **DispatcherServlet** (Spring) — delegates to the right chef

When you type `https://api.example.com/users/42` in a browser:
1. Your browser opens a TCP connection to the server.
2. It sends an HTTP GET request.
3. The **Servlet Container** (Tomcat) receives it.
4. Tomcat maps the URL to a **Servlet** (in Spring, that's `DispatcherServlet`).
5. The Servlet processes the request, talks to the database, builds a response.
6. Tomcat sends the HTTP response back to your browser.

**The J2EE (Java EE / Jakarta EE) stack:**
- **Servlet API** — the foundation. Defines how Java handles HTTP.
- **JSP** — HTML with embedded Java (legacy, mostly replaced by Thymeleaf/React).
- **WebSocket** — for real-time.
- **JAX-RS** — for REST APIs (alternative to Spring MVC).
- **JSF** — component-based UI framework.

**Spring MVC sits ON TOP of the Servlet API.** You can't have Spring MVC without Servlets.

**FAANG Discussion Point:**  
*"Why did Spring choose the Servlet API as its foundation?"* — Because it was the **only mature, standardized 
way** to handle HTTP in Java. The Servlet spec is the contract between your app and any Java web server. Spring's 
value-add is making it **less painful** (no `web.xml`, no manual HTTP parsing).

---

<a name="2-servlet-api-basics"></a>
## 2. The Servlet API: Foundation of J2EE Web

A **Servlet** is a Java class that handles HTTP requests. The interface is simple:

```java
public interface Servlet {
    void init(ServletConfig config) throws ServletException;
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException;
    void destroy();
    ServletConfig getServletConfig();
    String getServletInfo();
}
```

In practice, you extend `HttpServlet`:

```java
public class HelloServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().write("Hello, World!");
    }
}
```

**Key interfaces/classes:**
- `HttpServlet` — base class with `doGet`, `doPost`, `doPut`, `doDelete`.
- `HttpServletRequest` — represents the incoming request (headers, params, body, session).
- `HttpServletResponse` — represents the outgoing response (status, headers, body writer).
- `ServletConfig` — initialization parameters for this servlet.
- `ServletContext` — application-wide shared state (one per web app).

**FAANG Discussion Point:**  
*"Is Servlet thread-safe?"* — **No.** A single Servlet instance handles **multiple concurrent requests**. You must 
use **local variables** (per-request state in `service()`, `doGet()`, etc.) rather than instance fields. The 
container reuses one Servlet instance for the lifetime of the app.

---

<a name="3-servlet-container-internals"></a>
## 3. The Servlet Container (Tomcat) — Internals

A **Servlet Container** (e.g., **Apache Tomcat**, Jetty, WildFly) is a **Java program** that:
1. Listens on a TCP port (e.g., 8080).
2. Parses incoming HTTP requests.
3. Maps them to Servlets using **deployment descriptors** (`web.xml`) or annotations (`@WebServlet`).
4. Manages Servlet **lifecycle** (init → service → destroy).
5. Handles **threading**, **session management**, and **resource pooling**.

**Tomcat's internal architecture:**
```
┌──────────────────────────────────────────────────────────────┐
│                      APACHE TOMCAT                            │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Server (top-level Catalina component)                 │ │
│  │  └─ Service                                            │ │
│  │     └─ Connector (HTTP/1.1, AJP, HTTP/2)              │ │
│  │        └─ Engine                                       │ │
│  │           └─ Host (e.g., "localhost")                  │ │
│  │              └─ Context (a single web app)             │ │
│  │                 └─ Wrapper (a single Servlet)          │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

**What each component does:**
- **Server** — represents the entire Tomcat instance. One per JVM.
- **Service** — groups one or more Connectors with an Engine.
- **Connector** — handles the network protocol (HTTP). Has a `port` attribute. Default: 8080.
- **Engine** — processes requests, picks the right Host.
- **Host** — virtual host (e.g., `localhost`).
- **Context** — represents one **web application** (one WAR). Has a `contextPath` (e.g., `/myapp`).
- **Wrapper** — wraps a single Servlet.

**Tomcat's request handling pipeline (high-level):**
```
HTTP Request (TCP bytes)
    ↓
[Acceptor thread] — receives bytes, hands to a Poller
    ↓
[Poller thread] — converts bytes to HTTPRequest, finds the right Container
    ↓
[Container] — Engine → Host → Context → Wrapper
    ↓
[Wrapper.invoke(req, res)] — calls Servlet.service()
    ↓
[Servlet] — your code
    ↓
[Response written back through the same chain]
```

**FAANG Discussion Point:**  
*"How does Tomcat handle 10,000 concurrent requests with only ~200 threads?"* — Tomcat uses the **NIO connector** 
(Java New I/O, non-blocking). One **Acceptor** thread accepts new connections. A pool of **Poller** threads 
detects which connections have new data. A pool of **Worker** threads (size = `maxThreads`, default 200) actually 
runs the Servlet code. The NIO pattern means one thread can monitor many connections; only active ones consume 
worker threads.

---

<a name="4-servlet-lifecycle"></a>
## 4. Servlet Lifecycle — The Full Story

The container controls a Servlet's lifecycle:

```
┌──────────────────────────────────────────────────────────┐
│  1. CLASS LOADING                                        │
│     Container loads the Servlet class (lazy, on first    │
│     request, or eager via <load-on-startup>1</...>)     │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│  2. INSTANTIATION                                        │
│     Container calls no-arg constructor                   │
│     (Servlets MUST have a public no-arg constructor)    │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│  3. INITIALIZATION (init() called ONCE)                  │
│     - Container creates ServletConfig                    │
│     - Container calls init(ServletConfig)               │
│     - Spring's DispatcherServlet builds the entire       │
│       WebApplicationContext HERE                        │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│  4. SERVICE (called for EVERY request)                  │
│     - Container creates HttpServletRequest/Response     │
│     - Calls service(req, res) which routes to doGet(),  │
│       doPost(), etc.                                    │
│     - ⚠️ Multiple threads call service() concurrently!  │
└─────────────────────────┬────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│  5. DESTRUCTION (destroy() called ONCE at shutdown)      │
│     - Cleanup resources, close connections               │
│     - Container unregisters the Servlet                  │
└──────────────────────────────────────────────────────────┘
```

**Code example with lifecycle logging:**
```java
@WebServlet(urlPatterns = "/hello", loadOnStartup = 1)
public class HelloServlet extends HttpServlet {
    
    @Override
    public void init() throws ServletException {
        System.out.println("Servlet initialized");
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.getWriter().write("Hello!");
    }
    
    @Override
    public void destroy() {
        System.out.println("Servlet destroyed");
    }
}
```

**`loadOnStartup` values:**
- Negative or absent — lazy init (on first request).
- 0 or positive — eager init during startup. Lower number = earlier init.

**FAANG Discussion Point:**  
*"Can init() throw an exception to prevent the Servlet from being used?"* — Yes! Throwing `ServletException` (or 
`UnavailableException`) makes the Servlet unavailable. The container won't route requests to it, and may try to 
re-initialize later. This is how Spring's `DispatcherServlet` fails fast if beans are misconfigured.

---

<a name="5-request-lifecycle"></a>
## 5. How a Request Travels: HTTP → Response

The **complete end-to-end flow** when a browser hits a Servlet-based app:

```
CLIENT (Browser)
    │
    │ 1. DNS Lookup → IP address
    │ 2. TCP 3-way handshake
    │ 3. Sends HTTP request:
    │      GET /api/users/42 HTTP/1.1
    │      Host: api.example.com
    │      Accept: application/json
    │      Cookie: JSESSIONID=ABC123
    │
    ▼
[NETWORK LAYER — TCP socket on port 8080]
    │
    ▼
TOMCAT — Acceptor Thread
    │  Accepts the connection, registers with a Poller
    ▼
TOMCAT — Poller Thread
    │  Detects data on socket, reads bytes
    │  Parses HTTP/1.1 → creates Http11Processor
    ▼
[Http11Processor]
    │  Parses request line, headers, body
    │  Creates Coyote Request/Response objects
    │  Determines the target Container:
    │    Engine → Host → Context → Wrapper
    │  (URL mapping: /api/users/42 → DispatcherServlet)
    ▼
[CoyoteAdapter.service(req, res)]
    │  Adapts Coyote to Catalina
    │  Calls the Mapper to find the right Wrapper
    ▼
[StandardWrapperValve.invoke()]
    │  Allocates a thread from the Executor (thread pool)
    │  Sets up classloader, request attributes
    ▼
[FilterChain.doFilter()]
    │  Invokes ALL configured filters in order
    │  Each filter can wrap the request/response
    │  Eventually calls chain.doFilter() → next filter → Servlet
    ▼
[Servlet.service(req, res)]  ← DISPATCHERSERVLET IN SPRING
    │  HttpServlet.service() dispatches to doGet/doPost/etc.
    │  Spring's DispatcherServlet does WAY more — see Section 13
    ▼
[Controller method runs]
    │  @GetMapping("/api/users/{id}")
    │  public User getUser(@PathVariable Long id) { ... }
    │  Returns a User object (or ModelAndView, ResponseEntity)
    ▼
[Response is built]
    │  HttpMessageConverter (Jackson) → JSON bytes
    │  resp.getWriter().write(json)
    ▼
[Filter chain unwinds]
    │  post-processing in each filter
    ▼
[Coyote Response written to socket]
    ▼
[Network sends HTTP response back]
    │
    ▼
CLIENT receives:
    HTTP/1.1 200 OK
    Content-Type: application/json
    Body: {"id":42, "name":"John"}
```

**FAANG Discussion Point:**  
*"At what point is the response actually sent to the client?"* — When you call `response.getWriter().flush()` (or 
close the writer), or when the container's buffer is full. After that, **you can't change the status code or add 
headers** — the response is committed. This is a common source of bugs.

---

<a name="6-filters"></a>
## 6. Filters — Pre/Post Processing

A **Filter** is a Servlet API concept that intercepts requests **before** they reach a Servlet. They form a 
**chain** — each filter can:
- Inspect/modify the request
- Inspect/modify the response
- Decide to **abort** the chain (e.g., authentication failure)

```java
@WebFilter(urlPatterns = "/*")
public class LoggingFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        long start = System.currentTimeMillis();
        HttpServletRequest httpReq = (HttpServletRequest) req;
        System.out.println("→ " + httpReq.getMethod() + " " + httpReq.getRequestURI());
        
        chain.doFilter(req, res); // pass to the next filter / servlet
        
        long duration = System.currentTimeMillis() - start;
        System.out.println("← Completed in " + duration + "ms");
    }
}
```

**Filter chain execution:**
```
Request → Filter1.doFilter() 
            → Filter2.doFilter() 
                → Filter3.doFilter() 
                    → Servlet.service()
                ← returns to Filter3 (post-processing)
            ← returns to Filter2
        ← returns to Filter1
Response sent
```

**Registration in `web.xml`:**
```xml
<filter>
    <filter-name>loggingFilter</filter-name>
    <filter-class>com.example.LoggingFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>loggingFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

**FAANG Discussion Point:**  
*"Where does Spring Security's authentication happen — Filter or Interceptor?"* — **Filter chain** (specifically 
`DelegatingFilterProxy` / `FilterChainProxy`). Authentication must happen **before** `DispatcherServlet` runs, 
before any URL-to-handler resolution. Filters see the raw HTTP; Interceptors see Spring-resolved handlers.

---

<a name="7-listeners"></a>
## 7. Listeners — Event-Driven Hooks

**Listeners** react to **lifecycle events** in the Servlet container:

| Listener | Event | Use Case |
|---|---|---|
| `ServletContextListener` | App startup/shutdown | Initialize connection pools, log startup |
| `HttpSessionListener` | Session created/destroyed | Track active users |
| `ServletRequestListener` | Request start/end | Request-scoped logging |

```java
@WebListener
public class AppStartupListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("App started: " + sce.getServletContext().getContextPath());
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("App shutting down");
    }
}
```

**FAANG Discussion Point:**  
*"When does contextInitialized() run relative to Spring's startup?"* — It runs **before** any Servlet init. 
Listeners are initialized in declaration order, then Servlets (in `loadOnStartup` order). In Spring Boot, the 
`ApplicationContext` is created **inside** `DispatcherServlet.init()`, which happens after listeners.

---

<a name="8-threading-model"></a>
## 8. Threading Model: One Thread Per Request

The Servlet spec uses a **thread-per-request** model:
1. Container has a **thread pool** (default Tomcat: 200 max threads).
2. When a request arrives, a thread is **borrowed** from the pool.
3. The thread runs the entire Filter chain → Servlet → response.
4. The thread is **returned** to the pool when done.

```
Thread Pool (200 threads)
┌──┬──┬──┬──┬──┬──┬──┬──┐
│T1│T2│T3│T4│T5│T6│T7│T8│  ← idle threads
└──┴──┴──┴──┴──┴──┴──┴──┘

Request arrives → T1 picks it up
Request arrives → T2 picks it up
...
All threads busy → new requests QUEUE (acceptCount=100 default)
Queue full → clients get "Connection Refused"
```

**Configuration in `server.xml` (Tomcat):**
```xml
<Connector port="8080" 
           maxThreads="200" 
           minSpareThreads="25"
           acceptCount="100" 
           connectionTimeout="20000" />
```

**Critical implications:**
- **Long-running requests block threads.** If 10 requests are slow, you lose 10 threads.
- **Servlet instance is shared** across all threads. Don't store request-specific data in instance fields.
- **For high concurrency**, use async Servlets (`Servlet 3.0+`):
  ```java
  @WebServlet(urlPatterns = "/long-task", asyncSupported = true)
  public class LongTaskServlet extends HttpServlet {
      @Override
      protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
          AsyncContext ctx = req.startAsync();
          ctx.start(() -> {
              // do work in a separate thread
              try {
                  Thread.sleep(5000);
                  ctx.getResponse().getWriter().write("Done");
              } catch (Exception e) { /* ... */ }
              finally { ctx.complete(); }
          });
      }
  }
  ```

**FAANG Discussion Point:**  
*"How does Spring MVC differ from async Servlets?"* — Spring MVC 5+ supports async controllers via `Callable<T>` 
or `DeferredResult<T>` / `SseEmitter`. The thread is **released back to the pool** while the work completes, then 
a new thread is used to write the response. This is the basis for **Server-Sent Events** and **WebSocket** 
integration in Spring.

---

<a name="9-session-management"></a>
## 9. Session Management

A **Session** (`HttpSession`) is a server-side storage tied to a client via a cookie (`JSESSIONID` by default in 
Tomcat). It survives across multiple requests.

**Lifecycle:**
1. First request: client has no `JSESSIONID` cookie.
2. Servlet calls `req.getSession(true)` → container generates an ID, creates a session, sends `Set-Cookie: 
JSESSIONID=ABC123` in response.
3. Subsequent requests: client sends `Cookie: JSESSIONID=ABC123`.
4. Container looks up the session by ID.
5. Session has a timeout (default 30 min). Container destroys it after inactivity.

**Code:**
```java
HttpSession session = request.getSession(); // get or create
session.setAttribute("user", user);
User user = (User) session.getAttribute("user");
session.invalidate(); // logout
```

**Session replication in clusters:**
For horizontal scaling, sessions can be:
- **Sticky sessions** — load balancer always sends the same client to the same server.
- **Replicated** — Tomcat broadcasts session changes to all nodes (expensive).
- **External store** — Redis, Memcached (best for stateless microservices).

**FAANG Discussion Point:**  
*"Should you store JWTs in HttpSession or localStorage?"* — **Neither is perfect.** HttpSession is server-stateful 
(defeats statelessness). LocalStorage is XSS-vulnerable. Best practice: **stateless JWT in HttpOnly cookie** or 
short-lived access token + refresh token in memory.

---

<a name="10-dispatch-vs-redirect"></a>
## 10. Dispatching & Forwards vs Redirects

**Server-side forward** (`RequestDispatcher.forward()`):
- Same request, same URL bar.
- Faster (no client round-trip).
- Used in MVC for view rendering.

```java
RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/views/home.jsp");
rd.forward(request, response);
```

**Redirect** (`response.sendRedirect()`):
- New request, new URL bar.
- Used after POST to avoid double-submission (Post-Redirect-Get pattern).
- Status 302 (or 303 for HTTP/1.1).

**Spring MVC equivalents:**
```java
// Forward (default in Spring MVC with view resolver)
return "home"; // forward to /WEB-INF/views/home.jsp

// Redirect
return "redirect:/home";
```

**FAANG Discussion Point:**  
*"Why is the Post-Redirect-Get pattern important?"* — After a form POST, if the user refreshes the page, the 
browser re-submits the form → duplicate order/charge. Redirect changes the URL to a GET endpoint, so refresh 
re-fetches (idempotent) instead of re-submitting.

---

<a name="11-dispatcher-servlet"></a>
## 11. Spring's DispatcherServlet: The Front Controller

In Spring MVC, **all** requests go through `DispatcherServlet` (a single Servlet, mapped to `/` in Spring Boot). 
It's the **front controller** pattern.

**Why a single front controller?**
- **Centralized dispatching** — one place to handle URL → handler mapping.
- **Cross-cutting concerns** — security, CORS, i18n applied uniformly.
- **Decoupling** — controllers don't know about each other; the dispatcher wires them up.

**What `DispatcherServlet` IS:**
- A Spring bean (singleton, lazy by default).
- Extends `FrameworkServlet` → `HttpServletBean` → `HttpServlet`.
- Holds a `WebApplicationContext` (child of root context).
- Has **9 special beans** (the `DispatcherServlet` strategy beans) it looks up by type.

**The 9 strategy beans:**
| Bean | Purpose |
|---|---|
| `MultipartResolver` | Parses `multipart/form-data` |
| `LocaleResolver` | Resolves the user's locale (i18n) |
| `ThemeResolver` | Resolves the theme (UI look) |
| `HandlerMapping` | Maps URL to handler |
| `HandlerAdapter` | Invokes the handler method |
| `HandlerExceptionResolver` | Maps exceptions to responses |
| `RequestToViewNameTranslator` | Derives view name from request |
| `ViewResolver` | Resolves view name to View object |
| `FlashMapManager` | Manages flash attributes (Post-Redirect-Get) |

**FAANG Discussion Point:**  
*"What happens if you don't define a HandlerMapping?"* — Spring provides **defaults** (e.g., 
`RequestMappingHandlerMapping` for `@RequestMapping`). If multiple are present, you can set their `order`. Spring 
uses all of them; the first one that returns a non-null handler wins.

---

<a name="12-dispatcher-bootstrap"></a>
## 12. DispatcherServlet Internals: Bootstrapping

When does `DispatcherServlet` initialize?

**Old style (web.xml):**
```xml
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

**Modern Spring Boot style:** Auto-configured. `DispatcherServletAutoConfiguration` registers it for `/` and sets 
`loadOnStartup=1`.

**The initialization sequence (`init()`):**
```java
// Pseudocode of DispatcherServlet.init() flow
@Override
public void init() {
    // 1. Create WebApplicationContext (or get from ContextLoaderListener)
    this.webApplicationContext = createWebApplicationContext(rootContext);
    
    // 2. Call refresh() — Spring context lifecycle
    webApplicationContext.refresh();
    
    // 3. Look up the 9 strategy beans
    this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
    this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
    this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
    this.handlerMappings = initHandlerMappings(context);  // can be multiple
    this.handlerAdapters = initHandlerAdapters(context);  // can be multiple
    this.handlerExceptionResolvers = initHandlerExceptionResolvers(context);
    this.requestToViewNameTranslator = ...;
    this.viewResolvers = initViewResolvers(context);
    this.flashMapManager = ...;
}
```

**FAANG Discussion Point:**  
*"Is DispatcherServlet thread-safe?"* — **Yes**, because it stores **only references to strategy beans** (which 
are themselves thread-safe singletons). The actual request/response objects are per-request (stored in 
`ThreadLocal` or method parameters).

---

<a name="13-nine-step-flow"></a>
## 13. The 9-Step Request Flow in Spring MVC

`DispatcherServlet.doDispatch()` is the heart. Here's what happens for **every** request:

```java
// Conceptual pseudocode of doDispatch()
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    
    // 1. Multipart parsing
    processedRequest = checkMultipart(request);
    multipartParsed = (processedRequest != request);
    
    // 2. Determine handler
    mappedHandler = getHandler(processedRequest); // consults HandlerMappings
    
    if (mappedHandler == null) {
        noHandlerFound(processedRequest, response); // 404
        return;
    }
    
    // 3. Determine HandlerAdapter
    HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
    
    // 4. Pre-handle interceptors
    if (!mappedHandler.applyPreHandle(processedRequest, response)) {
        return; // interceptor returned false
    }
    
    // 5. Actually invoke the controller method
    ModelAndView mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
    
    // 6. Apply default view name (if none returned)
    applyDefaultViewName(processedRequest, mv);
    
    // 7. Post-handle interceptors
    mappedHandler.applyPostHandle(processedRequest, response, mv);
    
    // 8. Process dispatch result (view resolution, exception handling)
    processDispatchResult(processedRequest, response, mappedHandler, mv, exception);
    
    // 9. Trigger afterCompletion in interceptors (cleanup, metrics)
    mappedHandler.triggerAfterCompletion(processedRequest, response, null);
}
```

**The 9 phases:**
1. **Multipart resolution** — parse `multipart/form-data` if needed.
2. **Handler lookup** — `HandlerMapping` returns a `HandlerExecutionChain` (handler + interceptors).
3. **Adapter lookup** — find the `HandlerAdapter` that can invoke this handler.
4. **Pre-handle** — `HandlerInterceptor.preHandle()` runs.
5. **Handler invocation** — adapter calls the controller method (via reflection).
6. **Default view name** — if method returned null, derive view name from URL.
7. **Post-handle** — `HandlerInterceptor.postHandle()` runs.
8. **Dispatch result** — view resolution, exception resolution, response writing.
9. **After-completion** — `HandlerInterceptor.afterCompletion()` runs.

**FAANG Discussion Point:**  
*"What if a handler returns null?"* — If a `@RequestMapping` method returns `void` or null, Spring uses 
`RequestToViewNameTranslator` to derive a view name from the URL. For REST (`@RestController`), the response is 
written by `HttpMessageConverter`, and a null return becomes an empty body with the configured status.

---

<a name="14-key-components"></a>
## 14. Key Components: HandlerMapping, Adapter, ViewResolver

### `HandlerMapping`
Maps URL + method + headers to a handler. Built-in: `RequestMappingHandlerMapping` (scans `@RequestMapping`).

```java
// What it does internally
public class RequestMappingHandlerMapping {
    private Map<RequestMappingInfo, Method> mappingRegistry;
    
    public HandlerMethod getHandlerInternal(HttpServletRequest request) {
        RequestMappingInfo info = ...; // build from URL, method, headers
        Method method = mappingRegistry.get(info);
        return new HandlerMethod(bean, method);
    }
}
```

### `HandlerAdapter`
Invokes the handler. Built-in: `RequestMappingHandlerAdapter` (handles `@Controller` methods).

```java
// What it does
public ModelAndView handle(HttpServletRequest req, HttpServletResponse res, Object handler) {
    HandlerMethod hm = (HandlerMethod) handler;
    
    // 1. Resolve arguments (@PathVariable, @RequestParam, @RequestBody)
    Object[] args = resolveArguments(req, hm);
    
    // 2. Invoke method via reflection
    Object returnValue = hm.getMethod().invoke(hm.getBean(), args);
    
    // 3. Handle return value (@ResponseBody, ModelAndView, etc.)
    return handleReturnValue(returnValue, ...);
}
```

### `ViewResolver`
Resolves view name (string) to actual `View` object. Built-in: `InternalResourceViewResolver` (JSP), 
`ThymeleafViewResolver`.

```java
public View resolveViewName(String viewName, Locale locale) {
    if (viewName.startsWith("redirect:")) return new RedirectView(...);
    if (viewName.startsWith("forward:")) return new InternalResourceView(...);
    return new InternalResourceView("/WEB-INF/views/" + viewName + ".jsp");
}
```

### `HttpMessageConverter`
Serializes/deserializes HTTP body. Built-ins:
- `MappingJackson2HttpMessageConverter` — JSON (Jackson).
- `Jaxb2RootElementHttpMessageConverter` — XML (JAXB).
- `StringHttpMessageConverter` — plain text.
- `ByteArrayHttpMessageConverter` — bytes.
- `ResourceHttpMessageConverter` — file downloads.

**FAANG Discussion Point:**  
*"What is the order of HttpMessageConverter selection?"* — Iterates through registered converters. The first one 
whose `canRead(Class, MediaType)` returns true wins. For JSON, it's always Jackson (if Jackson is on the 
classpath). For form data, it's `AllEncompassingFormHttpMessageConverter`.

---

<a name="15-architecture-diagrams"></a>
## 15. Architecture & Flow Diagrams

### **15.1 High-Level Architecture**

```
┌──────────────────────────────────────────────────────────────┐
│                       BROWSER / CLIENT                       │
└─────────────────────────────┬────────────────────────────────┘
                              │ HTTP Request
                              ▼
        ┌──────────────────────────────────────────┐
        │        SERVLET CONTAINER (Tomcat)        │
        │                                          │
        │  ┌──────────────────────────────────┐   │
        │  │   NIO Connector (port 8080)     │   │
        │  │   - Acceptor thread              │   │
        │  │   - Poller threads               │   │
        │  │   - Worker thread pool (max 200) │   │
        │  └────────────┬─────────────────────┘   │
        │               │                          │
        │               ▼                          │
        │  ┌──────────────────────────────────┐   │
        │  │   Engine → Host → Context        │   │
        │  │        → Wrapper                 │   │
        │  └────────────┬─────────────────────┘   │
        └───────────────┼──────────────────────────┘
                        │
                        ▼
        ┌──────────────────────────────────────────┐
        │     [SERVLET FILTERS]                     │
        │   - Spring Security                      │
        │   - CORS                                │
        │   - Logging                             │
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────┐
        │          DispatcherServlet                   │
        │         (Front Controller)                   │
        │                                              │
        │  ┌────────────────────────────────────────┐ │
        │  │  9 Strategy Beans:                     │ │
        │  │  • HandlerMapping                      │ │
        │  │  • HandlerAdapter                      │ │
        │  │  • ViewResolver                        │ │
        │  │  • HandlerExceptionResolver            │ │
        │  │  • MultipartResolver                   │ │
        │  │  • LocaleResolver                      │ │
        │  │  • ThemeResolver                       │ │
        │  │  • RequestToViewNameTranslator         │ │
        │  │  • FlashMapManager                     │ │
        │  └────────────────────────────────────────┘ │
        └────────────┬───────────────────────────────┘
                     │
                     ▼
        ┌──────────────────────────────────────────┐
        │    [HANDLER INTERCEPTORS]                │
        │   preHandle → postHandle →               │
        │   afterCompletion                        │
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────┐
        │   @Controller method                     │
        │   returns ModelAndView / User / ...     │
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────┐
        │   HttpMessageConverter (Jackson)         │
        │   OR ViewResolver → View render          │
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────┐
        │   [HANDLER INTERCEPTORS]                 │
        │   afterCompletion                        │
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────┐
        │     [SERVLET FILTERS]                     │
        │   Response post-processing              │
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
                              HTTP Response
```

### **15.2 Tomcat Internal Threading**

```
┌────────────────────────────────────────────────────┐
│  Acceptor Thread (1)                                │
│  - Accepts new TCP connections                     │
│  - Registers with Poller                           │
└─────────────────────┬──────────────────────────────┘
                      │
                      ▼
┌────────────────────────────────────────────────────┐
│  Poller Thread Pool (1-2)                          │
│  - Monitors NIO channels for new data              │
│  - Hands active connections to Worker threads      │
└─────────────────────┬──────────────────────────────┘
                      │
                      ▼
┌────────────────────────────────────────────────────┐
│  Worker Thread Pool (maxThreads=200)               │
│  - Executes Filter chain + Servlet                │
│  - Returns to pool when done                       │
└────────────────────────────────────────────────────┘
```

### **15.3 DispatcherServlet 9-Step Flow**

```
HTTP Request
    │
    ▼
[1] MultipartResolver.checkMultipart()
    │ (if multipart/form-data, parse into MultipartFile)
    ▼
[2] HandlerMapping.getHandler()
    │ (returns HandlerExecutionChain with interceptors)
    ▼
[3] HandlerAdapter.getHandlerAdapter()
    │ (selects adapter for the handler type)
    ▼
[4] HandlerInterceptor.preHandle()  ← can abort with false
    │
    ▼
[5] HandlerAdapter.handle()
    │ ├─ Resolve method args (@PathVariable, @RequestBody)
    │ ├─ Invoke controller method via reflection
    │ └─ Wrap return value (ModelAndView, @ResponseBody)
    ▼
[6] RequestToViewNameTranslator
    │ (if view name is null, derive from URL)
    ▼
[7] HandlerInterceptor.postHandle()
    │
    ▼
[8] processDispatchResult()
    │ ├─ If exception → HandlerExceptionResolver
    │ └─ Else → ViewResolver → View.render()
    ▼
[9] HandlerInterceptor.afterCompletion()
    │ (cleanup, metrics — even on exception)
    │
    ▼
HTTP Response
```

### **15.4 Lifecycle of a Spring MVC Application**

```
Application Startup
    │
    ▼
[1] Tomcat starts
    │
    ▼
[2] ContextLoaderListener (if configured) creates root WebApplicationContext
    │ (loads @Configuration classes, starts beans)
    │
    ▼
[3] DispatcherServlet.init() called
    │ ├─ Creates child WebApplicationContext
    │ ├─ Scans @Controller, @RestController, @RequestMapping
    │ ├─ Initializes 9 strategy beans
    │ └─ Registers HandlerMapping with all @RequestMapping methods
    │
    ▼
[4] Application ready — accepting requests
    │
    ▼
[5] Request arrives → Thread allocated → Full MVC flow
    │
    ▼
[6] App shutdown
    │ ├─ @PreDestroy callbacks
    │ ├─ DispatcherServlet.destroy()
    │ ├─ Context closed
    │ └─ Tomcat stops
```

### **15.5 Request Through the Container Stack**

```
Browser Request
    ↓
┌─────────────────────────┐
│ Tomcat Connector (NIO)  │  ← Network layer
└──────────┬──────────────┘
           ↓
┌─────────────────────────┐
│ Coyote → Catalina       │  ← Protocol layer
└──────────┬──────────────┘
           ↓
┌─────────────────────────┐
│ Servlet Filters         │  ← Pre-processing
└──────────┬──────────────┘
           ↓
┌─────────────────────────┐
│ DispatcherServlet       │  ← Spring's front controller
└──────────┬──────────────┘
           ↓
┌─────────────────────────┐
│ HandlerInterceptors     │  ← Spring's MVC layer
└──────────┬──────────────┘
           ↓
┌─────────────────────────┐
│ @Controller method      │  ← Your business code
└──────────┬──────────────┘
           ↓
(Return path: same stack, reversed)
```

---

<a name="16-faang-points"></a>
## 16. FAANG Discussion Points (The Top 15)

### 1. **Thread-Per-Request vs Reactive**
> *"Why does Spring MVC use thread-per-request? What are the tradeoffs?"*

- **Pro:** Simple mental model, blocking APIs are familiar.
- **Con:** Each thread consumes ~1MB stack memory. 200 threads = ~200MB. Hard to scale beyond a few thousand 
concurrent connections per node.
- **Alternative:** Spring WebFlux (reactive, non-blocking, event-loop). Netflix, LinkedIn use it for high-fanout 
services.

### 2. **Why Servlets Are Singleton + Stateful**
> *"Is the Servlet a singleton? What if it stores state?"*

Yes, **single instance** for the app lifetime. Storing request-specific data in fields = bug. Use **method-local 
variables** or **ThreadLocal**.

### 3. **Filter vs Interceptor**
> *"Where would you put authentication logic?"*

- **Filter** — Spring Security, CORS, request logging. Runs before MVC, sees raw HTTP.
- **Interceptor** — Performance metrics, request auditing. Has access to the matched handler.

### 4. **DispatcherServlet as a Single Point**
> *"What's the advantage of a front controller?"*

Centralizes URL mapping, exception handling, view resolution, and cross-cutting concerns. Alternative: each URL 
mapped to its own Servlet (legacy J2EE pattern). Spring chose front controller for cohesion.

### 5. **Async vs Sync**
> *"When would you use @Async controller methods?"*

For **long-running requests** (file processing, external API calls, batch jobs). The thread is released back to 
the pool; the response is written by a different thread when the work completes. Use `Callable<T>`, 
`DeferredResult<T>`, or `SseEmitter` for streaming.

### 6. **Session Replication**
> *"How do you scale Spring MVC across multiple nodes?"*

- **Sticky sessions** — load balancer routes by JSESSIONID.
- **External session store** — Redis (`spring-session-data-redis`).
- **Stateless tokens** — JWT in cookie, no server-side session.

### 7. **Why NIO Connector?**
> *"Why is Tomcat's NIO connector better than the old BIO one?"*

NIO uses **one thread per many connections** (selector-based). BIO uses **one thread per connection** (wasteful). 
NIO scales to 10K+ connections with 200 threads.

### 8. **DispatcherServlet Lazy vs Eager Init**
> *"What's loadOnStartup=1 vs the default?"*

`loadOnStartup=1` (Spring Boot's default) **eagerly initializes** at startup. Catches bean wiring errors early. 
Without it, the first request triggers init (fail-slow, not fail-fast).

### 9. **The 9 Strategy Beans**
> *"Can you override ViewResolver or HandlerMapping?"*

Yes — define your own bean. Spring auto-detects. For example, add a `ThymeleafViewResolver` and JSP pages stop 
working (or set `order` for both).

### 10. **MultipartResolver**
> *"How do you handle file uploads > 1GB?"*

- Default `MultipartResolver` loads the file into memory (up to threshold) or disk. For huge files, use 
**streaming** (`MultipartFile.getInputStream()` and write directly to S3) or **pre-signed URLs** (client uploads 
directly to S3, bypassing Spring).

### 11. **HandlerExceptionResolver Chain**
> *"How are exceptions handled in Spring MVC?"*

Three resolvers in order:
1. `ExceptionHandlerExceptionResolver` (your `@ExceptionHandler` / `@ControllerAdvice`).
2. `ResponseStatusExceptionResolver` (your `@ResponseStatus` annotations).
3. `DefaultHandlerExceptionResolver` (Spring's built-in mappings).

### 12. **HandlerMethodArgumentResolver**
> *"How does Spring resolve @RequestParam and @RequestBody?"*

Built-in resolvers for each annotation type. You can write custom resolvers for custom annotations (e.g., 
`@CurrentUser` for authenticated principal).

### 13. **DispatcherServlet's WebApplicationContext**
> *"Why does DispatcherServlet have its own context?"*

The **root context** holds services, repositories, infrastructure beans. The **web context** (child) holds 
controllers, view resolvers, HandlerMappings. This separation lets you reuse the root context in non-web apps 
(e.g., batch jobs).

### 14. **Static Resources in Spring MVC**
> *"How does /resources/static/js/app.js get served?"*

By default, `DispatcherServlet` is mapped to `/`, including static resources. To avoid the MVC overhead, configure 
`addResourceHandlers()` in `WebMvcConfigurer` to serve them via the container directly.

### 15. **What Happens at HTTP 404?**
> *"Where does the 404 response come from?"*

If no `@RequestMapping` matches, `DispatcherServlet` calls `noHandlerFound()`. If `throwExceptionIfNoHandlerFound` 
is true, `NoHandlerFoundException` is thrown (caught by `DefaultHandlerExceptionResolver` → 404). Otherwise, 
Tomcat's default 404 is used.

---

<a name="17-followup"></a>
## 17. Top 20 Follow-up Questions & Answers

### **Q1: How does Tomcat differ from WildFly / Jetty?**
**A:** All implement the Servlet spec. **Tomcat** is lightweight (Servlet + JSP only). **Jetty** is embeddable, 
async-first. **WildFly** is full Java EE (EJB, JMS, JTA). Spring Boot supports all three 
(`spring-boot-starter-tomcat`, `jetty`, `undertow`).

### **Q2: What is the difference between ServletContext and ServletConfig?**
**A:** **ServletConfig** is per-Servlet (init params for one Servlet). **ServletContext** is per-web-app (shared 
across all Servlets). ServletContext is created first; ServletConfig is created with each Servlet.

### **Q3: Can a Servlet be reloaded?**
**A:** Yes, but **disabled by default in production**. Set `<Context reloadable="true">` in `context.xml`. Tomcat 
watches `WEB-INF/classes` and `WEB-INF/lib` for changes and reloads the context (destructive, loses sessions).

### **Q4: What happens if a Servlet is accessed before init() completes?**
**A:** Container **queues** the request. The first thread completes init; subsequent requests proceed normally. If 
init() throws an exception, all queued requests get 500.

### **Q5: Why is HttpServletRequest not thread-safe?**
**A:** It's a per-request object, accessed by **only one thread** at a time. It's not designed to be shared. Don't 
pass it to other threads without synchronization.

### **Q6: What is the `web.xml` deployment descriptor?**
**A:** XML file in `WEB-INF/` that configures Servlets, filters, listeners, error pages, and security constraints. 
**Optional in Servlet 3.0+** (replaced by annotations).

### **Q7: How do you handle exceptions thrown in a Filter?**
**A:** Filters are **outside** Spring MVC, so `@ControllerAdvice` doesn't catch them. Options:
1. Catch and write response in the filter.
2. Use Spring Security's `AuthenticationEntryPoint` / `AccessDeniedHandler` for auth failures.
3. Inject `HandlerExceptionResolver` into the filter.

### **Q8: What is the difference between Servlet 3.0 and 3.1?**
**A:** **3.0** added async (`@WebServlet(asyncSupported=true)`), annotations, programmatic config. **3.1** added 
non-blocking I/O. **4.0** added HTTP/2, server push.

### **Q9: How does Spring Boot embed Tomcat?**
**A:** `spring-boot-starter-web` includes `spring-boot-starter-tomcat`, which adds Tomcat as a dependency. 
`TomcatServletWebServerFactory` creates a `Tomcat` instance programmatically. No external Tomcat needed; `java 
-jar app.jar` runs it.

### **Q10: What is the difference between `request.getParameter()` and `request.getAttribute()`?**
**A:** **getParameter** — query string / form data (string values, from client). **getAttribute** — server-side 
request attributes (any object, set by filters, interceptors, or controllers). Attributes are scoped to the 
request.

### **Q11: Can you write your own HttpMessageConverter?**
**A:** Yes — implement `HttpMessageConverter<T>`, register it via `WebMvcConfigurer.configureMessageConverters()`. 
Used for custom formats (Protobuf, Avro, YAML).

### **Q12: How does Spring handle HTTP HEAD and OPTIONS requests?**
**A:** **HEAD** is auto-handled by Spring — invokes the GET method, discards the body. **OPTIONS** is handled by 
`DispatcherServlet` itself, returning `Allow` header with supported methods.

### **Q13: What is the `getServletContext().getRealPath()` used for?**
**A:** Converts a **webapp-relative** path (e.g., `/WEB-INF/upload`) to a **filesystem-relative** path. Useful for 
file I/O. **In WAR deployments**, it returns the unpacked path. In exploded/dev mode, it returns the actual 
directory.

### **Q14: How do you prevent session fixation attacks?**
**A:** Change the session ID after authentication. Spring Security does this automatically via 
`session-fixation-protection`. The old session ID is invalidated; a new one is created.

### **Q15: What is the difference between ServletContextListener and @PostConstruct?**
**A:** **ServletContextListener** runs at the **web app** level, before any Servlet. **`@PostConstruct`** runs at 
the **bean** level, when the bean is created. They serve different scopes.

### **Q16: Can Spring MVC work without web.xml?**
**A:** Yes! Since Servlet 3.0, you can use `AbstractAnnotationConfigDispatcherServletInitializer` (Java config) or 
Spring Boot (auto-config). No `web.xml` needed.

### **Q17: How does DispatcherServlet handle a request for a static resource?**
**A:** By default, `DispatcherServlet` handles `/` (everything). Static resources are matched by 
`ResourceHttpRequestHandler` (a built-in handler). The `addResourceHandlers()` config controls it.

### **Q18: What is a Welcome File?**
**A:** A file served when the user hits the context root (e.g., `/`). Configured in `web.xml`:
```xml
<welcome-file-list>
    <welcome-file>index.html</welcome-file>
    <welcome-file>index.jsp</welcome-file>
</welcome-file-list>
```

### **Q19: How do you enable HTTP/2 in Spring Boot?**
**A:** Set `server.http2.enabled=true` (default in 2.4+). Requires Tomcat 9+ (Servlet 4.0+). HTTPS is required for 
HTTP/2 in most browsers.

### **Q20: What is the difference between WAR and JAR deployment?**
**A:** **WAR** (Web Application Archive) — deployed to external Tomcat/WildFly. Has `WEB-INF/web.xml`. **JAR** 
(Spring Boot executable JAR) — embedded Tomcat, `java -jar app.jar`. Modern Spring Boot prefers JAR for 
microservices.

---

## 🎯 Summary: The Mental Model

To summarize, here is the complete flow in 5 sentences:

1. **Tomcat** receives the HTTP request via NIO and allocates a worker thread.
2. The thread executes the **Filter chain** (security, CORS, logging).
3. **DispatcherServlet** looks up the `@RequestMapping` via `HandlerMapping`, picks a `HandlerAdapter`, runs 
interceptors, and invokes the controller method via reflection.
4. The return value is converted to JSON (Jackson) or rendered as a view (Thymeleaf).
5. The response travels back through the same chain, the thread returns to the pool, and HTTP response bytes are 
flushed to the client.

That is the **entire** life of a Spring MVC request, from TCP socket to `@GetMapping` and back. 🌍