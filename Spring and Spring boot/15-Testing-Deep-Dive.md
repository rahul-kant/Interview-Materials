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

## TestContainers - Advanced Usage

### Why TestContainers?

**Problem with Mocks**:
```java
// Testing with H2 (in-memory DB)
@DataJpaTest  // Uses H2 by default
class UserRepositoryTest {
    // Tests pass with H2, but fail in production with PostgreSQL!
}
```

**Solution: TestContainers**:
```java
// Test with real PostgreSQL
@DataJpaTest
@Testcontainers
class UserRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    // Tests run against actual PostgreSQL - catches SQL dialect issues!
}
```

### Complete TestContainers Setup

#### 1. Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>redis</artifactId>
    <scope>test</scope>
</dependency>
</dependencies>
```

#### 2. Database Testing with TestContainers

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldSaveAndRetrieveUser() {
        User user = new User("john@example.com", "John Doe");
        User saved = userRepository.save(user);
        
        Optional<User> found = userRepository.findById(saved.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }
    
    @Test
    void shouldTestComplexQuery() {
        // This tests actual PostgreSQL-specific features
        List<User> users = userRepository.findUsersWithJsonQuery();
        assertThat(users).isNotEmpty();
    }
}
```

#### 3. Reusable Containers (Faster Tests)

**Problem**: Starting containers is slow

**Solution**: Reuse containers across tests

```java
@Testcontainers
public abstract class AbstractIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);  // Reuse container across test runs
    
    static {
        postgres.start();
    }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}

// Inherit in your tests
@SpringBootTest
class UserServiceTest extends AbstractIntegrationTest {
    // Tests run with reused container - much faster!
}
```

**Enable reuse in `testcontainers.properties`**:
```properties
# src/test/resources/testcontainers.properties
testcontainers.reuse.enable=true
```

#### 4. Multiple Containers (Docker Compose)

```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        
        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Test
    void shouldTestFullFlow() {
        // Test with real PostgreSQL, Kafka, and Redis!
    }
}
```

#### 5. Custom Container Images

```java
@Container
static GenericContainer<?> myService = new GenericContainer<>(
    DockerImageName.parse("my-registry/my-service:latest")
)
    .withExposedPorts(8080)
    .withEnv("SPRING_PROFILES_ACTIVE", "test")
    .withCommand("--spring.datasource.url=jdbc:postgresql://postgres:5432/testdb")
    .waitingFor(Wait.forHttp("/actuator/health")
        .forStatusCode(200)
        .withStartupTimeout(Duration.ofMinutes(2)));

@Test
void shouldCallExternalService() {
    String baseUrl = "http://" + myService.getHost() + ":" 
        + myService.getFirstMappedPort();
    
    RestTemplate restTemplate = new RestTemplate();
    String response = restTemplate.getForObject(baseUrl + "/api/data", String.class);
    
    assertThat(response).isNotNull();
}
```

#### 6. Network Configuration (Service Communication)

```java
@Testcontainers
class MicroserviceIntegrationTest {
    
    static Network network = Network.newNetwork();
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withNetwork(network)
        .withNetworkAliases("postgres");
    
    @Container
    static GenericContainer<?> userService = new GenericContainer<>("user-service:latest")
        .withNetwork(network)
        .withNetworkAliases("user-service")
        .withExposedPorts(8080)
        .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/testdb");
    
    @Container
    static GenericContainer<?> orderService = new GenericContainer<>("order-service:latest")
        .withNetwork(network)
        .withNetworkAliases("order-service")
        .withExposedPorts(8080)
        .withEnv("USER_SERVICE_URL", "http://user-service:8080");
    
    @Test
    void shouldTestServiceCommunication() {
        // Services can communicate via network aliases
    }
}
```

#### 7. MongoDB Testing

```java
@SpringBootTest
@Testcontainers
class ProductRepositoryTest {
    
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:6.0")
        .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    }
    
    @Autowired
    private ProductRepository productRepository;
    
    @Test
    void shouldSaveProduct() {
        Product product = new Product("Laptop", 999.99);
        Product saved = productRepository.save(product);
        
        assertThat(saved.getId()).isNotNull();
    }
}
```

#### 8. Kafka Testing

```java
@SpringBootTest
@Testcontainers
class OrderEventProducerTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );
    
    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    @Autowired
    private KafkaConsumer<String, OrderEvent> consumer;
    
    @Test
    void shouldProduceAndConsumeMessage() throws InterruptedException {
        OrderEvent event = new OrderEvent("order-123", "CREATED");
        
        kafkaTemplate.send("order-events", event);
        
        // Wait for message consumption
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> consumer.getReceivedMessages().size() == 1);
        
        assertThat(consumer.getReceivedMessages())
            .hasSize(1)
            .first()
            .extracting(OrderEvent::getOrderId)
            .isEqualTo("order-123");
    }
}
```

#### 9. Redis Testing

```java
@SpringBootTest
@Testcontainers
class CacheServiceTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .withCommand("redis-server --requirepass secret");
    
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "secret");
    }
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Test
    void shouldCacheData() {
        redisTemplate.opsForValue().set("key1", "value1");
        
        String value = (String) redisTemplate.opsForValue().get("key1");
        
        assertThat(value).isEqualTo("value1");
    }
}
```

#### 10. Docker Compose with TestContainers

```yaml
# docker-compose-test.yml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: testdb
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "5432:5432"
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    ports:
      - "9092:9092"
```

```java
@SpringBootTest
@Testcontainers
class DockerComposeTest {
    
    @Container
    static ComposeContainer compose = new ComposeContainer(
        new File("docker-compose-test.yml")
    )
        .withExposedService("postgres", 5432)
        .withExposedService("redis", 6379)
        .withExposedService("kafka", 9092);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> 
            "jdbc:postgresql://" + compose.getServiceHost("postgres", 5432) + 
            ":" + compose.getServicePort("postgres", 5432) + "/testdb"
        );
        // Configure other services...
    }
}
```

#### 11. Performance Testing with TestContainers

```java
@SpringBootTest
@Testcontainers
class PerformanceTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withCommand(
            "postgres",
            "-c", "max_connections=500",
            "-c", "shared_buffers=256MB"
        );
    
    @Test
    void shouldHandleHighLoad() throws Exception {
        int threads = 100;
        int requestsPerThread = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads * requestsPerThread);
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        userRepository.save(new User("user" + j, "test"));
                        latch.countDown();
                    } catch (Exception e) {
                        log.error("Error", e);
                    }
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        stopWatch.stop();
        
        log.info("Processed {} requests in {}ms", 
            threads * requestsPerThread, 
            stopWatch.getTotalTimeMillis());
        
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(10000);
    }
}
```

### TestContainers Best Practices

✅ **DO**:
1. Use `@DynamicPropertySource` for dynamic configuration
2. Reuse containers across tests with `.withReuse(true)`
3. Use specific image versions (not `latest`)
4. Configure proper wait strategies
5. Use networks for inter-container communication
6. Clean up containers after tests

❌ **DON'T**:
1. Start containers in `@BeforeEach` (too slow)
2. Use random ports without proper configuration
3. Forget to set proper timeouts
4. Mix TestContainers with H2 in same test
5. Ignore container logs when tests fail

### Interview Tips

**Q: Why use TestContainers instead of H2?**
> "H2 is convenient but has different SQL dialect than production databases. TestContainers runs actual PostgreSQL/MySQL, catching database-specific issues. For example, PostgreSQL's JSON operations, MySQL's full-text search, or specific locking behaviors won't work in H2. The trade-off is slower test execution, but we mitigate this with container reuse."

**Q: How do you keep TestContainers tests fast?**
> "Three strategies: 1) Reuse containers with `.withReuse(true)` and `testcontainers.reuse.enable=true`, 2) Use singleton containers shared across test classes, 3) Run integration tests separately from unit tests in CI/CD pipeline. This keeps fast feedback loop for unit tests while ensuring thorough integration testing."

---

## Performance Testing with JMH (Java Microbenchmark Harness)

### Why JMH?

**Problem with Simple Timing**:
```java
@Test
void badPerformanceTest() {
    long start = System.nanoTime();
    someMethod();
    long end = System.nanoTime();
    System.out.println("Took: " + (end - start) + "ns");  // UNRELIABLE!
}
```

**Issues**:
- JVM warmup not accounted for
- GC can skew results
- No statistical analysis
- JIT compilation affects timing

**JMH Solution**: Proper warmup, statistical analysis, JIT handling

### Complete JMH Setup

#### 1. Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.openjdk.jmh</groupId>
        <artifactId>jmh-core</artifactId>
        <version>1.37</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.openjdk.jmh</groupId>
        <artifactId>jmh-generator-annprocess</artifactId>
        <version>1.37</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <finalName>benchmarks</finalName>
                        <transformers>
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainClass>org.openjdk.jmh.Main</mainClass>
                            </transformer>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

#### 2. Basic JMH Benchmark

```java
@BenchmarkMode(Mode.AverageTime)  // Measure average execution time
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)  // Shared state across all benchmark threads
@Fork(value = 2, warmups = 1)  // 2 forks, 1 warmup fork
@Warmup(iterations = 3, time = 1)  // 3 warmup iterations, 1 second each
@Measurement(iterations = 5, time = 1)  // 5 measurement iterations
public class StringConcatenationBenchmark {
    
    private static final int ITERATIONS = 1000;
    
    @Benchmark
    public String stringConcatenation() {
        String result = "";
        for (int i = 0; i < ITERATIONS; i++) {
            result += "item" + i;
        }
        return result;
    }
    
    @Benchmark
    public String stringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ITERATIONS; i++) {
            sb.append("item").append(i);
        }
        return sb.toString();
    }
    
    @Benchmark
    public String stringJoiner() {
        StringJoiner joiner = new StringJoiner("");
        for (int i = 0; i < ITERATIONS; i++) {
            joiner.add("item" + i);
        }
        return joiner.toString();
    }
    
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(StringConcatenationBenchmark.class.getSimpleName())
            .build();
        
        new Runner(opt).run();
    }
}
```

#### 3. Spring Boot Integration Benchmark

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 3, time = 10)
public class RepositoryBenchmark {
    
    private ApplicationContext context;
    private UserRepository userRepository;
    
    @Setup(Level.Trial)  // Run once before all iterations
    public void setup() {
        context = new SpringApplicationBuilder(Application.class)
            .web(WebApplicationType.NONE)
            .run();
        
        userRepository = context.getBean(UserRepository.class);
        
        // Seed test data
        for (int i = 0; i < 1000; i++) {
            userRepository.save(new User("user" + i, "test@example.com"));
        }
    }
    
    @TearDown(Level.Trial)  // Run once after all iterations
    public void tearDown() {
        SpringApplication.exit(context);
    }
    
    @Benchmark
    public List<User> benchmarkFindAll() {
        return userRepository.findAll();
    }
    
    @Benchmark
    public List<User> benchmarkFindAllWithProjection() {
        return userRepository.findAllProjectedBy();
    }
    
    @Benchmark
    public User benchmarkFindById() {
        return userRepository.findById(1L).orElse(null);
    }
}
```

#### 4. Cache Performance Benchmark

```java
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class CacheBenchmark {
    
    private CacheManager caffeineCacheManager;
    private CacheManager redisCacheManager;
    private Cache caffeineCache;
    private Cache redisCache;
    
    @Setup
    public void setup() {
        // Setup Caffeine
        CaffeineCacheManager caffeine = new CaffeineCacheManager();
        caffeine.setCaffeine(Caffeine.newBuilder().maximumSize(1000));
        this.caffeineCacheManager = caffeine;
        this.caffeineCache = caffeineCacheManager.getCache("test");
        
        // Setup Redis
        RedisTemplate<String, Object> redisTemplate = createRedisTemplate();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        this.redisCacheManager = RedisCacheManager.builder(
            redisTemplate.getConnectionFactory()
        ).cacheDefaults(config).build();
        this.redisCache = redisCacheManager.getCache("test");
        
        // Pre-populate
        for (int i = 0; i < 100; i++) {
            caffeineCache.put("key" + i, "value" + i);
            redisCache.put("key" + i, "value" + i);
        }
    }
    
    @Benchmark
    public Object caffeineCacheGet() {
        return caffeineCache.get("key50", String.class);
    }
    
    @Benchmark
    public Object redisCacheGet() {
        return redisCache.get("key50", String.class);
    }
    
    @Benchmark
    public void caffeineCachePut() {
        caffeineCache.put("newKey", "newValue");
    }
    
    @Benchmark
    public void redisCachePut() {
        redisCache.put("newKey", "newValue");
    }
}
```

#### 5. JSON Serialization Benchmark

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class SerializationBenchmark {
    
    private User user;
    private ObjectMapper objectMapper;
    private Gson gson;
    
    @Setup
    public void setup() {
        user = new User(1L, "John Doe", "john@example.com");
        objectMapper = new ObjectMapper();
        gson = new Gson();
    }
    
    @Benchmark
    public String jacksonSerialize() throws Exception {
        return objectMapper.writeValueAsString(user);
    }
    
    @Benchmark
    public String gsonSerialize() {
        return gson.toJson(user);
    }
    
    @Benchmark
    public User jacksonDeserialize() throws Exception {
        String json = objectMapper.writeValueAsString(user);
        return objectMapper.readValue(json, User.class);
    }
    
    @Benchmark
    public User gsonDeserialize() {
        String json = gson.toJson(user);
        return gson.fromJson(json, User.class);
    }
}
```

#### 6. Running Benchmarks

```bash
# Build benchmark JAR
mvn clean package

# Run all benchmarks
java -jar target/benchmarks.jar

# Run specific benchmark
java -jar target/benchmarks.jar ".*CacheBenchmark.*"

# With profilers
java -jar target/benchmarks.jar -prof gc  # GC profiler
java -jar target/benchmarks.jar -prof stack  # Stack profiler
java -jar target/benchmarks.jar -prof async  # Async profiler

# Custom parameters
java -jar target/benchmarks.jar \
    -wi 5 -i 10 \  # 5 warmup, 10 measurement iterations
    -f 3 \          # 3 forks
    -t 4 \          # 4 threads
    -r 5 \          # 5 seconds per iteration
    -w 3            # 3 seconds warmup
```

#### 7. Results Analysis

**Example Output**:
```
Benchmark                              Mode  Cnt    Score    Error  Units
CacheBenchmark.caffeineCacheGet       thrpt   15  5234567.123 ± 12345.678  ops/s
CacheBenchmark.redisCacheGet          thrpt   15   123456.789 ±  1234.567  ops/s
CacheBenchmark.caffeineCachePut       thrpt   15  3456789.012 ± 23456.789  ops/s
CacheBenchmark.redisCachePut          thrpt   15    98765.432 ±   987.654  ops/s
```

**Analysis**:
- Caffeine is **42x faster** for reads
- Caffeine is **35x faster** for writes
- Use Caffeine for local cache, Redis for distributed

### JMH Annotations Reference

| Annotation | Purpose |
|------------|---------|
| `@Benchmark` | Marks method as benchmark |
| `@BenchmarkMode` | Mode.Throughput, AverageTime, SampleTime, SingleShotTime |
| `@OutputTimeUnit` | Time unit for results |
| `@State` | State scope: Benchmark, Thread, Group |
| `@Setup` | Run before benchmark (Level: Trial, Iteration, Invocation) |
| `@TearDown` | Run after benchmark |
| `@Param` | Parameterized benchmarks |
| `@Fork` | JVM forks and warmup forks |
| `@Warmup` | Warmup iterations |
| `@Measurement` | Measurement iterations |
| `@Threads` | Number of threads |

### Best Practices

✅ **DO**:
1. Always warmup (3-5 iterations)
2. Run multiple forks (2-3)
3. Use appropriate benchmark modes
4. Avoid Dead Code Elimination (return results)
5. Use `@State` for shared state
6. Profile with `-prof gc` to check GC impact

❌ **DON'T**:
1. Trust single-run results
2. Benchmark in IDE (use built JAR)
3. Run benchmarks on busy system
4. Ignore JVM warmup phase
5. Benchmark trivial operations
6. Compare results across different machines

### Interview Tips

**Q: How do you measure performance in Spring Boot?**
> "For micro-benchmarks, I use JMH with proper warmup and multiple forks to account for JIT compilation and GC. For macro-benchmarks, I use load testing tools like Gatling or JMeter. JMH is critical for comparing algorithm implementations or library choices, while load tests validate system behavior under production-like conditions."

**Q: What's the difference between JMH and simple timing?**
> "Simple timing with System.nanoTime() doesn't account for JVM warmup, JIT compilation, or GC pauses. JMH handles all of this automatically with warmup iterations, multiple forks, and statistical analysis. For example, comparing cache libraries, JMH showed Caffeine is 40x faster than Redis for local operations, which simple timing wouldn't reliably show due to variance."

---

## Mocking Strategies & Best Practices
- **MockMvc**: For testing controllers without a full server.
- **@MockBean**: Replaces a bean in the Spring Context with a Mockito mock.
- **SpyBean**: Wraps a real bean, allowing you to mock specific methods while keeping others real.
- **Avoid Over-Mocking**: If you mock everything, you're not testing the integration. Use **TestContainers** for real DB/Messaging tests.
