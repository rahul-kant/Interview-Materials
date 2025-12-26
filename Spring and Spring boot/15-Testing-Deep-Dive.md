# Advanced Testing Strategies in Spring Boot

## Table of Contents
1. [The Testing Pyramid](#the-testing-pyramid)
2. [Consumer-Driven Contract Testing (Pact / Spring Cloud Contract)](#consumer-driven-contract-testing-pact--spring-cloud-contract)
3. [Mutation Testing (PITest)](#mutation-testing-pitest)
4. [Performance Testing (Gatling / JMeter)](#performance-testing-gatling--jmeter)
5. [Architecture Testing (ArchUnit)](#architecture-testing-archunit)
6. [Mocking Strategies & Best Practices](#mocking-strategies--best-practices)

---

## The Testing Pyramid

## 🧪 Testing Pyramid

> [!TIP]
> **Interview Pro-Tip: "How do you test asynchronous code in Spring?"**
> Use **Awaitility**. It allows you to "wait" for an asynchronous condition to become true without using `Thread.sleep()`.
> ```java
> await().atMost(5, SECONDS).until(() -> emailService.getCount() == 1);
> ```

### 🔍 Deep Dive: @SpyBean vs @MockBean
- **@MockBean**: Replaces the real bean with a complete mock. All methods return null/default unless stubbed.
- **@SpyBean**: Wraps the *real* bean. Methods call the real implementation unless you explicitly stub them. **Use @SpyBean when you want to test the real logic but monitor or stub one specific method.**

### 🛠️ Complex Example: Reactive Testing with StepVerifier
```java
@Test
void testReactiveFlow() {
    Flux<String> flux = Flux.just("A", "B", "C");
    
    StepVerifier.create(flux)
        .expectNext("A")
        .expectNext("B")
        .expectNext("C")
        .verifyComplete();
}
```

## The Testing Pyramid
A healthy test suite follows the pyramid:
- **Unit Tests** (Many): Fast, test logic in isolation.
- **Integration Tests** (Some): Test interaction between components (e.g., `@DataJpaTest`).
- **End-to-End Tests** (Few): Test the whole system flow.

---

## Consumer-Driven Contract Testing (CDCT)
Solves the problem of "breaking changes" in microservices.

### Spring Cloud Contract
1. **Producer** defines a contract (Groovy/YAML).
2. **Spring Cloud Contract** generates tests for the producer and a **Stub JAR**.
3. **Consumer** uses the Stub JAR to run tests against a mock producer.

**Example Contract**:
```groovy
Contract.make {
    request {
        method 'GET'
        url '/users/1'
    }
    response {
        status 200
        body([
            id: 1,
            name: 'John Doe'
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
```

---

## Mutation Testing (PITest)
Unit test coverage (line/branch) can be misleading. Mutation testing checks the **quality** of your tests.

### How it Works
1. PIT introduces "mutants" (small bugs) into your code (e.g., changing `>` to `>=`).
2. It runs your tests.
3. If tests **fail**, the mutant is **killed** (Good).
4. If tests **pass**, the mutant **survived** (Bad - your tests didn't catch the bug).

---

## Performance Testing (Gatling)
Gatling uses a Scala/Java DSL to define load tests.

### Example Gatling Test (Java)
```java
public class BasicSimulation extends Simulation {
    HttpProtocolBuilder httpProtocol = http.baseUrl("http://localhost:8080");

    ScenarioBuilder scn = scenario("Basic Scenario")
        .exec(http("request_1").get("/api/users"));

    {
        setUp(scn.injectOpen(atOnceUsers(100)).protocols(httpProtocol));
    }
}
```

---

## Architecture Testing (ArchUnit)
Ensures your code follows architectural rules (e.g., "Controllers should not call Repositories directly").

### Example
```java
@Test
void servicesShouldOnlyBeCalledByControllers() {
    classes().that().resideInAPackage("..service..")
        .should().onlyBeAccessed().byAnyPackage("..controller..", "..service..");
}
```

---

## Mocking Strategies & Best Practices
- **MockMvc**: For testing controllers without a full server.
- **@MockBean**: Replaces a bean in the Spring Context with a Mockito mock.
- **SpyBean**: Wraps a real bean, allowing you to mock specific methods while keeping others real.
- **Avoid Over-Mocking**: If you mock everything, you're not testing the integration. Use **TestContainers** for real DB/Messaging tests.
