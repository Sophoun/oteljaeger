# OpenTelemetry Jaeger Spring Boot Starter

A Spring Boot starter library for automatic distributed tracing using OpenTelemetry and Jaeger.

## Features

- Automatic tracing of inbound HTTP requests (via Servlet Filter)
- Automatic tracing of outbound RestTemplate calls (via ClientHttpRequestInterceptor)
- Request/response body capture (configurable)
- Request/response header capture (configurable)
- Context propagation across service calls
- Auto-configured RestTemplate with tracing interceptor
- **Fat JAR** with all dependencies included

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

Output: `oteljaeger-spring-boot-starter/build/libs/oteljaeger-spring-boot-starter-0.0.1-SNAPSHOT.jar` (20 MB)

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
    
    // Your other dependencies...
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
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

### Step 4: Create a REST Controller

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

2. Test your endpoint:
```bash
curl http://localhost:8080/api/user
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

Here's a complete working example:

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
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

### MyApplication.java

```java
package com.example.myapp;

import com.sophoun.oteljaeger.starter.EnableOtelJaeger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableOtelJaeger
public class MyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### UserController.java

```java
package com.example.myapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/user")
    public Map<String, Object> getUser() {
        return restTemplate.getForObject(
            "https://jsonplaceholder.typicode.com/users/1", 
            Map.class
        );
    }

    @GetMapping("/user/{id}/posts")
    public Object[] getUserPosts(@PathVariable int id) {
        return restTemplate.getForObject(
            "https://jsonplaceholder.typicode.com/posts?userId=" + id, 
            Object[].class
        );
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

## Example Trace

```
GET /api/users/1 (2340ms)
  HTTP GET /users/1 (325ms)
  HTTP GET /posts (107ms)
  HTTP GET /comments (68ms)
  HTTP GET /todos (195ms)
```

---

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `oteljaeger.enabled` | `true` | Enable/disable tracing |
| `oteljaeger.service-name` | `oteljaeger` | Service name in Jaeger UI |
| `oteljaeger.exporter-endpoint` | `http://localhost:4318/v1/traces` | OTLP HTTP endpoint |
| `oteljaeger.exporter-timeout-seconds` | `10` | Exporter timeout |
| `oteljaeger.capture-headers` | `true` | Capture request/response headers |
| `oteljaeger.capture-bodies` | `true` | Capture request/response bodies |
| `oteljaeger.max-body-size` | `65536` | Max body size to capture (-1 for unlimited) |

---

## Auto-Configured Beans

| Bean | Description |
|------|-------------|
| `OpenTelemetry` | OTEL SDK instance |
| `Tracer` | OTEL Tracer |
| `OpenTelemetryFilter` | Inbound request tracing |
| `RestTemplateInterceptor` | Outbound call tracing |
| `RestTemplateBeanPostProcessor` | Auto-adds interceptor to RestTemplates |
| `RestTemplate` | Pre-configured with tracing interceptor |

All beans use `@ConditionalOnMissingBean`, so you can override any of them.

---

## Custom RestTemplate

If you need a custom `RestTemplate`, define your own bean - it will automatically get the tracing interceptor:

```java
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
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
│       ├── OpenTelemetryFilter.java        # Inbound tracing
│       ├── RestTemplateInterceptor.java    # Outbound tracing
│       ├── RestTemplateBeanPostProcessor.java # Auto-adds interceptor
│       └── OtelConfig.java                # OTEL SDK setup
└── oteljaeger-demo/                        # Demo application
    ├── build.gradle.kts
    └── src/main/java/.../demo/
        ├── OteljaegerDemoApplication.java  # @EnableOtelJaeger
        ├── UserController.java             # REST endpoints
        ├── ExternalUserService.java        # External API calls
        └── UserPipelineService.java        # 7-step pipeline
```

---

## License

Apache License 2.0
