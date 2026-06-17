# 🚀 Servlets, Tomcat, DispatcherServlet, Spring MVC & Spring Boot — Complete Deep Dive

## Table of Contents

1. 10,000 Foot Overview
2. What Problem Servlets Solve
3. Servlet Architecture
4. Servlet Container Internals (Tomcat)
5. Request Lifecycle End-to-End
6. Servlet Lifecycle
7. Threading Model
8. Filters
9. Listeners
10. Session Management
11. DispatcherServlet Deep Dive
12. Spring MVC Internal Flow
13. HandlerMapping Internals
14. HandlerAdapter Internals
15. HttpMessageConverter Internals
16. Exception Handling Flow
17. Interceptors
18. Spring Boot Internals
19. Tomcat vs Spring MVC vs Spring Boot
20. FAANG Interview Topics
21. Staff Engineer Discussion Points
22. Mental Models & Summary

---

# 1. 10,000 Foot Overview

A browser sends an HTTP request.

The request travels through:

```text
Browser
  ↓
TCP/IP
  ↓
Tomcat Connector
  ↓
Servlet Container
  ↓
Filters
  ↓
DispatcherServlet
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
  ↓
JSON/HTML Response
  ↓
Browser
```

Spring MVC does NOT replace Servlets.

Spring MVC is built ON TOP OF Servlets.

DispatcherServlet itself is a Servlet.

---

# 2. Why Servlets Were Invented

Before Servlets, CGI was common.

```text
Request
  ↓
New OS Process
  ↓
Response
```

1000 requests = 1000 processes.

Expensive.

Servlets introduced:

```text
One Java Object
      +
Thread Per Request
```

Much more scalable.

---

# 3. What is a Servlet?

Definition:

A Servlet is a Java class that receives HTTP requests and generates HTTP responses.

Example:

```java
public class UserServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp) {

        resp.getWriter().write("Hello");
    }
}
```

---

# 4. Tomcat Internal Architecture

```text
Server
 └── Service
      └── Connector
            └── Engine
                  └── Host
                        └── Context
                              └── Wrapper
                                    └── Servlet
```

## Connector

Responsible for:

- HTTP
- HTTPS
- HTTP/2
- TCP sockets

## Engine

Request processing engine.

## Host

Virtual host.

Example:

```text
api.company.com
```

## Context

Web application.

Example:

```text
/myapp
```

## Wrapper

Represents a Servlet.

---

# 5. Complete Request Lifecycle

User calls:

```http
GET /users/100
```

Flow:

```text
Browser
   ↓
DNS Lookup
   ↓
TCP Handshake
   ↓
HTTP Request
   ↓
Tomcat Connector
   ↓
Thread Allocation
   ↓
Filter Chain
   ↓
Servlet
   ↓
Business Logic
   ↓
Response
   ↓
Browser
```

---

# 6. Servlet Lifecycle

## Load

Class loaded.

## Instantiate

```java
new UserServlet()
```

Only once.

## init()

Runs once.

```java
public void init()
```

Used for:

- Configuration
- Cache Loading
- Resource Setup

## service()

Runs per request.

Internally:

```text
GET    → doGet()
POST   → doPost()
PUT    → doPut()
DELETE → doDelete()
```

## destroy()

Runs during shutdown.

---

# 7. Threading Model

Most important interview topic.

Servlets are:

```text
Singleton Objects
```

Tomcat creates one servlet instance.

Requests use different threads.

```text
Servlet Instance
       ↑
   T1 T2 T3 T4
```

Never store request data in fields.

Bad:

```java
private String username;
```

Good:

```java
String username;
```

inside methods.

---

# 8. Filters

Filters execute before Servlets.

```text
Request
 ↓
Filter1
 ↓
Filter2
 ↓
Servlet
 ↑
Filter2
 ↑
Filter1
 ↑
Response
```

Uses:

- Authentication
- Logging
- CORS
- Compression

---

# 9. Listeners

Container events.

Examples:

```text
ServletContextListener
HttpSessionListener
ServletRequestListener
```

Use cases:

- Startup logic
- Session tracking
- Request metrics

---

# 10. Session Management

Session stored server side.

Client receives:

```text
JSESSIONID
```

Flow:

```text
Request
 ↓
Create Session
 ↓
Store User Data
 ↓
Send Cookie
 ↓
Future Requests
 ↓
Reuse Session
```

---

# 11. DispatcherServlet Deep Dive

DispatcherServlet is:

```text
Front Controller
```

All requests pass through it.

```text
Browser
  ↓
Tomcat
  ↓
DispatcherServlet
  ↓
Controllers
```

Benefits:

- Centralized routing
- Validation
- Exception handling
- View resolution

---

# 12. Spring MVC Internal Flow

```text
Request
 ↓
DispatcherServlet
 ↓
HandlerMapping
 ↓
HandlerExecutionChain
 ↓
Interceptor.preHandle()
 ↓
HandlerAdapter
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
Database
 ↓
Return Value
 ↓
HttpMessageConverter
 ↓
Response
```

---

# 13. HandlerMapping Internals

At startup Spring scans:

```java
@GetMapping
@PostMapping
@RequestMapping
```

Creates registry.

```text
/users/{id}
        ↓
UserController#getUser

/orders/{id}
        ↓
OrderController#getOrder
```

Request lookup becomes very fast.

---

# 14. HandlerAdapter Internals

Different handler types exist.

DispatcherServlet does not invoke controllers directly.

It uses HandlerAdapter.

```text
Controller
    ↓
HandlerAdapter
    ↓
Reflection
    ↓
Method Invocation
```

Responsibilities:

- Resolve parameters
- Invoke method
- Process return value

---

# 15. HttpMessageConverter

Converts Java ↔ HTTP.

Example:

```java
User
```

to

```json
{
  "id":100,
  "name":"Rahul"
}
```

Common converters:

- Jackson JSON
- XML
- String
- Byte Array

---

# 16. Exception Handling

Controller throws:

```java
UserNotFoundException
```

Flow:

```text
Controller
   ↓
DispatcherServlet
   ↓
HandlerExceptionResolver
   ↓
@ControllerAdvice
   ↓
JSON Error Response
```

---

# 17. Interceptors

Spring MVC specific.

Lifecycle:

```text
preHandle()
      ↓
Controller
      ↓
postHandle()
      ↓
afterCompletion()
```

Used for:

- Auditing
- Metrics
- Tracing

---

# 18. Spring Boot Internals

Without Boot:

- web.xml
- DispatcherServlet config
- Tomcat setup
- Jackson config

With Boot:

```java
@SpringBootApplication
```

Auto configures:

- Embedded Tomcat
- DispatcherServlet
- Jackson
- Validation
- Error Handling

---

# 19. Relationship Between Components

```text
Spring Boot
      ↓
Spring MVC
      ↓
DispatcherServlet
      ↓
Servlet API
      ↓
Tomcat
      ↓
TCP/IP
```

Important:

Spring Boot is NOT a replacement for Spring MVC.

Spring Boot configures Spring MVC.

Spring MVC uses DispatcherServlet.

DispatcherServlet is a Servlet.

Servlets run inside Tomcat.

---

# 20. FAANG Interview Questions

1. Why are Servlets singleton?
2. Why are Servlets not thread safe?
3. Difference between ServletConfig and ServletContext?
4. Difference between Filter and Interceptor?
5. How does DispatcherServlet work?
6. How does HandlerMapping work?
7. How does Spring resolve @PathVariable?
8. How does Jackson serialize JSON?
9. What happens when no handler matches?
10. How does embedded Tomcat start?
11. What is maxThreads?
12. What is acceptCount?
13. What is NIO connector?
14. Difference between Tomcat and Netty?
15. Spring MVC vs WebFlux?

---

# 21. Staff Engineer Discussion

## Why Thread-Per-Request Eventually Hits Limits

Each thread consumes memory.

```text
200 Threads
≈
200MB+ Stack Memory
```

Long running requests block threads.

This led to:

```text
Reactive Programming
Spring WebFlux
Netty
```

## Why DispatcherServlet Exists

Without DispatcherServlet:

```text
100 APIs
=
100 Servlets
```

With Spring MVC:

```text
1 DispatcherServlet
100 Controllers
```

Much easier to maintain.

---

# 22. Ultimate Mental Model

```text
CLIENT
  ↓
TCP/IP
  ↓
TOMCAT CONNECTOR
  ↓
SERVLET CONTAINER
  ↓
FILTER CHAIN
  ↓
DISPATCHERSERVLET
  ↓
HANDLERMAPPING
  ↓
HANDLERADAPTER
  ↓
CONTROLLER
  ↓
SERVICE
  ↓
REPOSITORY
  ↓
DATABASE
  ↓
HTTPMESSAGECONVERTER
  ↓
JSON
  ↓
HTTP RESPONSE
  ↓
CLIENT
```

If you understand every box in this diagram and can explain what it does, why it exists, and how data flows through it, you have a strong Senior/Staff level understanding of the Servlet and Spring MVC ecosystem.
