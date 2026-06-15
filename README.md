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

Add `@EnableOtelJaeger` annotation to your main application class:

```java
package com.example.myapp;

import com.sophoun.oteljaeger.starter.EnableOtelJaeger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableOtelJaeger
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

Create `src/main/resources/application.properties`:

```properties
# Application name
spring.application.name=my-app

# Server port
server.port=8080

# Otel Jaeger configuration
oteljaeger.service-name=my-app
oteljaeger.exporter-endpoint=http://localhost:4318/v1/traces
oteljaeger.enabled=true
oteljaeger.capture-headers=true
oteljaeger.capture-bodies=true
oteljaeger.max-body-size=65536
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

1. `RuntimeAttachRunListener` runs during Spring Boot startup (before beans are created)
2. It configures the agent to enable only HTTP client instrumentations:
   - `otel.instrumentation.httpclient.enabled=true` (for RestTemplate)
   - `otel.instrumentation.reactor-netty-client.enabled=true` (for WebClient)
   - `otel.instrumentation.tomcat.enabled=false` (avoids server-side deadlocks)
   - `otel.instrumentation.spring-web.enabled=false` (avoids server-side deadlocks)
3. Attaches the agent to the running JVM
4. The agent instruments all outgoing HTTP calls automatically

### Overriding Agent Configuration

You can override the default agent settings via system properties or environment variables:

```bash
# Via system properties
java -Dotel.instrumentation.tomcat.enabled=true -jar my-app.jar

# Via environment variables
export OTEL_INSTRUMENTATION_TOMCAT_ENABLED=true
java -jar my-app.jar
```

### Disabling Auto-Attach

To disable the agent auto-attach and use manual instrumentation only:

```properties
# Set to false to skip agent attachment
# The agent status is stored in system property: oteljaeger.agent.active
```

The auto-attach can be disabled by removing the `RuntimeAttachRunListener` registration from
`META-INF/spring.factories`, or by catching and ignoring the agent attachment failure.

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

import com.sophoun.oteljaeger.starter.EnableOtelJaeger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableOtelJaeger
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

### application.properties

```properties
spring.application.name=my-traced-app
server.port=8080

oteljaeger.service-name=my-traced-app
oteljaeger.exporter-endpoint=http://localhost:4318/v1/traces
```

---

## What Gets Traced

### Inbound Requests (Automatic)
- HTTP method, URL, headers
- Request body (POST/PUT/PATCH)
- Response body
- Status code
- Duration

### Outbound RestTemplate Calls (Automatic)
- HTTP method, URL, headers
- Request body
- Response body
- Status code
- Duration
- Parent-child relationship with inbound request

### Outbound WebClient Calls (Automatic)
- HTTP method, URL, headers
- Request body
- Response body
- Status code
- Duration
- Parent-child relationship with inbound request
- Works with both Spring-managed and library-created WebClient instances

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

### Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `oteljaeger.enabled` | `true` | Enable/disable tracing |
| `oteljaeger.service-name` | `oteljaeger` | Service name in Jaeger UI |
| `oteljaeger.exporter-endpoint` | `http://localhost:4318/v1/traces` | OTLP HTTP endpoint |
| `oteljaeger.exporter-timeout-seconds` | `10` | Exporter timeout |
| `oteljaeger.capture-headers` | `true` | Capture request/response headers |
| `oteljaeger.capture-bodies` | `true` | Capture request/response bodies |
| `oteljaeger.max-body-size` | `65536` | Max body size to capture (-1 for unlimited) |

### Agent Configuration (via system properties or env vars)

| System Property | Env Variable | Default | Description |
|-----------------|--------------|---------|-------------|
| `otel.instrumentation.common.default.enabled` | `OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED` | `false` | Enable all instrumentations |
| `otel.instrumentation.httpclient.enabled` | `OTEL_INSTRUMENTATION_HTTPCLIENT_ENABLED` | `true` | Trace HttpURLConnection (RestTemplate) |
| `otel.instrumentation.reactor-netty-client.enabled` | `OTEL_INSTRUMENTATION_REACTOR_NETTY_CLIENT_ENABLED` | `true` | Trace Reactor Netty (WebClient) |
| `otel.instrumentation.tomcat.enabled` | `OTEL_INSTRUMENTATION_TOMCAT_ENABLED` | `false` | Trace Tomcat server requests |
| `otel.instrumentation.spring-web.enabled` | `OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED` | `false` | Trace Spring MVC server requests |

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

### With OTel Agent (Agent Instrumentation)

When the OTel Java agent attaches successfully, it provides its own OpenTelemetry SDK
and instrumentations. The following manual beans are **skipped** to avoid conflicts:

- `OpenTelemetryFilter`
- `RestTemplateInterceptor`
- `RestTemplateBeanPostProcessor`
- `WebClientFilter`

The agent handles all tracing automatically, including library-created HTTP clients.

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

### macOS Apple Silicon

On macOS with Apple Silicon (M1/M2/M3), the OTel Java agent disables Tomcat and Spring MVC
server-side instrumentations by default to avoid deadlocks caused by Netty 4.1.70's incomplete
Apple Silicon DNS support. Only HTTP client instrumentations are enabled.

If you need server-side instrumentation on macOS, pass the JVM argument:

```bash
java -Djava.net.preferIPv4Stack=true -jar my-app.jar
```

### Linux / Production

On Linux, all instrumentations work correctly. To enable server-side tracing:

```bash
java -Dotel.instrumentation.tomcat.enabled=true \
     -Dotel.instrumentation.spring-web.enabled=true \
     -jar my-app.jar
```

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
│   └── src/main/java/.../starter/
│       ├── EnableOtelJaeger.java           # @EnableOtelJaeger annotation
│       ├── OtelJaegerProperties.java       # Configuration properties
│       ├── OtelJaegerAutoConfiguration.java # Auto-configuration
│       ├── OpenTelemetryFilter.java        # Inbound tracing (manual)
│       ├── RestTemplateInterceptor.java    # Outbound RestTemplate tracing (manual)
│       ├── RestTemplateBeanPostProcessor.java # Auto-adds interceptor
│       ├── WebClientFilter.java            # Outbound WebClient tracing (manual)
│       ├── RuntimeAttachRunListener.java   # OTel agent auto-attach
│       ├── AgentNotActiveCondition.java    # Conditional for agent detection
│       └── OtelConfig.java                # OTEL SDK setup
├── src/main/resources/META-INF/
│   └── spring.factories                    # Auto-config + RunListener registration
└── oteljaeger-demo/                        # Demo application
    ├── build.gradle.kts
    └── src/main/java/.../demo/
        ├── OteljaegerDemoApplication.java  # @EnableOtelJaeger
        ├── UserController.java             # REST endpoints
        ├── ExternalUserService.java        # RestTemplate calls
        ├── ExternalUserWebClientService.java # WebClient calls
        └── UserPipelineService.java        # Multi-step pipeline
```

---

## License

Apache License 2.0
