# OpenTelemetry Jaeger Spring Boot Starter

A Spring Boot starter library for automatic distributed tracing using OpenTelemetry and Jaeger.

## Features

- **Automatic RestTemplate tracing** (via ClientHttpRequestInterceptor)
- **Automatic WebClient tracing** (via ExchangeFilterFunction)
- **OTel Java agent auto-attach** - instruments library-created HTTP clients (e.g., from `WebClient.builder()`) without any CLI flags
- Request/response body capture (configurable)
- Request/response header capture (configurable)
- Context propagation across service calls
- Auto-configured `RestTemplate` and `WebClient` with tracing
- **Fat JAR** with all dependencies included (includes embedded OTel Java agent)

## Requirements

- Java 8+
- Spring Boot 2.0.x - 2.7.x
- Jaeger (via Docker or external)

---

## Quick Start (Fat JAR)

### Step 1: Download the Fat JAR

Clone and build the JAR, or download from releases:

```bash
git clone https://github.com/Sophoun/oteljaeger.git
cd oteljaeger
./gradlew :oteljaeger-spring-boot-starter:shadowJar
```

Output: `oteljaeger-spring-boot-starter/build/libs/oteljaeger-spring-boot-starter-0.0.1-SNAPSHOT.jar` (~37 MB)

---

### Step 2: Add to Your Spring Boot Project

#### Option A: Using Gradle

1. Create `libs/` folder in your project root:
```bash
mkdir -p libs
cp /path/to/oteljaeger-spring-boot-starter-0.0.1-SNAPSHOT.jar libs/
```

2. Update `build.gradle.kts`:
```kotlin
plugins {
    id("org.springframework.boot") version "2.6.0"
    id("io.spring.dependency-management")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    // Add this if you use WebClient
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Your other dependencies...
}
```

#### Option B: Using Maven

1. Create `libs/` folder in your project root:
```bash
mkdir -p libs
cp /path/to/oteljaeger-spring-boot-starter-0.0.1-SNAPSHOT.jar libs/
```

2. Update `pom.xml`:
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <java.version>1.8</java.version>
        <spring-boot.version>2.6.0</spring-boot.version>
    </properties>
    
    <dependencies>
        <!-- Add this dependency -->
        <dependency>
            <groupId>com.sophoun</groupId>
            <artifactId>oteljaeger-spring-boot-starter</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/oteljaeger-spring-boot-starter-0.0.1-SNAPSHOT.jar</systemPath>
        </dependency>
        
        <!-- Spring Boot dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
        
        <!-- Add this if you use WebClient -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
        
        <!-- Your other dependencies... -->
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### Step 3: Enable Tracing in Your Application

The starter auto-configures itself via `spring.factories`. Just add the dependency and
configure `application.yml` — no annotation needed.

```java
package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

---

### Step 4: Create REST Controllers

#### Using RestTemplate (auto-traced)

```java
package com.example.myapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class UserController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/api/user")
    public String getUser() {
        // This call will be automatically traced
        String response = restTemplate.getForObject(
            "https://jsonplaceholder.typicode.com/users/1", 
            String.class
        );
        return response;
    }
}
```

#### Using WebClient (auto-traced)

```java
package com.example.myapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class UserController {

    private final WebClient webClient;

    // Inject the auto-configured WebClient.Builder (includes tracing filter)
    public UserController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build();
    }

    @GetMapping("/api/user-webclient")
    public String getUserWebClient() {
        // This call will be automatically traced
        return webClient.get()
            .uri("/users/{id}", 1)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
}
```

> **Note:** The starter automatically traces both `RestTemplate` and `WebClient`. The OTel Java agent
> also instruments library-created `WebClient` instances (e.g., from `WebClient.builder()`) without
> requiring any additional configuration.

---

### Step 5: Configure Application

Create `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: my-app

server:
  port: 8080

# OpenTelemetry + Jaeger tracing
oteljaeger:
  enabled: true
  service-name: my-app
  exporter-endpoint: http://localhost:4318/v1/traces
```

---

### Step 6: Start Jaeger

Create `docker-compose.yml` in your project root:

```yaml
services:
  jaeger:
    image: jaegertracing/all-in-one:1.53
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    ports:
      - "16686:16686"  # Jaeger UI
      - "4317:4317"    # OTLP gRPC
      - "4318:4318"    # OTLP HTTP
```

Start Jaeger:

```bash
docker-compose up -d
```

---

### Step 7: Run and Test

1. Start your Spring Boot application:
```bash
# Gradle
./gradlew bootRun

# Maven
mvn spring-boot:run
```

2. Test your endpoints:
```bash
curl http://localhost:8080/api/user
curl http://localhost:8080/api/user-webclient
```

3. View traces in Jaeger UI:
   - Open http://localhost:16686
   - Select your service name from the dropdown
   - Click "Find Traces"
   - Click on a trace to see the details

---

### What You'll See in Jaeger

```
GET /api/user (1234ms)
  HTTP GET /users/1 (456ms)
    - http.method: GET
    - http.url: https://jsonplaceholder.typicode.com/users/1
    - http.status_code: 200
    - http.request.body: (if POST/PUT)
    - http.response.body: {"id": 1, "name": "Leanne Graham", ...}
```

---

## OTel Java Agent Auto-Attach

The starter automatically attaches the [OTel Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
at application startup via `RuntimeAttachRunListener`. This provides:

- **Automatic instrumentation of library-created HTTP clients** - any `WebClient` or `RestTemplate`
  created outside of Spring (e.g., via `WebClient.builder()` in a library) will be traced
- **Zero CLI flags required** - no need for `-javaagent` JVM argument
- **Context propagation** across all instrumented HTTP calls

### How It Works

1. `RuntimeAttachRunListener.starting()` runs first — sets the `otel.javaagent.testing.runtime-attach.main-method-check` bypass property
2. `RuntimeAttachRunListener.environmentPrepared()` runs after `application.yml` is loaded — sets base agent flags (Tomcat disabled, Netty enabled, etc.), reads all `oteljaeger.*` properties, maps them to `otel.*` system properties, and attaches the agent
3. When the agent is active, manual `WebClientFilter`, `webClientFilterInjector`, and `WeConnectorWebClientInjector` beans are disabled to avoid duplicate spans

### Overriding Agent Configuration

All agent settings can now be configured via `application.yml` (see [Configuration Reference](#configuration-reference)).
You can also override via system properties or environment variables:

```bash
# Via system properties (highest priority)
java -Dotel.service.name=my-app -jar my-app.jar

# Via CLI arguments
java --oteljaeger.service-name=my-app -jar my-app.jar

# Via environment variables
export OTEL_SERVICE_NAME=my-app
java -jar my-app.jar
```

### Disabling Auto-Attach

If the OTel agent fails to attach (e.g., due to class loading conflicts), it falls back to manual instrumentation automatically. The agent status is stored in system property `oteljaeger.agent.active` (`true`/`false`).

To force manual-only instrumentation, you can remove the `RuntimeAttachRunListener` registration from
`META-INF/spring.factories`.

---

## Alternative: Use from Maven Repository

### Local Maven

```bash
./gradlew :oteljaeger-spring-boot-starter:publishToMavenLocal
```

**Gradle:**
```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("com.sophoun:oteljaeger-spring-boot-starter:0.0.1-SNAPSHOT")
}
```

**Maven:**
```xml
<repositories>
    <repository>
        <id>local</id>
        <url>file://${user.home}/.m2/repository</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.sophoun</groupId>
        <artifactId>oteljaeger-spring-boot-starter</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Maven Central

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.sophoun:oteljaeger-spring-boot-starter:1.0.0")
}
```

---

## Complete Example Project

### build.gradle.kts (Gradle)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "2.6.0"
    id("io.spring.dependency-management")
}

group = "com.example"
version = "1.0.0"
sourceCompatibility = "1.8"

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

### MyApplication.java

```java
package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### UserController.java

```java
package com.example.myapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private RestTemplate restTemplate;

    private final WebClient webClient;

    public UserController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build();
    }

    @GetMapping("/user")
    public Map<String, Object> getUserRestTemplate() {
        return restTemplate.getForObject(
            "https://jsonplaceholder.typicode.com/users/1", 
            Map.class
        );
    }

    @GetMapping("/user-webclient/{id}")
    public Map<String, Object> getUserWebClient(@PathVariable int id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
}
```

### application.yml

```yaml
spring:
  application:
    name: my-traced-app

server:
  port: 8080

oteljaeger:
  service-name: my-traced-app
  exporter-endpoint: http://localhost:4318/v1/traces
```

---

## What Gets Traced

### Inbound Requests (Manual Filter)
- HTTP method, URL, headers
- Request body (POST/PUT/PATCH)
- Response body
- Status code
- Duration

### Outbound RestTemplate Calls (Manual Interceptor)
- HTTP method, URL, headers
- Request body
- Response body
- Status code
- Duration
- Parent-child relationship with inbound request

### Outbound WebClient Calls (Agent or Manual Filter)
- HTTP method, URL, headers
- Request body
- Response body
- Status code
- Duration
- Parent-child relationship with inbound request
- Works with both Spring-managed and library-created WebClient instances

### Automatic (Agent-Only Instrumentations)

When the OTel agent is active, it automatically instruments:

- **HTTP clients** — Reactor Netty (WebClient), HttpURLConnection (RestTemplate)
- **Databases** — JDBC, MyBatis, Oracle, MySQL, PostgreSQL
- **Messaging** — Kafka, Spring Kafka
- **Cloud** — AWS SDK (S3, SQS, etc.)
- **Frameworks** — Spring WebMVC (when enabled)

## Example Trace

```
GET /api/user (2340ms)
  HTTP GET /users/1 (325ms)
  HTTP GET /posts (107ms)
  HTTP GET /comments (68ms)
  HTTP GET /todos (195ms)
```

---

## Configuration Reference

All configuration is done via `application.yml` (or `application.properties`) under the `oteljaeger` prefix.
Each property is mapped to the corresponding `otel.*` system property for the OTel Java agent automatically.

```yaml
oteljaeger:
  enabled: true
  service-name: my-app
  exporter-endpoint: http://localhost:4318/v1/traces
  exporter-protocol: http/protobuf
  exporter-timeout-seconds: 10
  traces-exporter: otlp
  metrics-exporter: none
  logs-exporter: none
  instrumentation-tomcat-enabled: false
  instrumentation-servlet-enabled: false
  instrumentation-netty-enabled: true
  instrumentation-reactor-enabled: true
  capture-headers: true
  capture-bodies: true
  max-body-size: 65536
  trace-mybatis: true
  trace-external-api: true
  trace-aws-sdk: true
  trace-kafka: true
  trace-spring-kafka: true
```

### General

| Property | Default | Mapped To | Description |
|----------|---------|-----------|-------------|
| `oteljaeger.enabled` | `true` | — | Enable/disable all tracing. Disables agent attachment and all manual beans. |
| `oteljaeger.service-name` | `oteljaeger` | `otel.service.name` | Service name shown in Jaeger UI. |

### OTLP Exporter

| Property | Default | Mapped To | Description |
|----------|---------|-----------|-------------|
| `oteljaeger.exporter-endpoint` | `http://localhost:4318/v1/traces` | `otel.exporter.otlp.endpoint` (base URL) | OTLP HTTP endpoint. Path is stripped for the agent. |
| `oteljaeger.exporter-protocol` | `http/protobuf` | `otel.exporter.otlp.protocol` | OTLP protocol: `http/protobuf` or `grpc`. |
| `oteljaeger.exporter-timeout-seconds` | `10` | — (manual SDK only) | Exporter timeout in seconds. Only applies to manual SDK fallback. |
| `oteljaeger.traces-exporter` | `otlp` | `otel.traces.exporter` | Trace exporter: `otlp`, `none`, `jaeger`, etc. |
| `oteljaeger.metrics-exporter` | `none` | `otel.metrics.exporter` | Metrics exporter: `none`, `otlp`, `prometheus`, etc. |
| `oteljaeger.logs-exporter` | `none` | `otel.logs.exporter` | Logs exporter: `none`, `otlp`, etc. |

### Instrumentation

| Property | Default | Mapped To | Description |
|----------|---------|-----------|-------------|
| `oteljaeger.instrumentation-tomcat-enabled` | `false` | `otel.instrumentation.tomcat.enabled` | Enable Tomcat server instrumentation. Disable to avoid deadlocks with Spring Boot 2.6 + Reactor Netty. |
| `oteljaeger.instrumentation-servlet-enabled` | `false` | `otel.instrumentation.servlet.enabled` | Enable Servlet instrumentation. Disable together with Tomcat. |
| `oteljaeger.instrumentation-netty-enabled` | `true` | `otel.instrumentation.netty-4.1.enabled` | Enable Reactor Netty (WebClient) instrumentation. |
| `oteljaeger.instrumentation-reactor-enabled` | `true` | `otel.instrumentation.reactor.enabled` | Enable Project Reactor instrumentation. |

### Span Capture (Manual Filters Only)

These apply only to the manual `OpenTelemetryFilter`, `RestTemplateInterceptor`, and `WebClientFilter`.
They have no effect when the OTel agent is active (agent handles its own span attributes).

| Property | Default | Description |
|----------|---------|-------------|
| `oteljaeger.capture-headers` | `true` | Capture request/response headers as span attributes. |
| `oteljaeger.capture-bodies` | `true` | Capture request/response bodies as span attributes. |
| `oteljaeger.max-body-size` | `65536` | Maximum body size to capture in bytes. `-1` for unlimited. |

### Tracing Scope

| Property | Default | Mapped To | Description |
|----------|---------|-----------|-------------|
| `oteljaeger.trace-mybatis` | `true` | `otel.instrumentation.mybatis.enabled` | Enable MyBatis SQL query tracing. |
| `oteljaeger.trace-external-api` | `true` | — | Enable WeExterfaceServiceFactory external API call tracing. |
| `oteljaeger.trace-aws-sdk` | `true` | `otel.instrumentation.aws-sdk.enabled` | Enable AWS SDK (S3, etc.) tracing. |
| `oteljaeger.trace-kafka` | `true` | `otel.instrumentation.kafka.enabled` | Enable Kafka producer/consumer tracing. |
| `oteljaeger.trace-spring-kafka` | `true` | `otel.instrumentation.spring-kafka.enabled` | Enable Spring Kafka tracing. |

### Property Resolution Order

Properties are resolved in this priority order (highest first):

1. **JVM system properties** (`-Dotel.service.name=...`)
2. **CLI arguments** (`--oteljaeger.service-name=...`)
3. **Environment variables** (`OTEL_SERVICE_NAME=...`)
4. **application.yml** (`oteljaeger.service-name: ...`)
5. **Default value**

---

## Auto-Configured Beans

### Without OTel Agent (Manual Instrumentation)

| Bean | Description |
|------|-------------|
| `OpenTelemetry` | Custom OTEL SDK instance |
| `Tracer` | OTEL Tracer |
| `OpenTelemetryFilter` | Inbound request tracing |
| `RestTemplateInterceptor` | Outbound RestTemplate tracing |
| `RestTemplateBeanPostProcessor` | Auto-adds interceptor to RestTemplates |
| `RestTemplate` | Pre-configured with tracing interceptor |
| `WebClientFilter` | Outbound WebClient tracing |
| `WebClient.Builder` | Pre-configured with tracing filter |
| `WebClient` | Pre-configured with tracing filter |
| `webClientFilterInjector` | BeanPostProcessor — injects filter into all WebClient beans |
| `weExterfaceBeanPostProcessor` | BeanPostProcessor — instruments WeExterfaceServiceFactory |
| `weConnectorWebClientInjector` | Injects filter into WeConnector's internal WebClient |

### With OTel Agent (Agent Instrumentation)

When the OTel Java agent attaches successfully, it provides its own OpenTelemetry SDK
and instrumentations. The following manual beans are **skipped** to avoid conflicts:

- `WebClientFilter` — agent instruments Reactor Netty automatically
- `webClientFilterInjector` BeanPostProcessor — agent handles all WebClient instances
- `WeConnectorWebClientInjector` — agent instruments WeConnector's internal WebClient

The agent handles all tracing automatically, including library-created HTTP clients.

`OpenTelemetryFilter` (inbound) and `RestTemplateInterceptor` (outbound) remain active
even with the agent, since they provide additional span attributes (bodies, headers) that
the agent does not capture by default.

All beans use `@ConditionalOnMissingBean`, so you can override any of them.

---

## Custom RestTemplate / WebClient

If you need a custom `RestTemplate` or `WebClient`, define your own bean - it will automatically
get tracing:

```java
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
}

@Bean
public WebClient webClient(WebClient.Builder builder) {
    return builder
            .baseUrl("https://api.example.com")
            .build();
}
```

---

## Disabling Tracing

```properties
oteljaeger.enabled=false
```

Or exclude auto-configuration:

```java
@SpringBootApplication(exclude = {OtelJaegerAutoConfiguration.class})
```

---

## Platform Notes

### macOS / Spring Boot 2.6 + Reactor Netty

Tomcat and Servlet instrumentations are disabled by default to avoid deadlocks
caused by Spring Boot 2.6 + Reactor Netty class loading conflicts. Only HTTP client
instrumentations (Netty, Reactor) are enabled.

To enable server-side instrumentation, add to your `application.yml`:

```yaml
oteljaeger:
  instrumentation-tomcat-enabled: true
  instrumentation-servlet-enabled: true
```

Or pass as JVM argument:

```bash
java -Djava.net.preferIPv4Stack=true -jar my-app.jar
```

### Linux / Production

All instrumentations work correctly on Linux. To enable server-side tracing, set the
properties above in your `application.yml` or pass them as JVM arguments.

---

## Project Structure

```
oteljaeger/
├── build.gradle.kts                        # Root build
├── settings.gradle.kts                     # Multi-module config
├── docker-compose.yml                      # Jaeger setup
├── README.md                               # This file
├── oteljaeger-spring-boot-starter/         # Library (Fat JAR)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/.../starter/
│       │   ├── OtelJaegerProperties.java       # Configuration properties (all oteljaeger.* keys)
│       │   ├── OtelJaegerAutoConfiguration.java # Auto-configuration
│       │   ├── OtelConfig.java                 # OTEL SDK setup (agent or manual)
│       │   ├── OpenTelemetryFilter.java        # Inbound request tracing (manual)
│       │   ├── RestTemplateInterceptor.java    # Outbound RestTemplate tracing (manual)
│       │   ├── RestTemplateBeanPostProcessor.java # Auto-adds interceptor
│       │   ├── WebClientFilter.java            # Outbound WebClient tracing (manual)
│       │   ├── WeConnectorWebClientInjector.java # Injects filter into WeConnector factory
│       │   ├── WeExterfaceInstrumentation.java # Wraps WeExterfaceServiceFactory via Proxy
│       │   ├── RuntimeAttachRunListener.java   # OTel agent auto-attach + YAML bridging
│       │   └── WhenAgentInactiveCondition.java # Condition: true when agent is NOT active
│       └── resources/META-INF/
│           └── spring.factories                # Auto-config + RunListener registration
└── oteljaeger-demo/                        # Demo application
    ├── build.gradle.kts
    └── src/main/java/.../demo/
        ├── OteljaegerDemoApplication.java  # Spring Boot main class
        ├── UserController.java             # REST endpoints
        ├── ExternalUserService.java        # RestTemplate calls
        ├── ExternalUserWebClientService.java # WebClient calls
        └── UserPipelineService.java        # Multi-step pipeline
```

---

## License

Apache License 2.0
