# 🚀 Spring Web MVC Mastery — Interview Guide (25 Questions Deep Dive)

---

## 1. What is the Spring MVC Framework?

Spring MVC (Model-View-Controller) is a **web framework** built on the Servlet API that follows the MVC design 
pattern. It provides a **front controller** (`DispatcherServlet`) that dispatches HTTP requests to appropriate 
handlers (controllers), processes the request, and returns a response. It's part of the broader Spring Framework 
and integrates seamlessly with the Spring IoC container.

**The MVC responsibilities:**
- **Model** — `User`, `Product`, `Order` (POJOs / DTOs / Entities)
- **View** — Thymeleaf templates, JSP, JSON (REST)
- **Controller** — `@Controller` / `@RestController` with `@RequestMapping` methods

**Maven dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**FAANG Discussion Point:**  
*"Is Spring MVC still relevant in a reactive world?"* — Absolutely! Spring MVC uses **blocking I/O** with 
thread-per-request. For high-concurrency apps, Spring offers **Spring WebFlux** (reactive, non-blocking). FAANG 
companies use MVC for most CRUD services and WebFlux for streaming/high-fanout workloads.

**Follow-up Q: How does Spring MVC differ from Spring WebFlux?**  
- **MVC**: Servlet API, blocking, thread-per-request, `@Controller`.  
- **WebFlux**: Reactive Streams, non-blocking, event-loop, `@RestController` with `Mono`/`Flux`. They share many 
annotations (`@GetMapping`, etc.) but have different internals.

---

## 2. What is DispatcherServlet?

`DispatcherServlet` is the **front controller** in Spring MVC — a single Servlet that receives **every** HTTP 
request to the web application. It delegates the request to appropriate handler components and returns the 
response. It's the **central dispatcher** of the entire framework.

**Key responsibilities:**
1. Receives every HTTP request (mapped to `/` by default)
2. Resolves the correct **handler** (Controller method)
3. Applies **HandlerInterceptors**, **filters**
4. Resolves the **View** (for traditional MVC)
5. Renders the response

**Declared in `web.xml` (legacy) or auto-configured by Spring Boot:**

```java
// Spring Boot auto-configures this. But conceptually:
public class MyWebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override
    protected Class<?>[] getRootConfigClasses() { return new Class[]{RootConfig.class}; }
    @Override
    protected Class<?>[] getServletConfigClasses() { return new Class[]{WebConfig.class}; }
    @Override
    protected String[] getServletMappings() { return new String[]{"/"}; }
}
```

**FAANG Discussion Point:**  
*"Why use a front controller pattern instead of multiple servlets?"* — Centralizes concerns: security, logging, 
CORS, locale resolution, exception handling. Avoids the "servlet sprawl" problem. All cross-cutting concerns have 
one entry point.

**Follow-up Q: Can you have multiple DispatcherServlets in one app?**  
Yes, but rare. Each has its own `WebApplicationContext`. Used when you have **separate, isolated web modules** 
(e.g., admin UI vs public site with different configs).

---

## 3. How does DispatcherServlet work internally? (THE most asked question)

The **request processing flow** involves 9 key components orchestrated by `DispatcherServlet`:

```
HTTP Request
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. HandlerMapping        → finds which @Controller method   │
│ 2. HandlerInterceptor    → preHandle()                      │
│ 3. HandlerExecutionChain → wraps the handler + interceptors │
│ 4. HandlerAdapter        → invokes the controller method    │
│ 5. @Controller method    → returns ModelAndView / @ResponseBody│
│ 6. HttpMessageConverter  → serializes Java objects to JSON   │
│ 7. ViewResolver          → maps view name to template        │
│ 8. View                  → renders HTML (Thymeleaf, JSP)     │
│ 9. HandlerInterceptor    → postHandle() / afterCompletion() │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
HTTP Response
```

**Detailed step-by-step:**

1. **Request hits DispatcherServlet** → `doService()` is called.
2. `DispatcherServlet` consults `HandlerMapping` (e.g., `RequestMappingHandlerMapping`) to find which controller 
method matches the URL.
3. Returns a `HandlerExecutionChain` (handler + interceptors).
4. `preHandle()` of `HandlerInterceptor`s is called.
5. `DispatcherServlet` picks a `HandlerAdapter` (`RequestMappingHandlerAdapter` for `@Controller`).
6. The adapter **invokes the controller method**, resolving `@RequestParam`, `@PathVariable`, `@RequestBody`, etc.
7. Controller returns a value (or `ModelAndView`).
8. If `@ResponseBody` / `@RestController` → `HttpMessageConverter` serializes the return value to JSON.
9. Else → `ViewResolver` resolves the view name to a template.
10. `View` renders the response.
11. `postHandle()` runs. `afterCompletion()` runs after the response is written.

**Code (handler method):**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

**FAANG Discussion Point:**  
*"What if you call `System.out.println` in `postHandle`? Does it run after the response is sent to the client?"* — 
`postHandle` runs **before** the response is written. `afterCompletion` runs **after**, so it's the place for 
cleanup, logging, or metrics (e.g., request duration).

**Follow-up Q: How does DispatcherServlet get wired with Spring's beans?**  
It has its own `WebApplicationContext` (child of root context). It uses the context to look up `HandlerMapping`, 
`HandlerAdapter`, etc. — these are themselves beans.

---

## 4. What is the @Controller annotation?

`@Controller` is a **stereotype annotation** that marks a class as a Spring MVC controller. It's a specialization 
of `@Component`, so it's auto-discovered by component scanning. Methods in a `@Controller` typically return **view 
names** (strings) that get resolved to templates.

```java
@Controller
@RequestMapping("/web/users")
public class UserWebController {
    
    @GetMapping("/list")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users/list"; // resolved to /WEB-INF/views/users/list.html
    }
}
```

**Difference from `@RestController`:**
- `@Controller` returns view names (HTML)
- `@RestController` = `@Controller` + `@ResponseBody` on every method (returns JSON/XML)

**FAANG Discussion Point:**  
*"Why does Spring distinguish @Controller and @RestController?"* — To make the **intent explicit**. A team can see 
at a glance which controllers serve HTML vs JSON. It's also **migration-friendly**: change `@Controller` to 
`@RestController` when adding a JSON API to an existing app.

**Follow-up Q: Can a method in @Controller return JSON?**  
Yes — annotate it with `@ResponseBody`. Or use `ResponseEntity<User>` for full HTTP control.

---

## 5. What is the @RestController annotation?

`@RestController` is a **convenience annotation** introduced in Spring 4.0. It's a meta-annotation: `@Controller` 
+ `@ResponseBody` on every method. It's the standard for **RESTful web services**.

```java
@RestController
@RequestMapping("/api/users")
public class UserRestController {
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody @Valid UserDTO dto) {
        User created = userService.create(dto);
        return ResponseEntity
            .created(URI.create("/api/users/" + created.getId()))
            .body(created);
    }
}
```

**Equivalent to:**
```java
@Controller
@RequestMapping("/api/users")
public class UserRestController {
    @GetMapping("/{id}")
    @ResponseBody
    public User getUser(@PathVariable Long id) { ... }
}
```

**FAANG Discussion Point:**  
*"Why return ResponseEntity instead of the raw object?"* — `ResponseEntity` lets you set **status code, headers, 
and body** explicitly. Raw object returns 200 OK with body, which is often not what you want (especially for 201 
Created or 204 No Content).

**Follow-up Q: What's the difference between @RestController and @Controller + @ResponseBody on class level?**  
Functionally identical. `@RestController` is just shorter. The bytecode-level behavior is the same.

---

## 6. What is @RequestMapping?

`@RequestMapping` is the **primary mapping annotation** that associates HTTP requests with controller methods. It 
can be applied at **class level** (for URL prefix) and **method level** (for specific endpoints).

```java
@Controller
@RequestMapping("/api/orders") // class-level prefix
public class OrderController {
    
    @RequestMapping(
        value = "/{id}",
        method = RequestMethod.GET, // can be GET, POST, PUT, DELETE, PATCH
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Order getOrder(@PathVariable Long id) { ... }
}
```

**HTTP method shortcuts (preferred):**
- `@GetMapping` = `@RequestMapping(method = GET)`
- `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`

**FAANG Discussion Point:**  
*"How does Spring resolve conflicts when two @RequestMapping match the same URL?"* — Ambiguous mapping → 
`IllegalStateException` at startup. The container picks the **most specific** match (e.g., `/users/me` wins over 
`/users/{id}` for the path `/users/me`).

**Follow-up Q: What's the difference between `consumes` and `produces`?**  
- `consumes` — the request body Content-Type (e.g., `application/json`).
- `produces` — the response Content-Type the client accepts (e.g., `application/json` or `*/*`).

---

## 7. @GetMapping vs @PostMapping vs @RequestMapping(method=...)?

The shortcut annotations are **convenience meta-annotations** that hardcode the HTTP method:

| Annotation | Equivalent |
|---|---|
| `@GetMapping("/x")` | `@RequestMapping(value="/x", method=GET)` |
| `@PostMapping("/x")` | `@RequestMapping(value="/x", method=POST)` |
| `@PutMapping("/x")` | `@RequestMapping(value="/x", method=PUT)` |
| `@DeleteMapping("/x")` | `@RequestMapping(value="/x", method=DELETE)` |
| `@PatchMapping("/x")` | `@RequestMapping(value="/x", method=PATCH)` |

**Why use shortcuts?**
- **Readability** — `@GetMapping` is shorter and intent is obvious.
- **Type-safety** — Less chance of forgetting the `method` attribute.
- **Tooling** — IDEs and OpenAPI generators recognize them.

**Code example:**
```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping           // GET /api/products
    public List<Product> all() { ... }
    
    @GetMapping("/{id}")  // GET /api/products/123
    public Product one(@PathVariable Long id) { ... }
    
    @PostMapping          // POST /api/products
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody Product p) { ... }
    
    @PutMapping("/{id}")  // PUT /api/products/123
    public Product update(@PathVariable Long id, @RequestBody Product p) { ... }
    
    @DeleteMapping("/{id}") // DELETE /api/products/123
    public void delete(@PathVariable Long id) { ... }
}
```

**FAANG Discussion Point:**  
*"Why is it better to use @GetMapping than @RequestMapping?"* — Cleaner code, less verbose, and many libraries 
(OpenAPI/Swagger generators) specifically look for the shortcut annotations to generate API documentation.

**Follow-up Q: Can you combine multiple HTTP methods on one method?**  
Only with `@RequestMapping` and a method array: `@RequestMapping(value="/x", method={GET, POST})`. Not recommended 
— use separate methods for clarity.

---

## 8. What are @PathVariable and @RequestParam?

**`@PathVariable` — extracts from URL path:**
```java
@GetMapping("/users/{id}/orders/{orderId}")
public Order getOrder(
    @PathVariable Long id,
    @PathVariable Long orderId) { ... }
```
URL: `/users/5/orders/99` → `id=5, orderId=99`

**`@RequestParam` — extracts from query string:**
```java
@GetMapping("/users")
public List<User> search(
    @RequestParam(required = false) String name,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) { ... }
```
URL: `/users?name=John&page=0&size=20` → `name="John", page=0, size=20`

**Matrix variables** (less common):
```java
@GetMapping("/cars/{id}")
public String getCar(
    @PathVariable String id,
    @MatrixVariable Map<String, String> attrs) { ... }
```
URL: `/cars/33;color=red;year=2024`

**FAANG Discussion Point:**  
*"When do you use @PathVariable vs @RequestParam?"* —  
- **`@PathVariable`** for **resource identifiers** (`/users/123`).  
- **`@RequestParam`** for **filters, sort, pagination, search** (`/users?name=John&page=2`).  
- **REST best practice**: path = resource, query = operation on resource.

**Follow-up Q: What if a @RequestParam is missing?**  
By default, Spring throws `MissingServletRequestParameterException` → 400 Bad Request. Use `required = false` to 
make it optional. Use `defaultValue` to provide a fallback.

---

## 9. What is @RequestBody and @ResponseBody?

**`@RequestBody` — deserializes the HTTP request body to a Java object:**
```java
@PostMapping("/users")
public User createUser(@RequestBody @Valid UserDTO dto) {
    return userService.create(dto);
}
```
Spring uses `HttpMessageConverter` (Jackson for JSON, JAXB for XML) to parse the body.

**`@ResponseBody` — serializes the return value to the HTTP response body:**
```java
@GetMapping("/users/{id}")
@ResponseBody
public User getUser(@PathVariable Long id) {
    return userService.findById(id); // serialized to JSON
}
```

**Content-Type negotiation:**  
Spring uses the `Accept` header from the client to pick the converter:
- `Accept: application/json` → Jackson → `User` → JSON
- `Accept: application/xml` → JAXB → XML

**Validation:**  
Combine with `@Valid` to trigger Bean Validation:
```java
public class UserDTO {
    @NotBlank @Email private String email;
    @Size(min = 8) private String password;
}
```

**FAANG Discussion Point:**  
*"How does Spring decide which HttpMessageConverter to use?"* — Order matters: `ByteArrayHttpMessageConverter` → 
`StringHttpMessageConverter` → `ResourceHttpMessageConverter` → `AllEncompassingFormHttpMessageConverter` (form 
data) → `MappingJackson2HttpMessageConverter` (JSON, if Jackson is on classpath). First one whose 
`canRead`/`canWrite` returns true wins.

**Follow-up Q: What's the difference between @RequestParam and @RequestBody?**  
- `@RequestParam` — key=value pairs in URL query string or form data.  
- `@RequestBody` — raw body (typically JSON/XML). Used for `POST`/`PUT`/`PATCH` with complex payloads.

---

## 10. What is ResponseEntity?

`ResponseEntity<T>` is a wrapper that gives you **full control** over the HTTP response: status code, headers, and 
body. It's the recommended return type for REST APIs that need more than just `200 OK`.

```java
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return userService.findById(id)
        .map(user -> ResponseEntity.ok()
            .header("X-Custom-Header", "value")
            .body(user))
        .orElse(ResponseEntity.notFound().build());
}

@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody @Valid UserDTO dto) {
    User created = userService.create(dto);
    URI location = URI.create("/api/users/" + created.getId());
    return ResponseEntity.created(location).body(created);
}
```

**Builder methods:**
- `ResponseEntity.ok(body)` → 200
- `ResponseEntity.created(uri)` → 201
- `ResponseEntity.accepted()` → 202
- `ResponseEntity.noContent()` → 204
- `ResponseEntity.badRequest()` → 400
- `ResponseEntity.notFound()` → 404
- `ResponseEntity.status(HttpStatus.CONFLICT).body(...)` → 409

**FAANG Discussion Point:**  
*"When do you return ResponseEntity vs @ResponseStatus?"* —  
- **`ResponseEntity`** — need headers, dynamic status, conditional logic.  
- **`@ResponseStatus(HttpStatus.CREATED)`** — simple, fixed status.  
- **Direct object** — always 200 OK with body. Use only for read-only endpoints that can't fail.

**Follow-up Q: What's the difference between @ResponseStatus and ResponseEntity?**  
`@ResponseStatus` is a declarative annotation on a method/exception. `ResponseEntity` is programmatic and dynamic. 
Both end up setting the status code, but `ResponseEntity` lets you conditionally set headers, etc.

---

## 11. What is @ModelAttribute?

`@ModelAttribute` binds **request parameters** (form data or query string) to a **Java object**. It's primarily 
used in **traditional MVC** (HTML form submissions), not REST APIs.

```java
@PostMapping("/users")
public String createUser(@ModelAttribute UserDTO user) {
    userService.create(user);
    return "redirect:/users";
}

@ModelAttribute
public void populateCommon(Model model) {
    model.addAttribute("roles", roleService.findAll());
}
```

**How it works:**
1. Spring creates a new `UserDTO` instance.
2. It binds each request parameter (`?name=John&email=john@x.com`) to a setter (`setName`, `setEmail`).
3. The populated object is passed to the method.

**FAANG Discussion Point:**  
*"Why prefer @ModelAttribute for forms but @RequestBody for APIs?"* — Forms send 
`application/x-www-form-urlencoded` (key=value pairs), perfect for `@ModelAttribute`. APIs send JSON, which 
`@ModelAttribute` can't parse. The reverse: `@RequestBody` on form data won't work.

**Follow-up Q: Can @ModelAttribute be used with @RequestBody?**  
Not on the same parameter. They handle different content types. Use `@ModelAttribute` for 
`application/x-www-form-urlencoded`, `@RequestBody` for `application/json`.

---

## 12. What is a ViewResolver?

`ViewResolver` resolves **view names** (strings returned from controllers) into actual `View` objects that render 
the response. Used in traditional MVC (not REST).

```java
public interface ViewResolver {
    View resolveViewName(String viewName, Locale locale) throws Exception;
}
```

**Common implementations:**
- `InternalResourceViewResolver` — JSP, HTML
- `ThymeleafViewResolver` — Thymeleaf templates
- `FreeMarkerViewResolver` — FreeMarker
- `GroovyMarkupViewResolver` — Groovy templates

**Configuration example:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".html");
        return resolver;
    }
}
```

**Controller method:**
```java
@GetMapping("/users")
public String listUsers(Model model) {
    model.addAttribute("users", userService.findAll());
    return "users/list"; // resolves to /WEB-INF/views/users/list.html
}
```

**FAANG Discussion Point:**  
*"How does content negotiation affect ViewResolver?"* — In a pure REST API (with `@ResponseBody`), the 
`ViewResolver` is bypassed. The `HttpMessageConverter` writes the response directly. In mixed apps, you can have 
both, with content negotiation choosing based on `Accept` header.

**Follow-up Q: What is a chained ViewResolver?**  
Multiple resolvers ordered by priority. Spring tries each in order. The first one that returns a non-null `View` 
wins. Used when you have multiple view technologies (e.g., JSP + Thymeleaf fallback).

---

## 13. What are HandlerInterceptors?

`HandlerInterceptor` is a **filter-like mechanism** scoped to Spring MVC handlers (controllers). It runs **after** 
Servlet filters but **before** the controller method.

**Three methods:**
```java
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, 
                              Object handler) {
        System.out.println("Before: " + req.getRequestURI());
        return true; // false = abort the request
    }
    
    @Override
    public void postHandle(HttpServletRequest req, HttpServletResponse res, 
                            Object handler, ModelAndView mav) {
        System.out.println("After handler, before view render");
    }
    
    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, 
                                  Object handler, Exception ex) {
        System.out.println("After everything (response sent)");
    }
}
```

**Registration:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor())
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/public/**");
    }
}
```

**Interceptors vs Filters:**

| Aspect | Filter | Interceptor |
|---|---|---|
| **Scope** | All Servlets | Spring MVC handlers only |
| **Access to** | Raw `HttpServletRequest` | Handler info (method, model) |
| **Runs** | Before/after DispatcherServlet | Around the controller method |
| **Spring beans** | ❌ Manual lookup | ✅ Dependency injection |
| **Use case** | Security, encoding, CORS | Logging, auth, performance |

**FAANG Discussion Point:**  
*"Why use Interceptor instead of Filter for authentication?"* — Interceptors are **Spring-aware** (DI, access to 
handler info), and run **after** the URL is resolved to a controller. Filters are general-purpose and run for all 
requests (static resources included).

**Follow-up Q: Can an interceptor stop the request?**  
Yes — return `false` from `preHandle`. The controller method never runs. You can write the response directly 
(`res.getWriter().write("Forbidden")`).

---

## 14. What is @ControllerAdvice?

`@ControllerAdvice` is a specialization of `@Component` that allows you to handle **exceptions globally**, bind 
request parameters globally, or modify the model before any controller executes. It's applied to all `@Controller` 
/ `@RestController` classes in the application.

**Use cases:**
1. **`@ExceptionHandler`** — global exception → JSON error response
2. **`@InitBinder`** — register custom property editors
3. **`@ModelAttribute`** — populate common model attributes

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("USER_NOT_FOUND", ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_FAILED", errors.toString()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "Something went wrong"));
    }
}
```

**FAANG Discussion Point:**  
*"How do you order multiple @ControllerAdvice classes?"* — Use `@Order(1)`, `@Order(2)`, etc. Lower order = higher 
priority. The advice with the most specific exception handler wins.

**Follow-up Q: What's the difference between @ControllerAdvice and @RestControllerAdvice?**  
`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`. The latter is the standard for JSON APIs. Use 
`@ControllerAdvice` only if you need to return views (rare).

---

## 15. What is HandlerExceptionResolver?

`HandlerExceptionResolver` is the **lower-level** interface for resolving exceptions in Spring MVC. 
`@ControllerAdvice` is built on top of it. There are several built-in implementations:

1. **`ExceptionHandlerExceptionResolver`** — processes `@ExceptionHandler` (in `@Controller` or 
`@ControllerAdvice`).
2. **`ResponseStatusExceptionResolver`** — handles `@ResponseStatus` annotations on exceptions.
3. **`DefaultHandlerExceptionResolver`** — converts Spring MVC exceptions to status codes (e.g., 
`MissingServletRequestParameterException` → 400).
4. **Custom resolvers** — implement the interface to add custom logic.

**FAANG Discussion Point:**  
*"How would you implement a custom HandlerExceptionResolver?"*  
```java
@Component
public class CustomExceptionResolver implements HandlerExceptionResolver {
    @Override
    public ModelAndView resolveException(HttpServletRequest req, HttpServletResponse res, 
                                          Object handler, Exception ex) {
        // log, transform, or delegate
        return null; // null = let next resolver try
    }
}
```

**Follow-up Q: Why does Spring have 3 default resolvers?**  
Each handles a different exception source. By default, all 3 run in order. The first one that returns a non-null 
`ModelAndView` wins.

---

## 16. What is a Filter, and how is it different from an Interceptor?

**`Filter`** is a **Servlet API** concept (Jakarta Servlet). It runs **before** `DispatcherServlet` and can 
intercept **all** requests (including static resources, JSPs, etc.).

**Common use cases:**
- Security (Spring Security uses a `FilterChainProxy`)
- CORS
- Request/response logging
- Character encoding
- GZIP compression

```java
@Component
public class RequestLoggingFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        long start = System.currentTimeMillis();
        chain.doFilter(req, res); // pass to next filter
        long duration = System.currentTimeMillis() - start;
        System.out.println("Request took " + duration + "ms");
    }
}
```

**Interceptors** are **Spring MVC** concepts. They run after filters and only for matched controllers.

**Execution order:**
```
HTTP Request
    ↓
[Servlet Filters]              ← e.g., Spring Security, CORS
    ↓
DispatcherServlet
    ↓
[HandlerInterceptors]          ← preHandle
    ↓
[@Controller method]
    ↓
[HandlerInterceptors]          ← postHandle
    ↓
[HandlerInterceptors]          ← afterCompletion
    ↓
[Servlet Filters]              ← response post-processing
    ↓
HTTP Response
```

**FAANG Discussion Point:**  
*"Where would you put authentication — Filter or Interceptor?"* — **Filter** (specifically `DelegatingFilterProxy` 
or Spring Security's `FilterChainProxy`). Authentication must happen before the request enters MVC, before any 
handler resolution. Spring Security uses filters for this exact reason.

**Follow-up Q: Can a Filter inject Spring beans?**  
Not directly. The `Filter` interface is created by the Servlet container, not Spring. Use `DelegatingFilterProxy` 
to delegate to a Spring bean, or use `@Component` on a `Filter` (Spring Boot auto-registers it).

---

## 17. What is Multipart File Upload? How to handle it in Spring?

Spring MVC supports **multipart file uploads** for `multipart/form-data` requests. The `MultipartResolver` parses 
the request and binds uploaded files to `MultipartFile` parameters.

**HTML form:**
```html
<form method="POST" enctype="multipart/form-data" action="/upload">
    <input type="file" name="file" />
    <button>Upload</button>
</form>
```

**Controller:**
```java
@RestController
public class FileUploadController {
    
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) 
            throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("Empty file");
        
        Path dest = Paths.get("/uploads/" + file.getOriginalFilename());
        Files.copy(file.getInputStream(), dest);
        return ResponseEntity.ok("Uploaded: " + file.getSize() + " bytes");
    }
    
    @PostMapping("/upload-multiple")
    public ResponseEntity<String> uploadMultiple(
            @RequestParam("files") MultipartFile[] files) {
        for (MultipartFile f : files) { /* process */ }
        return ResponseEntity.ok("Uploaded " + files.length + " files");
    }
}
```

**Configuration limits (application.properties):**
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
spring.servlet.multipart.file-size-threshold=1MB
```

**FAANG Discussion Point:**  
*"How does Spring parse multipart requests?"* — Two implementations: `StandardServletMultipartResolver` (uses 
Servlet 3.0+ built-in support, **default in Spring Boot**) and `CommonsMultipartResolver` (uses Apache Commons 
FileUpload, deprecated). The former is preferred for performance.

**Follow-up Q: How do you upload large files (>1GB) in Spring?**  
Use **streaming** — set `spring.servlet.multipart.resolve-lazily=true` and use `StreamingResponseBody` for the 
response. For very large files, offload to S3 with pre-signed URLs and don't stream through Spring at all.

---

## 18. What is Content Negotiation?

Content negotiation is the process where **server picks the response format** (JSON, XML, HTML) based on the 
client's `Accept` header (or URL suffix as fallback).

**Mechanisms (in priority order):**
1. **`Accept` header** (HTTP standard): `Accept: application/json`
2. **URL extension**: `/users.json`, `/users.xml`
3. **Query parameter**: `/users?format=json`
4. **Default**: configured `defaultContentType`

**Configuration:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .defaultContentType(MediaType.APPLICATION_JSON)
            .favorPathExtension(true)
            .favorParameter(true)
            .parameterName("format")
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML);
    }
}
```

**FAANG Discussion Point:**  
*"Should you use URL extensions (.json, .xml) or Accept header?"* — **Accept header is the REST standard** (Roy 
Fielding's thesis). URL extensions are legacy and break REST principles. Most modern APIs (GitHub, Stripe) use 
Accept header exclusively.

**Follow-up Q: What if the client sends `Accept: */*`?**  
Server returns the **default content type** (usually `application/json` for APIs). You can also configure 
**multiple representations** for the same endpoint by registering different `HttpMessageConverter`s.

---

## 19. What is @SessionAttributes and @SessionAttribute?

**`@SessionAttribute`** — reads an attribute from `HttpSession`:
```java
@GetMapping("/dashboard")
public String dashboard(@SessionAttribute("user") User user) {
    return "dashboard"; // user came from a previous controller
}
```

**`@SessionAttributes`** — class-level, stores model attributes in the session:
```java
@Controller
@RequestMapping("/checkout")
@SessionAttributes("cart")
public class CheckoutController {
    
    @ModelAttribute("cart")
    public Cart cart() { return new Cart(); }
    
    @PostMapping("/add")
    public String addItem(@RequestParam Long itemId, @ModelAttribute Cart cart) {
        cart.add(itemId);
        return "cart";
    }
}
```

**FAANG Discussion Point:**  
*"When should you use HttpSession vs @SessionAttribute?"* — `@SessionAttribute` is **type-safe** and 
**Spring-managed**. Use it for known session objects. For arbitrary session storage (e.g., flash messages), use 
`HttpSession` directly.

**Follow-up Q: What's the difference between session scope and request scope?**  
- **Request scope** — bean lives for one HTTP request (`@Scope("request")`).  
- **Session scope** — bean lives for the user's session (`@Scope("session")`).  
- **Application scope** — bean lives for the entire app runtime.

---

## 20. What is CORS, and how to configure it in Spring?

**CORS (Cross-Origin Resource Sharing)** is a browser security feature that restricts HTTP requests initiated from 
scripts to a different origin (domain, port, or scheme). Spring provides declarative CORS support.

**Problem scenario:**
- Frontend: `https://app.example.com` (port 443)
- API: `https://api.example.com` (port 443)

These are different origins. Browser blocks AJAX requests unless the API sends specific headers.

**Solution 1: Method-level @CrossOrigin:**
```java
@RestController
@CrossOrigin(origins = "https://app.example.com")
public class UserController {
    @GetMapping("/users")
    public List<User> all() { ... }
}
```

**Solution 2: Global configuration:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://app.example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

**Solution 3: Spring Security:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(request -> {
        CorsConfig config = new CorsConfig();
        config.addAllowedOrigin("https://app.example.com");
        config.addAllowedMethod("*");
        return config;
    }));
    return http.build();
}
```

**FAANG Discussion Point:**  
*"What's the difference between CORS and CSRF?"* —  
- **CORS** — browser-side mechanism to allow/block cross-origin AJAX.  
- **CSRF** — server-side mechanism to prevent forged form submissions.  
They're complementary. CORS doesn't replace CSRF protection; they're different threat models.

**Follow-up Q: What is a preflight request (OPTIONS)?**  
Before sending a "complex" CORS request (non-simple method like `PUT`, or custom headers), the browser sends an 
`OPTIONS` request to check if the server allows it. Spring handles this automatically if CORS is configured.

---

## 21. What is the difference between @RequestMapping and @GetMapping (and other shortcuts)?

Already covered in Q7. The shortcuts are **meta-annotations** that hardcode the HTTP method. They are 
**semantically clearer** and **less error-prone** than `@RequestMapping(method=...)`.

**Advanced tip:** You can create your own custom annotations:
```java
@RequestMapping(method = RequestMethod.GET, produces = "application/json")
public @interface JsonGet {
    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] value() default {};
}
```

**Follow-up Q: Are there other shortcut annotations?**  
- `@PatchMapping`  
- `@RequestMapping` with no method (matches any HTTP method)  
- Custom shortcuts via meta-annotations

---

## 22. What is the @Valid annotation and how does validation work?

`@Valid` (or `@Validated` for Spring-specific features) triggers **Bean Validation** (JSR-380) on the method 
parameter. Constraints are declared on the DTO and checked automatically.

**DTO with validation:**
```java
public class CreateUserRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50)
    private String name;
    
    @NotNull @Email
    private String email;
    
    @Min(18) @Max(120)
    private int age;
    
    @Pattern(regexp = "^[A-Z]{2}$")
    private String country;
}
```

**Controller:**
```java
@PostMapping("/users")
public ResponseEntity<User> create(@RequestBody @Valid CreateUserRequest req) {
    return ResponseEntity.ok(userService.create(req));
}
```

**If validation fails:** `MethodArgumentNotValidException` is thrown. Handle it in `@ControllerAdvice`:

```java
@RestControllerAdvice
public class ValidationHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handle(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
```

**FAANG Discussion Point:**  
*"What is the difference between @Valid and @Validated?"* —  
- **`@Valid`** (JSR-380) — standard, supports cascading (`@Valid` on nested objects).  
- **`@Validated`** (Spring) — adds **validation groups** (`@Validated(SaveGroup.class)`).  
Use `@Valid` for simple cases; use `@Validated` when you need different validation rules for different scenarios 
(create vs update).

**Follow-up Q: How do you handle validation for path variables and query params?**  
Add `@Validated` at class level and constraints on parameters:
```java
@RestController
@Validated
public class UserController {
    @GetMapping("/users/{id}")
    public User get(@PathVariable @Min(1) Long id) { ... }
}
```
Failed validation throws `ConstraintViolationException`.

---

## 23. What is the difference between Spring MVC and Spring Boot?

| Aspect | Spring MVC | Spring Boot |
|---|---|---|
| **What** | Web framework (DispatcherServlet, controllers) | Auto-config + opinionated setup |
| **Dependency** | `spring-webmvc` | `spring-boot-starter-web` |
| **Configuration** | Manual (web.xml, dispatcher config) | Auto-configured |
| **Embedded server** | External (Tomcat WAR in Tomcat) | Embedded (Tomcat in JAR) |
| **Bootstrapping** | Deploy WAR to external server | `java -jar app.jar` |
| **Production features** | Manual (metrics, health) | Built-in (Actuator) |

**Spring Boot is opinionated defaults + auto-configuration on top of Spring MVC.** When you add 
`spring-boot-starter-web`, it pulls in Spring MVC + an embedded Tomcat + Jackson + validation + Actuator.

**FAANG Discussion Point:**  
*"Would you ever use Spring MVC without Spring Boot?"* — Rare. Legacy apps with `web.xml`, deployable WARs. In 
greenfield projects, **always use Spring Boot**. It removes boilerplate (no XML, no manual DispatcherServlet 
registration).

**Follow-up Q: What is spring-boot-starter-web?**  
A **Maven/Gradle starter** that aggregates: `spring-web`, `spring-webmvc`, `spring-boot-starter-tomcat` (embedded 
server), `spring-boot-starter-json` (Jackson), and `spring-boot-starter-validation` (Hibernate Validator).

---

## 24. What is the difference between @RequestParam and @PathVariable with examples?

Already covered in Q8. Recap:
- **`@PathVariable`** — part of the URL path (`/users/123` → `id=123`).
- **`@RequestParam`** — query string (`/users?name=John` → `name=John`).

**Mixed example:**
```java
@GetMapping("/users/{userId}/orders")
public List<Order> getOrders(
    @PathVariable Long userId,
    @RequestParam(required = false) String status,
    @RequestParam(defaultValue = "0") int page) {
    return orderService.find(userId, status, page);
}
```
URL: `/users/5/orders?status=SHIPPED&page=0`

**FAANG Discussion Point:**  
*"When do you choose @RequestParam over @PathVariable?"* — Path variables identify the **resource hierarchy** 
(parent → child), while query parameters are **modifiers or filters** on the resource. Example: `/orders/2024` is 
a year (path), `/orders?year=2024` is a filter (query).

**Follow-up Q: Can you have an Optional query parameter?**  
Yes — `@RequestParam Optional<String> status` or `@RequestParam(required = false) String status`. Spring handles 
both.

---

## 25. How does Spring MVC handle exceptions? (Deep dive)

Spring MVC provides **multiple layers** of exception handling, executed in a specific order:

**Layer 1: ResponseStatusExceptionResolver** — handles exceptions annotated with `@ResponseStatus`:
```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("User " + id + " not found");
    }
}
```
When thrown, Spring returns 404 automatically.

**Layer 2: DefaultHandlerExceptionResolver** — converts Spring MVC's own exceptions to status codes:
- `MissingServletRequestParameterException` → 400
- `HttpRequestMethodNotSupportedException` → 405
- `HttpMediaTypeNotSupportedException` → 415

**Layer 3: ExceptionHandlerExceptionResolver** — processes `@ExceptionHandler` (in `@Controller` or 
`@ControllerAdvice`):
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(UserNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }
}
```

**Layer 4: Custom HandlerExceptionResolver** — implement the interface for custom logic.

**Order of resolution:**
```
1. ResponseStatusExceptionResolver    (looks for @ResponseStatus)
2. DefaultHandlerExceptionResolver    (handles Spring exceptions)
3. ExceptionHandlerExceptionResolver  (looks for @ExceptionHandler)
4. Custom resolvers                   (your implementations)
```

**Modern approach — ResponseStatusException:**
Spring 5+ allows you to throw an exception with status directly:
```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "User " + id + " not found"));
}
```

**FAANG Discussion Point:**  
*"What is the best practice for exception handling in REST APIs?"* — Use `@RestControllerAdvice` with 
`@ExceptionHandler` for **business exceptions**, and `ResponseStatusException` for **edge cases**. Don't expose 
internal stack traces to clients. Log them server-side with a correlation ID.

**Follow-up Q: How do you handle exceptions from filters (before MVC)?**  
Filters run **before** `DispatcherServlet`, so `@ControllerAdvice` doesn't catch their exceptions. Solutions:
1. Manually catch and write the response in the filter.
2. Use Spring Security's `AuthenticationEntryPoint` / `AccessDeniedHandler` for auth.
3. Use `HandlerExceptionResolver` injected into the filter.

---

## 🏛️ Architecture Diagram: Spring MVC Big Picture

```
┌──────────────────────────────────────────────────────────────┐
│                      BROWSER / CLIENT                        │
└─────────────────────────────┬────────────────────────────────┘
                              │ HTTP Request
                              ▼
        ┌──────────────────────────────────────────┐
        │        SERVLET CONTAINER (Tomcat)        │
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────┐
        │         [SERVLET FILTERS]                │  ← Security, CORS, Logging
        │  Filter1 → Filter2 → ... → FilterN       │
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────────┐
        │              DispatcherServlet                       │
        │  (Front Controller — single entry point)             │
        └─────┬─────────────┬─────────────┬─────────────┬──────┘
              │             │             │             │
              ▼             ▼             ▼             ▼
        HandlerMapping  HandlerAdapter  ViewResolver  HttpMessageConverter
              │             │             │             │
              │             │             │             │
              ▼             ▼             ▼             ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
        │@Controller│  │  Invoke  │  │ Thymeleaf│  │  Jackson │
        │  method  │→ │  method  │→ │  template │→ │   JSON   │
        └──────────┘  └──────────┘  └──────────┘  └──────────┘
              │             │             │             │
              └─────────────┴─────────────┴─────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────┐
        │         [HANDLER INTERCEPTORS]            │
        │  preHandle → ... → postHandle →          │
        │              afterCompletion            │
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────┐
        │         [SERVLET FILTERS]                │  ← Response post-processing
        └─────────────────────────┬────────────────┘
                                  │
                                  ▼
                              HTTP Response
```

---

## 🔥 @RequestMapping Resolution Flow

```
HTTP GET /api/users/42
    │
    ▼
DispatcherServlet.doDispatch()
    │
    ▼
RequestMappingHandlerMapping  ← scans all @Controller beans
    │  compares URL + method + headers
    ▼
HandlerExecutionChain
    │  (handler = UserController#getUser, interceptors = [...])
    ▼
RequestMappingHandlerAdapter  ← chosen by Spring
    │  resolves arguments (@PathVariable, @RequestParam)
    ▼
Reflection.invokes Method
    │  (UserController#getUser(42L))
    ▼
Return value (User object)
    │
    ▼
HttpMessageConverter (Jackson)  ← if @ResponseBody
    │  serializes to JSON
    ▼
HTTP 200 OK
    │  Content-Type: application/json
    │  Body: {"id":42, "name":"John"}
```

---

## 🔥 Global Exception Handling Flow

```
UserController.getUser(999L)
    │  throws UserNotFoundException
    ▼
Exception bubbles up to DispatcherServlet
    │
    ▼
HandlerExceptionResolverComposite
    │
    ├──→ ResponseStatusExceptionResolver
    │      (checks if exception has @ResponseStatus)
    │
    ├──→ DefaultHandlerExceptionResolver
    │      (handles Spring's own exceptions)
    │
    └──→ ExceptionHandlerExceptionResolver
           │
           └──→ scans @ControllerAdvice for @ExceptionHandler(UserNotFoundException.class)
                 │
                 ▼
                 GlobalExceptionHandler.handleNotFound()
                 │
                 ▼
                 ResponseEntity.status(404).body(error)
                 │
                 ▼
                 HTTP 404 Not Found
                 Body: {"code":"USER_NOT_FOUND","message":"User 999 not found"}
```

---

## 🎯 Top 15 Follow-up Questions to Prepare

1. **Q:** What is the difference between `DispatcherServlet` and a regular Servlet?  
   **A:** A regular Servlet handles one specific URL pattern. `DispatcherServlet` is the **front controller** — 
handles all requests and dispatches to controllers, view resolvers, etc.

2. **Q:** Can you have multiple `DispatcherServlet`s in one app?  
   **A:** Yes, with different URL mappings. Each has its own `WebApplicationContext`. Rare in practice.

3. **Q:** How does DispatcherServlet get the `HandlerMapping`?  
   **A:** They're Spring beans. `DispatcherServlet` is initialized with `WebApplicationContext` and looks them up 
by type from the container.

4. **Q:** What is `HandlerMethodArgumentResolver`?  
   **A:** Resolves method parameters in controllers (`@PathVariable`, `@RequestParam`, `@RequestBody`, etc.). You 
can write custom resolvers for custom annotations.

5. **Q:** What is the difference between `@Controller` and `@RestController`?  
   **A:** `@RestController` = `@Controller` + `@ResponseBody` on every method. Use for JSON APIs; use 
`@Controller` for HTML templates.

6. **Q:** How does Spring decide JSON vs XML serialization?  
   **A:** By the `Accept` header. `MappingJackson2HttpMessageConverter` handles JSON, 
`Jaxb2RootElementHttpMessageConverter` handles XML.

7. **Q:** What is the difference between `HandlerInterceptor` and `Filter`?  
   **A:** `Filter` is Servlet API (runs for all requests, including static). `Interceptor` is Spring MVC (runs for 
matched handlers, has Spring DI, can access the handler method).

8. **Q:** How do you return a custom 404 page?  
   **A:** Use `errorPage` filter, or `@ControllerAdvice` with `@ExceptionHandler(NoHandlerFoundException.class)`, 
or set `spring.mvc.throw-exception-if-no-handler-found=true` and handle it.

9. **Q:** What is the difference between `@ModelAttribute` and `@RequestBody`?  
   **A:** `@ModelAttribute` binds form data (key=value) to a POJO. `@RequestBody` deserializes the raw body 
(typically JSON) to a POJO.

10. **Q:** What is `WebMvcConfigurer`?  
   **A:** Interface with `default` methods to customize Spring MVC (interceptors, CORS, message converters, view 
controllers, etc.). Implement it in a `@Configuration` class.

11. **Q:** What is `ModelAndView`?  
   **A:** Legacy return type containing both the **model** (data) and the **view name**. Used in traditional MVC. 
Modern `@Controller` methods can return `String` (view name) + `Model` parameter.

12. **Q:** How do you upload a file in Spring MVC?  
   **A:** Add `spring.servlet.multipart.enabled=true` (default in Spring Boot), accept `MultipartFile` parameter, 
use `MultipartResolver` (auto-configured). For large files, consider S3 pre-signed URLs.

13. **Q:** What is `HandlerMethod`?  
   **A:** A wrapper around a controller method, providing metadata (method, bean, annotations, parameters). Used 
by `HandlerInterceptor` and AOP.

14. **Q:** What is the difference between `ResponseEntity` and `HttpEntity`?  
   **A:** `HttpEntity` represents an HTTP request/response with body + headers. `ResponseEntity` extends it to add 
**status code**. `RequestEntity` is the request-side equivalent.

15. **Q:** How do you test a Spring MVC controller?  
   **A:** Use `MockMvc` (unit test, no real server) or `TestRestTemplate` / `WebTestClient` (integration test). 
`@WebMvcTest(UserController.class)` loads only that controller's slice.