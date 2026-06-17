# 🚀 Spring Core & Spring Framework Mastery — Interview Guide (Deep Dive)

## 1. What is IoC (Inversion of Control)?

IoC is a **foundational design principle** in software engineering where the **control flow of a program is 
inverted** — instead of your business code controlling when and how external resources, services, or objects are 
created and wired, that responsibility is handed over to a **framework or container** (in our case, the Spring IoC 
container). This principle is often summarized as the **Hollywood Principle**: *"Don't call us, we'll call you."*

**Traditional (pre-IoC) approach:**
```java
public class UserService {
    private UserRepository repo = new UserRepositoryImpl();
    private EmailService email = new EmailService();
    private static final String SMTP_HOST = "smtp.gmail.com";
    
    public void register(User u) {
        repo.save(u);
        email.send(u.getEmail(), "Welcome!");
    }
}
```
Here, `UserService` *controls* the creation of its dependencies. Tight coupling. Hard to test. Hard to swap 
implementations.

**With IoC (Spring):**
```java
@Service
public class UserService {
    private final UserRepository repo;
    private final EmailService email;
    
    @Autowired
    public UserService(UserRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }
}
```
The container decides **which** implementation to inject, **when** to instantiate it, and **how long** it lives. 
Your class just declares what it needs.

**Forms of IoC in Spring:**
- **Dependency Injection** (the most common form)
- **Dependency Lookup** (`applicationContext.getBean()`)
- **Event-driven IoC** (`ApplicationEvent` + `@EventListener`)
- **Template Method IoC** (e.g., `JdbcTemplate` calls your callback)
- **Aspect-Oriented IoC** (interceptors like `@Transactional` calling your method)

**FAANG Discussion Point:**  
Interviewers love asking: *"Is IoC unique to Spring?"* — No! IoC is a **principle**; Spring is one 
*implementation*. Others include Java EE CDI, Guice, Dagger, and even non-DI frameworks using the Template Method 
pattern. Spring's contribution was making DI mainstream in enterprise Java.

**Follow-up Q: What's the difference between IoC and the Service Locator pattern?**  
Service Locator is a *competing* pattern where a class asks a registry (`ServiceLocator.get("email")`) for its 
dependencies. It's still IoC (the locator controls lookup), but it **hides** dependencies and makes testing 
harder. Spring's `@Autowired` is preferred because dependencies are **declared**, not fetched.

---

## 2. What is DI (Dependency Injection)?

Dependency Injection is a **specific design pattern** that implements IoC by **passing dependencies into an 
object** from the outside (via constructor, setter, or field) rather than letting the object construct them. The 
goal is **decoupling** — your class shouldn't know *how* its collaborators are built, only that they exist.

**Without DI (anti-pattern):**
```java
public class OrderService {
    private final StripePaymentService payment = new StripePaymentService("sk_live_xxx");
    private final MySQLOrderRepository repo = new MySQLOrderRepository("jdbc:mysql://prod...");
}
```
Problems: impossible to unit test, can't switch to PayPal, credentials are hardcoded, multiple instances of 
`StripePaymentService` for each OrderService.

**With DI (Spring):**
```java
@Service
public class OrderService {
    private final PaymentService payment;
    private final OrderRepository repo;
    
    public OrderService(PaymentService payment, OrderRepository repo) {
        this.payment = payment;
        this.repo = repo;
    }
}
```
Now, the **type** matters, not the implementation. The container injects the right bean. Test with mocks: `new 
OrderService(mockPayment, mockRepo)`.

**Types of DI in Spring:**
1. **Constructor Injection** ✅ — dependencies passed via constructor; allows `final` fields; fails fast.
2. **Setter Injection** — dependencies via `setX()` methods; useful for optional/circular deps.
3. **Field Injection** ❌ — `@Autowired` directly on fields; hides deps, prevents `final`, harder to test.

**Real-world analogy:** Think of a coffee machine. Without DI, the machine grinds its own beans, boils its own 
water, and roasts its own beans. With DI, you *inject* coffee powder and water — the machine doesn't care where 
they came from.

**FAANG Discussion Point:**  
*"Why does Google, Amazon, and Netflix prefer constructor injection?"* — Because **immutability** (final fields) 
makes services **thread-safe by default** in a multi-threaded request environment. Stateless services scale 
horizontally; mutable state requires locks or `ConcurrentHashMap`s.

**Follow-up Q: Can you have DI without a framework?**  
Absolutely. Manual DI (also called "Pure DI" by Mark Seemann) just means `new MyService(new RealRepo(), new 
RealEmail())` in a composition root. Frameworks automate it.

---

## 3. Difference between IoC and DI?

| Aspect | IoC | DI |
|---|---|---|
| **Nature** | Design principle (philosophy) | Design pattern (concrete technique) |
| **Scope** | Broad — includes DI, events, callbacks, AOP | Narrow — specifically about injecting collaborators |
| **Achieved via** | Containers, frameworks, callbacks | Constructors, setters, fields, method params |
| **Example in Spring** | `@PostConstruct`, `ApplicationEvent` | `@Autowired`, `@Inject` |
| **Relationship** | Parent concept | Child implementation |
| **Other implementations** | Service Locator, Template Method | Constructor/Setter/Field injection |

**One-liner:** *DI is to IoC what a recipe is to cooking — one way to achieve the broader goal.*

**FAANG Discussion Point:**  
*"Can you have DI without IoC?"* — Technically yes: if you write `new Service(new Repo())` manually, you're using 
DI, but you (not a container) control the wiring. This is called **"Pure DI"** and is very popular in 
functional/Clojure communities because it's explicit and framework-free.

**Follow-up Q: How does the Template Method pattern relate to IoC?**  
In `JdbcTemplate.query(...)`, you pass a `RowMapper` callback. The template controls *when* your code runs (after 
the query, per row). That's IoC — your code is called by the framework.

---

## 4. What is a Spring Bean?

A Spring Bean is **any object whose lifecycle is managed by the Spring IoC container**. This includes objects 
created from:
- Class-level annotations: `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`, 
`@Configuration`
- Method-level: `@Bean` methods inside `@Configuration` classes
- Programmatic registration: `BeanDefinitionRegistry.registerBeanDefinition()`

**Example:**
```java
@Service
public class PaymentService {
    public Payment process(Order order) {
        // ...
    }
}
```
The moment Spring scans this class, an instance is created, dependencies are wired, and lifecycle callbacks 
(init/destroy) are invoked — all automatically.

**Bean ≠ POJO:** A POJO is just a class with no special restrictions. A **bean is a managed POJO** with extra 
metadata (name, scope, init method, etc.).

**Bean Identity:** Each bean has a name (default: class name lowercased, e.g., `paymentService`). You can have 
multiple beans of the same type with different names — useful for `@Qualifier`.

**Lifecycle of a bean:**
1. **Definition** — metadata registered (`BeanDefinition`)
2. **Instantiation** — constructor called (or factory method)
3. **Populate Properties** — DI happens
4. **Aware callbacks** — `BeanNameAware.setBeanName()`, `ApplicationContextAware.setApplicationContext()`
5. **BeanPostProcessor** — pre-initialization hooks
6. **InitializingBean / @PostConstruct / init-method** — custom init
7. **BeanPostProcessor** — post-initialization (AOP proxy creation happens here!)
8. **Ready** — bean is in use
9. **@PreDestroy / DisposableBean / destroy-method** — cleanup

**FAANG Discussion Point:**  
*"What makes a good Spring bean?"* — It should be **stateless, immutable, and side-effect-free at construction 
time**. Stateful beans cause race conditions; mutable fields break Spring's thread-safety assumption.

**Follow-up Q: Can a final class be a bean?**  
Yes! Constructor injection is required (no field injection since fields can't be set after construction). Spring 
uses CGLIB for proxying final classes (in Spring 4+).

---

## 5. What is ApplicationContext?

`ApplicationContext` is Spring's **enterprise-grade IoC container** that extends the simpler `BeanFactory` 
interface and adds support for the full Spring ecosystem. It's the **central interface** for providing 
configuration information to the application and managing the entire bean lifecycle.

**Key capabilities beyond BeanFactory:**
- **Internationalization (i18n)** — `MessageSource` for locale-specific messages
- **Event Publication** — `ApplicationEventPublisher` (publish/subscribe model)
- **Resource Loading** — `Resource` abstraction (`classpath:`, `file:`, `URL:`)
- **Environment Abstraction** — profiles (`@Profile`), property sources
- **AOP Integration** — auto-detection of `@Aspect` beans
- **Web-Aware Variants** — `WebApplicationContext` for Spring MVC

**Common implementations:**
- `AnnotationConfigApplicationContext` — for Java config
- `ClassPathXmlApplicationContext` — for XML config
- `FileSystemXmlApplicationContext` — XML from filesystem
- `WebApplicationContext` — for Spring MVC apps

```java
ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
UserService service = ctx.getBean(UserService.class);
```

**FAANG Discussion Point:**  
*"Why is `ApplicationContext` eager for singletons?"* — It creates all non-lazy singletons at startup so that 
**configuration errors surface immediately** (fail-fast principle), not when the first user hits an endpoint. A 
missing dependency on startup is far better than a 500 error in production at 3 AM.

**Follow-up Q: What's the difference between `BeanFactory` and `ApplicationContext` in terms of memory?**  
`BeanFactory` is **lazy and lightweight** — good for resource-constrained devices (IoT, mobile). 
`ApplicationContext` pre-instantiates singletons and keeps more metadata in memory, but is the right choice for 
almost all server-side apps.

---

## 6. BeanFactory vs ApplicationContext?

| Feature | `BeanFactory` | `ApplicationContext` |
|---|---|---|
| **Bean instantiation** | Lazy (on `getBean()`) | Eager for singletons |
| **AOP support** | ❌ Manual | ✅ Built-in |
| **Event handling** | ❌ No | ✅ `ApplicationEventPublisher` |
| **i18n** | ❌ No | ✅ `MessageSource` |
| **Web integration** | ❌ | ✅ `WebApplicationContext` |
| **Resource loading** | Basic | ✅ `Resource` pattern |
| **Memory footprint** | Minimal | Higher |
| **Best for** | Embedded/IoT, lazy scenarios | Enterprise/web apps |

**Eager vs Lazy implications:**
- `BeanFactory` would only fail when you call `getBean(MyService.class)` and the dep is missing.
- `ApplicationContext` fails at startup, so you can't deploy a broken app.

**FAANG Discussion Point:**  
*"Have you ever used BeanFactory directly?"* — Almost never in production. It's an internal API. Even 
`AnnotationConfigApplicationContext` wraps a `DefaultListableBeanFactory` internally. You'll see it in framework 
code but rarely write it yourself.

**Follow-up Q: Can you make `ApplicationContext` lazy?**  
Yes — use `@Lazy` on bean classes, or `lazy-init="true"` in XML. The container won't instantiate them until first 
injection.

---

## 7. Constructor vs Setter Injection?

**Constructor Injection:**
```java
@Service
public class UserService {
    private final UserRepository repo;
    private final EmailService email;
    
    public UserService(UserRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }
}
```

**Setter Injection:**
```java
@Service
public class UserService {
    private UserRepository repo;
    private EmailService email;
    
    @Autowired(required = false)
    public void setUserRepository(UserRepository repo) { this.repo = repo; }
    
    @Autowired(required = false)
    public void setEmailService(EmailService email) { this.email = email; }
}
```

| Aspect | Constructor | Setter |
|---|---|---|
| **Immutability** | ✅ Allows `final` | ❌ No `final` |
| **Required deps** | ✅ Enforced | ❌ Optional (silent) |
| **Partial injection** | ❌ All-or-nothing | ✅ Granular |
| **Testability** | ✅ Easy | ⚠️ Need setters |
| **Circular deps** | Detected at startup | Hides them |
| **Java records** | ✅ Native support | ❌ Not applicable |
| **Thread-safety** | ✅ Inherently safe | ❌ Mutable state |
| **Discovery order** | No guarantees | N/A |

**FAANG Discussion Point:**  
*"Why does Spring documentation call constructor injection 'the recommended way'?"* — Because it **aligns with 
SOLID principles** (especially Dependency Inversion), enables **immutability** (a major concurrency win), and 
supports the **fail-fast** philosophy. Setter injection was the original way (Spring 1.0); constructor injection 
became idiomatic with Java's immutability culture.

**Follow-up Q: When would you use setter injection in 2024?**  
- Optional dependencies (e.g., a `MetricsRecorder` that's nice to have but not required)
- When you have a **circular dependency** that can't be refactored (still bad — use `@Lazy`)
- When the class is **instantiated by a third party** (e.g., JPA entities, library classes)

---

## 8. Why Constructor Injection is preferred?

The Spring team's official stance: **"The Spring team generally advocates constructor injection, as it lets you 
implement application components as immutable objects and ensures that required dependencies are not null."**

**1. Immutability:** Final fields can never be reassigned. No `setX()` after construction. This is huge in 
concurrent systems.

**2. Fail-Fast:** If a dependency is missing, `BeanCreationException` happens at startup. With field injection, 
you get a `NullPointerException` at runtime — possibly in production, possibly under load.

**3. Testability:** Unit tests are trivial: `new MyService(mockRepo)`. No Spring context needed. Faster tests. No 
`@SpringBootTest` overhead.

**4. Thread-safety:** Final fields are guaranteed to be safely published across threads (JMM happens-before). No 
`@Volatile` or `synchronized` needed.

**5. Detects circular dependencies at startup:** You can't have a cycle if every dependency is required and 
injected at construction.

**6. Compatibility with Java records:**
```java
@Service
public record UserService(UserRepository repo, EmailService email) { }
```
Records force constructor-style dependency declaration. Spring 6+ supports record-based beans natively.

**7. Explicit contract:** The constructor signature *is* the dependency list. New developers can read it and 
immediately know what the class needs.

**Counterpoint — when constructor injection hurts:**
- Classes with **many dependencies** (>5) suggest SRP violation. Consider refactoring.
- **Optional** dependencies make constructor signatures messy.
- **Inheritance hierarchies** where each subclass adds 1-2 deps → "constructor telescoping."

**FAANG Discussion Point:**  
*"How do you deal with a class that genuinely needs 10+ dependencies?"* — Refactor. Either:
1. Group related deps into a value object (e.g., `MailConfig` instead of `host, port, user, pass, ssl`).
2. Split the class into smaller, focused services (SRP).
3. Use a facade pattern.

**Follow-up Q: What's "constructor telescoping" and how do you solve it?**  
When constructors chain: `A(B)`, `B(C)`, `C(D)` — code becomes unreadable. Solutions: Builder pattern, 
`@ConfigurationProperties` for config groups, or use **records** to bundle immutable config.

---

## 9. Bean Lifecycle (Deep Dive)

The Spring bean lifecycle is the sequence of steps the container follows from **bean definition registration** to 
**destruction**. Understanding this is critical for debugging bean creation issues, AOP, and transactional 
behavior.

**Full lifecycle:**
```
1. BeanDefinition parsed/scanned
2. Instantiation (constructor called or factory method invoked)
3. Populate Properties (DI happens via setters, fields, or constructor args)
4. Aware callbacks (BeanNameAware, BeanFactoryAware, ApplicationContextAware, ...)
5. BeanPostProcessor.postProcessBeforeInitialization() ← many BPPs run here
6. @PostConstruct method
7. InitializingBean.afterPropertiesSet()
8. Custom init-method (from @Bean(initMethod="..."))
9. BeanPostProcessor.postProcessAfterInitialization() ← AOP proxy created here!
10. Bean is READY and cached
11. ... application runs ...
12. @PreDestroy method
13. DisposableBean.destroy()
14. Custom destroy-method
```

**Code example:**
```java
@Component
public class MyBean implements InitializingBean, DisposableBean, 
                                BeanNameAware, ApplicationContextAware {
    private String beanName;
    private ApplicationContext ctx;
    
    @Override public void setBeanName(String name) { this.beanName = name; }
    @Override public void setApplicationContext(ApplicationContext ctx) { this.ctx = ctx; }
    
    @PostConstruct
    public void postConstruct() { System.out.println("@PostConstruct"); }
    
    @Override public void afterPropertiesSet() { System.out.println("InitializingBean"); }
    
    @PostConstruct
    public void customInit() { System.out.println("Custom init"); }
    
    @PreDestroy
    public void preDestroy() { System.out.println("@PreDestroy"); }
    
    @Override public void destroy() { System.out.println("DisposableBean"); }
}
```

**Critical fact: AOP proxies are created in `postProcessAfterInitialization`!** This is why you can't intercept a 
bean's self-calls — the proxy wraps the *object*, not the *class*.

**FAANG Discussion Point:**  
*"What happens if @PostConstruct throws an exception?"* — The bean creation fails. The container marks the bean as 
broken. Subsequent `getBean()` calls re-throw the original exception wrapped in `BeanCreationException`. The 
application fails to start.

**Follow-up Q: Why doesn't Spring call @PreDestroy on prototype beans?**  
Because Spring doesn't track prototype instances after handing them to you. The container has no reference to call 
destroy on. **Workaround:** Implement `DisposableBean` and track instances in a custom 
`DestructionAwareBeanPostProcessor`.

---

## 10. What is BeanDefinition?

`BeanDefinition` is the **internal metadata object** that describes how to create a bean. It's the *recipe* the 
container follows. Everything you write in `@Component`, `@Bean`, or XML eventually becomes a `BeanDefinition`.

**Properties stored:**
- `beanClassName` — the actual class
- `scope` — singleton, prototype, request, session
- `constructorArgumentValues` — args for constructor
- `propertyValues` — setter/field values
- `initMethodName`, `destroyMethodName`
- `autowireMode` — by type, by name, constructor
- `lazyInit` — boolean
- `role` — `ROLE_APPLICATION`, `ROLE_INFRASTRUCTURE`, `ROLE_SUPPORT`
- `primary` — for `@Primary` resolution

**Programmatic example:**
```java
DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
AbstractBeanDefinition def = BeanDefinitionBuilder
    .genericBeanDefinition(MyService.class)
    .setScope("prototype")
    .setLazyInit(true)
    .addPropertyReference("repo", "userRepository")
    .getBeanDefinition();
factory.registerBeanDefinition("myService", def);
```

**FAANG Discussion Point:**  
*"How does Spring Boot's auto-configuration use BeanDefinition?"* — Through `AutoConfiguration.imports` 
(META-INF/spring/), each auto-config class is loaded, and its `@Bean` methods produce `BeanDefinition`s. 
Conditional annotations (`@ConditionalOnClass`, `@ConditionalOnMissingBean`) determine which definitions are 
*actually registered*.

**Follow-up Q: What's the difference between BeanDefinition and BeanFactoryPostProcessor?**  
`BeanDefinition` is *data*. `BeanFactoryPostProcessor` is a *callback* that runs before bean instantiation and can 
modify `BeanDefinition`s. Example: `PropertySourcesPlaceholderConfigurer` replaces `${...}` placeholders inside 
BeanDefinition property values *before* beans are created.

---

## 11. What is Singleton Scope?

Singleton is the **default scope** in Spring. It means **one shared instance per Spring IoC container** (not per 
JVM, not per ClassLoader — per *container*). All injections of a singleton bean receive the *same* reference.

```java
@Component
@Scope("singleton") // optional, default
public class CacheService { }
```

**How it works internally:**
- `DefaultListableBeanFactory` extends `DefaultSingletonBeanRegistry`
- A `ConcurrentHashMap<String, Object> singletonObjects` stores instances
- A `singletonFactories` map (Level 3 cache) holds `ObjectFactory` lambdas for early reference
- `getBean()` checks the cache first; creates only if absent

**Critical caveat:** Spring singletons are **NOT thread-safe by default**! The container guarantees *one instance* 
but says nothing about concurrent access. You must design singleton beans to be **stateless** (no mutable instance 
fields) or use synchronization.

**FAANG Discussion Point:**  
*"How do you make a Spring singleton thread-safe?"* — Three approaches:
1. **Stateless design** (best) — no instance fields, all state in method params.
2. **`ThreadLocal`** for per-request state.
3. **`ConcurrentHashMap`** or `synchronized` blocks for shared state.

**Follow-up Q: What's the difference between a Spring singleton and a GoF singleton in a multi-container setup?**  

GoF singleton is per ClassLoader/JVM. Spring singleton is per container. With two `ApplicationContext`s, you get 
*two* Spring singletons of the same class.

---

## 12. Spring Singleton vs GoF Singleton?

| Aspect | Spring Singleton | GoF Singleton |
|---|---|---|
| **Pattern type** | Container-managed | Static factory |
| **Scope** | Per `ApplicationContext` | Per ClassLoader/JVM |
| **How instantiated** | By Spring at startup (eager) | By static block / private constructor |
| **Thread-safety** | Not guaranteed | Not guaranteed (same issue) |
| **Multiple instances possible?** | Yes (multiple contexts) | No (private constructor) |
| **Enforcement** | Container convention | Compiler (private constructor) |
| **Testing** | Easy (mock injection) | Hard (global state) |

**GoF Singleton code:**
```java
public class ClassicSingleton {
    private static final ClassicSingleton INSTANCE = new ClassicSingleton();
    private ClassicSingleton() { /* prevent external instantiation */ }
    public static ClassicSingleton getInstance() { return INSTANCE; }
}
```

**Spring Singleton code (conceptual):**
```java
@Component
public class ModernSingleton { /* just a regular class */ }
```

**FAANG Discussion Point:**  
*"Why is the GoF Singleton considered an anti-pattern?"* — It violates SRP (class controls its own creation AND 
its business logic), hides dependencies (no constructor args), and makes testing painful. Spring's singleton is 
preferred because dependencies are explicit.

**Follow-up Q: How do you get multiple instances of a Spring singleton?**  
Start multiple `ApplicationContext`s. Each has its own bean cache. In integration tests with `@SpringBootTest`, 
contexts are usually shared within a test class but isolated between test classes.

---

## 13. What is Prototype Scope?

Prototype scope means **a new instance is created every time the bean is requested** (via `getBean()` or injection 
lookup).

```java
@Component
@Scope("prototype")
public class TaskRunner { 
    private final UUID id = UUID.randomUUID();
    public UUID getId() { return id; }
}
```

Each time you inject or fetch this bean, you get a fresh `TaskRunner` with a new UUID.

**Critical quirk:** Spring does **NOT** call `@PreDestroy` on prototype beans! The container has no reference 
after handing them to you.

**Workaround for proper cleanup:**
```java
@Component
public class PrototypeCleanupBPP implements DestructionAwareBeanPostProcessor, 
                                              BeanPostProcessor {
    private final List<DisposableBean> prototypes = new ArrayList<>();
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if (bean instanceof DisposableBean) prototypes.add((DisposableBean) bean);
        return bean;
    }
    
    @PreDestroy
    public void cleanup() {
        prototypes.forEach(DisposableBean::destroy);
    }
}
```

**FAANG Discussion Point:**  
*"What's the 'prototype-in-singleton' problem?"* — When a singleton bean has a prototype dependency, DI happens 
*once* (at singleton construction), so you get the *same* prototype forever. Solutions:
1. **ObjectProvider:** `@Autowired ObjectProvider<MyPrototype> provider; provider.getObject()` to fetch fresh 
ones.
2. **@Lookup:** Abstract method that Spring overrides to call `getBean()`.
3. **Provider<T>:** JSR-330 `jakarta.inject.Provider<T>`.
4. **Scoped proxy:** `@Scope(value="prototype", proxyMode=ScopedProxyMode.TARGET_CLASS)`.

**Follow-up Q: When would you actually use prototype scope?**  
- **Stateful objects** that hold per-request data (though request scope is usually better).
- **Jobs/tasks** that should not be shared across invocations.
- **Heavy objects** you want to create on demand rather than at startup.

---

## 14. What is Component Scanning?

Component scanning is the process by which Spring **automatically discovers and registers** classes annotated with 
stereotype annotations (`@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`, 
`@Configuration`) within a base package.

**Default behavior of `@SpringBootApplication`:**
```java
@SpringBootApplication // = @ComponentScan + @EnableAutoConfiguration + @SpringBootConfiguration
public class MyApp { }
```
It scans the package of `MyApp` and all sub-packages recursively.

**Custom scanning:**
```java
@ComponentScan(
    basePackages = "com.example",
    includeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX, 
        pattern = ".*Service"
    ),
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, 
        classes = LegacyService.class
    )
)
```

**How it works internally:**
1. `ClassPathScanningCandidateComponentProvider` reads all class files in the base package.
2. For each class, it uses **ASM** (a bytecode library) to read annotations **without loading the class**.
3. Classes with matching annotations become `BeanDefinition`s.
4. They're registered with `DefaultListableBeanFactory`.

**FAANG Discussion Point:**  
*"Why is component scanning slow on startup, and how do you optimize it?"* — Scanning 10,000 classes in 
`com.example` (wide base package) is expensive. Optimizations:
1. Use **narrow base packages**: `@ComponentScan(basePackages = "com.example.app")` not `"com.example"`.
2. Use **`@SpringBootApplication(scanBasePackages = ...)`** to restrict.
3. Use **index files**: Spring 6+ supports `META-INF/spring.components` for indexed scanning (built at compile 
time via annotation processor).
4. Use **compile-time DI** (e.g., Micronaut, Quarkus) for faster startup.

**Follow-up Q: What's the difference between `@Component` and `@Service`?**  
Functionally identical — both produce a singleton bean. But `@Service` is **semantic** (expresses intent: "this is 
a business service"). Tools and AOP advice can target `@Service` specifically. Same for `@Repository` (adds 
exception translation for JPA/JDBC) and `@Controller` (registers with Spring MVC dispatcher).

---

## 15. @Component vs @Bean?

**`@Component` — class-level:**
```java
@Component
public class MyService {
    public String greet() { return "Hello"; }
}
```
Spring auto-discovers and registers it.

**`@Bean` — method-level (inside `@Configuration`):**
```java
@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
}
```
You write the factory method. Spring invokes it and registers the result.

| Aspect | `@Component` | `@Bean` |
|---|---|---|
| **Applied to** | Class | Method |
| **Discovery** | Auto (component scan) | Explicit (config class is scanned, method is called) |
| **Use case** | Your own classes | Third-party classes |
| **Control** | Limited to annotations | Full programmatic control |
| **Initialization logic** | Constructor only | Can run any code before return |
| **Naming** | `myService` (lowercase) | Method name (`restTemplate`) |

**When to use `@Bean`:**
- Third-party classes (`RestTemplate`, `ObjectMapper`, `DataSource`)
- Classes needing **complex construction** (e.g., builder patterns)
- Classes where you need to **conditionally register** (use `@ConditionalOn...`)
- Legacy code you can't annotate

**FAANG Discussion Point:**  
*"What is 'lite mode' for @Bean?"* — When you put `@Bean` in a `@Component` (not `@Configuration`), Spring uses 
**lite mode** — no CGLIB proxying. This means inter-`@Bean` method calls return **new instances** each time 
(defeating singleton behavior). Always use `@Configuration` for inter-bean dependencies.

**Follow-up Q: Can you have `@Bean` in a `@Component` class with a method that takes another bean?**  
Yes, but the method's bean dependency will be injected via method parameters, not via direct method calls. So 
`lite mode` doesn't break this case.

---

## 16. @Autowired vs @Inject?

**`@Autowired` (Spring-specific):**
```java
@Autowired(required = false) // optional
private MyService service;
```

**`@Inject` (JSR-330 standard):**
```java
@Inject // always optional by default
private MyService service;
```

| Aspect | `@Autowired` | `@Inject` |
|---|---|---|
| **Standard** | Spring | JSR-330 (Java standard) |
| **`required` attribute** | ✅ Yes (`required = false`) | ❌ No (always optional) |
| **Default behavior** | Required (throws if missing) | Optional (null if missing) |
| **Works outside Spring** | ❌ | ✅ (Guice, CDI, etc.) |
| **Order with `@Qualifier`** | ✅ Yes | ✅ Yes (with `@Named`) |
| **Spring-specific features** | ✅ (`required`, profile-aware) | ❌ |

**`@Resource` (JSR-250):** Resolves by **name first**, then type. Legacy choice, still seen in older Spring code.

**FAANG Discussion Point:**  
*"Which one would you choose for a new microservice?"* — `@Autowired` for Spring Boot apps (works with 
Spring-specific features like `@Qualifier` and the `required` flag). `@Inject` if you anticipate switching DI 
containers. In practice, **most teams use `@Autowired` exclusively** for Spring projects.

**Follow-up Q: What if both `@Autowired` and `@Inject` are on the same field?**  
`@Autowired` wins (Spring's processor runs first by default). Don't mix them — confusing.

---

## 17. @Primary vs @Qualifier?

When **multiple beans of the same type** exist, Spring needs a tie-breaker.

**`@Primary` — global default:**
```java
@Component @Primary
public class SmsNotification implements Notification { }

@Component
public class EmailNotification implements Notification { }

@Service
public class AlertService {
    @Autowired private Notification notif; // gets SmsNotification
}
```

**`@Qualifier` — per-injection override:**
```java
@Component
public class SmsNotification implements Notification { }

@Component
public class EmailNotification implements Notification { }

@Service
public class AlertService {
    @Autowired @Qualifier("emailNotification")
    private Notification notif; // explicitly gets EmailNotification
}
```

| Aspect | `@Primary` | `@Qualifier` |
|---|---|---|
| **Applied to** | Bean class | Injection point |
| **Granularity** | Global (all injections) | Per-injection |
| **Conflict resolution** | Default | Override |
| **Use case** | One implementation is "the default" | Specific injection needs specific bean |
| **Both together** | `@Qualifier` wins | ✅ |

**FAANG Discussion Point:**  
*"How do you handle multiple implementations in a clean way?"* — Combine with `@Profile` or 
`@ConditionalOnProperty`:
```java
@Component @Profile("dev")
public class MockEmailService implements EmailService { }

@Component @Profile("prod")
public class SesEmailService implements EmailService { }
```

**Follow-up Q: What is `@Qualifier`'s default fallback?**  
The bean name. So `@Qualifier("paymentService")` works even if there's no `@Qualifier` annotation on the bean, as 
long as the bean's name is `paymentService`. This is why Spring uses class name lowercase as default bean name.

---

## 18. What is BeanPostProcessor?

`BeanPostProcessor` (BPP) is one of Spring's **most important extension points**. It lets you **intercept every 
bean** in the container before and after initialization. It's the foundation of:
- `@Autowired` resolution (`AutowiredAnnotationBeanPostProcessor`)
- AOP proxy creation (`AnnotationAwareAspectJAutoProxyCreator`)
- `@Transactional` (`PersistenceExceptionTranslationPostProcessor`)
- `@Scheduled` annotation processing
- `@Async` annotation processing
- Custom annotations like `@Metric`, `@AuditLog`

```java
@Component
public class LoggingBPP implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String name) {
        System.out.println("Before init: " + name);
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if (bean instanceof PaymentService) {
            // wrap in proxy, decorate, etc.
        }
        return bean;
    }
}
```

**How it works internally:**
1. `AbstractApplicationContext.refresh()` calls `PostProcessorRegistrationDelegate.registerBeanPostProcessors()`
2. All BPPs are **instantiated very early** (before normal beans)
3. They're added to a special list
4. When each bean is created, the container iterates through the BPP list
5. `postProcessBeforeInitialization` runs after DI but before `@PostConstruct`
6. `postProcessAfterInitialization` runs after `@PostConstruct` — **AOP proxy is created here**

**FAANG Discussion Point:**  
*"Why is `BeanPostProcessor` such a powerful design?"* — It's the **Decorator pattern at the container level**. 
You can add cross-cutting concerns (logging, security, transactions) without modifying business code. This is also 
why Spring apps can sometimes have "magic" behavior — a BPP is doing it transparently.

**Follow-up Q: What's the order of BPP execution?**  
Spring uses `Ordered` / `@Order` to determine priority. BPPs with lower order run first. For AOP, the 
`AnnotationAwareAspectJAutoProxyCreator` must run after BPPs that modify the bean (so the proxy is created last).

---

## 19. How does @Transactional work internally?

`@Transactional` is **AOP-based**. Spring creates a **proxy** around your bean at startup, and the proxy 
intercepts method calls to manage the database transaction.

**Step-by-step flow:**

```java
@Service
public class AccountService {
    @Transactional
    public void transfer(Account from, Account to, BigDecimal amount) {
        from.debit(amount);
        to.credit(amount);
    }
}
```

1. **Startup:** Spring's `TransactionInterceptor` (a `MethodInterceptor`) is registered.
2. **Bean creation:** `AnnotationAwareAspectJAutoProxyCreator` (a BPP) sees `@Transactional` → creates a **CGLIB 
or JDK proxy** wrapping the `AccountService` instance.
3. **Method call:** `accountService.transfer(...)` → actually calls `proxy.transfer(...)`.
4. **Proxy intercepts:** `TransactionInterceptor.invoke()` runs:
   - Reflects on `@Transactional` → reads `propagation`, `isolation`, `rollbackFor`, etc.
   - Asks `PlatformTransactionManager` (e.g., `JpaTransactionManager`) for a new transaction.
   - The manager acquires a **Connection** from the `DataSource` and binds it to a `ThreadLocal` (via 
`DataSourceUtils`).
5. **Actual method runs:** `transfer()` executes. Any `JdbcTemplate` or JPA call uses the bound Connection (so 
they all share the same transaction).
6. **Return path:** 
   - No exception → `commit()` → Connection commits, released from `ThreadLocal`.
   - `RuntimeException`/`Error` → `rollback()` (default rollback rules).
   - Checked exception → commit (configurable via `rollbackFor`).
7. **Connection cleanup:** Always returned to the pool, even on exception.

**Critical gotcha — self-invocation:**
```java
@Service
public class AccountService {
    @Transactional
    public void transfer(...) { 
        this.logAudit(); // ❌ NO TRANSACTION! (bypasses proxy)
    }
    
    @Transactional
    public void logAudit() { ... }
}
```
The internal call `this.logAudit()` skips the proxy, so the inner `@Transactional` is **ignored**. Fixes:
- Inject self via `@Autowired private AccountService self;` and call `self.logAudit()`.
- Move the method to a **separate bean**.
- Use `AopContext.currentProxy()` (requires `exposeProxy=true`).

**FAANG Discussion Point:**  
*"Why is @Transactional implemented via proxy and not bytecode instrumentation?"* — Proxies are **less invasive** 
(no need for `-javaagent`). They work with regular classes. The downside is the self-invocation problem. 
Alternatives like AspectJ load-time weaving solve this but require more setup.

**Follow-up Q: How does Spring pick which `PlatformTransactionManager` to use?**  
By **type**. With Spring Boot + JPA, the auto-config provides a `JpaTransactionManager`. With JDBC only, it's 
`DataSourceTransactionManager`. With JTA, it's `JtaTransactionManager`. You can have multiple — use 
`@Transactional("orderTxManager")` to specify.

---

## 20. How does Spring resolve circular dependencies?

A circular dependency is when **A depends on B, and B depends on A** (directly or transitively). Spring can 
resolve some forms, but not all.

**The 3-Level Cache (in `DefaultSingletonBeanRegistry`):**

```java
// Conceptual
Map<String, Object> singletonObjects      // Level 1: fully initialized
Map<String, Object> earlySingletonObjects // Level 2: early references
Map<String, ObjectFactory<?>> singletonFactories  // Level 3: lambdas
```

**Resolution flow (A ↔ B example with setter injection):**

1. Container starts creating `A`.
2. Calls A's constructor → creates raw `A` instance.
3. Registers an `ObjectFactory` for A in **Level 3 cache** (the factory knows how to create A — typically `() -> 
createBean(A)`).
4. Starts populating A's properties → needs B.
5. Container starts creating `B`.
6. Calls B's constructor → creates raw `B`.
7. Registers B's factory in Level 3.
8. Starts populating B's properties → needs A.
9. Container checks Level 1 (A not there — not fully done).
10. Checks Level 2 (A not there — never had an early reference).
11. Checks Level 3 (A's factory is there!) → calls factory → gets raw A → puts raw A in **Level 2**.
12. Injects this early A reference into B.
13. B finishes initialization → moves from Level 3 to Level 1.
14. A receives fully-initialized B → A finishes → moves from Level 3 to Level 1 (overwriting the Level 2 entry).
15. Done. Both A and B are fully constructed.

**When circular dependencies CANNOT be resolved:**
- **Constructor injection** — you need a fully-constructed bean to pass to the constructor, but the bean can't be 
constructed without the dependency.
- **Prototype scope** — Spring doesn't cache prototypes, so the 3-level cache trick doesn't work.
- **@Async / @Transactional proxies** — since Spring 6, self-references through proxies are blocked by default to 
prevent subtle bugs.

**Workarounds:**
1. **`@Lazy`:**
   ```java
   @Service
   public class A {
       private final B b;
       public A(@Lazy B b) { this.b = b; } // proxy injected, not real B
   }
   ```
2. **Setter injection** (with `setX()` methods instead of constructor).
3. **Refactor** — extract shared code into a third bean C, have A→C and B→C.
4. **ApplicationContext.getBean()** lookup inside the method (avoid).

**FAANG Discussion Point:**  
*"Why did Spring 6 disable setter-based circular dependency resolution by default?"* — Because **circular 
dependencies are almost always a design smell**. They indicate tight coupling. Spring 6's 
`spring.main.allow-circular-references=false` (default) forces you to fix the design. The 3-level cache was kept 
as an internal optimization for proxy creation, not cycle resolution.

**Follow-up Q: Can you detect circular dependencies at compile time?**  
Yes, with **arch-unit** library:
```java
@ArchTest
static final ArchRule noCycles = slices()
    .matching("com.example.(*)..")
    .should().beFreeOfCycles();
```

---

## 🏛️ Architecture Diagram (Big Picture)

```
┌──────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                     │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  @SpringBootApplication → @ComponentScan              │  │
│  │         ↓                                             │  │
│  │  Scans packages → builds BeanDefinitions              │  │
│  │         ↓                                             │  │
│  │  BeanFactoryPostProcessors (modify metadata)          │  │
│  │         ↓                                             │  │
│  │  Bean instantiation (singletons eager, prototypes lazy)│ │
│  │         ↓                                             │  │
│  │  BeanPostProcessors (Autowired, AOP, @Transactional)  │  │
│  │         ↓                                             │  │
│  │  @PostConstruct / InitializingBean / init-method      │  │
│  │         ↓                                             │  │
│  │  Bean is ready (cached in 3-level singleton registry) │  │
│  │         ↓                                             │  │
│  │  Application runs...                                  │  │
│  │         ↓                                             │  │
│  │  Shutdown → @PreDestroy / DisposableBean / destroy    │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## 🔥 @Transactional Flow Diagram

```
   Client: accountService.transfer(...)
                │
                ▼
   ┌──────────────────────────┐
   │   CGLIB/JDK Proxy        │  ◀── Created by BPP at startup
   │   (wraps AccountService) │
   └────────────┬─────────────┘
                ▼
   ┌──────────────────────────┐
   │  TransactionInterceptor  │  (reads @Transactional attrs)
   └────────────┬─────────────┘
                ▼
   ┌──────────────────────────┐
   │ PlatformTransactionManager │  (e.g., JpaTransactionManager)
   │ → getTransaction()        │
   │ → DataSource.getConnection()│
   │ → Connection bound to ThreadLocal│
   └────────────┬─────────────┘
                ▼
   ┌──────────────────────────┐
   │  transfer() body runs    │  (uses bound Connection for JPA/JDBC)
   └────────────┬─────────────┘
                ▼
       ┌────────┴────────┐
       │                 │
   no exception    RuntimeException
       │                 │
       ▼                 ▼
    commit()         rollback()
       │                 │
       └────────┬────────┘
                ▼
   ┌──────────────────────────┐
   │  Cleanup: unbind ThreadLocal│
   │  Return connection to pool │
   └──────────────────────────┘
```

---

## 🎯 Top 15 Follow-up Questions to Prepare

1. **Q:** What's the execution order of `@PostConstruct`, `InitializingBean`, and custom `init-method`?  
   **A:** `@PostConstruct` → `afterPropertiesSet()` → `init-method`. They're called in this order (PostConstruct 
is JSR-250, runs first).

2. **Q:** How do you make a Spring singleton thread-safe?  
   **A:** Make it stateless (no mutable fields), use `ThreadLocal`, or use `ConcurrentHashMap` with proper 
synchronization.

3. **Q:** Can you have a 3-way circular dependency (A→B→C→A)?  
   **A:** Yes, as long as at least one link is setter/field injection. Spring's 3-level cache handles cycles of 
any depth.

4. **Q:** Why does `@Transactional` not work on private methods?  
   **A:** Proxies intercept **public** method calls. Private methods are called via `this.method()` from inside 
the class, bypassing the proxy entirely.

5. **Q:** What's the difference between `BeanFactoryPostProcessor` and `BeanPostProcessor`?  
   **A:** `BeanFactoryPostProcessor` modifies `BeanDefinition` metadata **before** beans are created (e.g., 
`${property}` resolution). `BeanPostProcessor` modifies bean **instances** before/after initialization (AOP, 
`@Autowired`).

6. **Q:** How do you lazy-init a bean?  
   **A:** `@Lazy` on the class, on the injection point, or in XML `lazy-init="true"`. The bean is created on first 
use.

7. **Q:** Why does `@Transactional` not work in the same class self-invocation?  
   **A:** `this.method()` is a direct call, not going through the proxy. The proxy never intercepts. Fix: inject 
self, use a separate bean, or `AopContext.currentProxy()`.

8. **Q:** Can a `@Configuration` class be a bean itself?  
   **A:** Yes! It's a CGLIB-proxied bean where inter-`@Bean` method calls return the singleton (not a new 
instance). This is the difference between full mode (`@Configuration`) and lite mode (`@Component`).

9. **Q:** What's a `FactoryBean`?  
   **A:** A special bean whose **getObject()** returns the *actual* bean Spring registers. Used for complex 
initialization (e.g., `SqlSessionFactoryBean` in MyBatis). To inject the *FactoryBean* itself, use `&` prefix: 
`@Autowired @Qualifier("&myFactoryBean")`.

10. **Q:** How does Spring Boot's auto-configuration work?  
    **A:** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` lists config 
classes. Each uses `@Conditional*` to decide whether to apply. They produce `BeanDefinition`s that integrate with 
your app.

11. **Q:** What's `@ConditionalOnMissingBean`?  
    **A:** Registers a bean **only if** no other bean of that type exists. Lets users override defaults. Used 
heavily in Spring Boot's auto-config.

12. **Q:** How do you create a custom scope?  
    **A:** Implement `Scope` interface (`get()`, `remove()`, `registerDestructionCallback()`, etc.) and register 
via `ConfigurableBeanFactory.registerScope("myScope", new MyScope())`. Use with `@Scope("myScope")`.

13. **Q:** What's `BeanPostProcessor` vs `BeanFactoryPostProcessor` execution order?  
    **A:** All `BeanFactoryPostProcessors` run first (before any bean is created). Then `BeanPostProcessors` run 
for each bean during creation. Within BPPs, order is controlled by `@Order` or `Ordered` interface.

14. **Q:** Why are Spring's singleton beans not thread-safe?  
    **A:** Spring guarantees one instance per container, not concurrent access safety. Stateful singletons need 
explicit synchronization.

15. **Q:** What's the difference between `singletonObjects` and `earlySingletonObjects`?  
    **A:** `singletonObjects` holds **fully initialized** beans (Level 1). `earlySingletonObjects` holds **raw, 
uninitialized** references exposed for circular dep resolution (Level 2). Once a bean is fully done, it moves to 
Level 1.

---