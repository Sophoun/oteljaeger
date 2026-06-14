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

### 1. Build the Fat JAR

```bash
./gradlew :oteljaeger-spring-boot-starter:shadowJar
```

Output:
```
oteljaeger-spring-boot-starter/build/libs/oteljaeger-spring-boot-starter-0.0.1-SNAPSHOT.jar (20 MB)
```

### 2. Copy JAR to Your Project

```bash
mkdir -p libs
cp oteljaeger-spring-boot-starter/build/libs/oteljaeger-spring-boot-starter-0.0.1-SNAPSHOT.jar libs/
```

### 3. Add Dependency

**Gradle:**
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.sophoun</groupId>
    <artifactId>oteljaeger-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/oteljaeger-spring-boot-starter-0.0.1-SNAPSHOT.jar</systemPath>
</dependency>
```

### 4. Enable Tracing

```java
@SpringBootApplication
@EnableOtelJaeger
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 5. Configure

**application.properties:**
```properties
oteljaeger.service-name=my-service
oteljaeger.exporter-endpoint=http://localhost:4318/v1/traces
oteljaeger.enabled=true
oteljaeger.capture-headers=true
oteljaeger.capture-bodies=true
oteljaeger.max-body-size=65536
```

### 6. Start Jaeger

```bash
docker-compose up -d
```

**docker-compose.yml:**
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

### 7. Test

```bash
curl http://localhost:8080/api/user
```

### 8. View Traces

Open http://localhost:16686 in your browser.

---

## Alternative: Use from Maven Repository

### Local Maven

```bash
./gradlew :oteljaeger-spring-boot-starter:publishToMavenLocal
```

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("com.sophoun:oteljaeger-spring-boot-starter:0.0.1-SNAPSHOT")
}
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

## Building

```bash
# Build fat JAR
./gradlew :oteljaeger-spring-boot-starter:shadowJar

# Build demo app
./gradlew :oteljaeger-demo:bootJar

# Build everything
./gradlew clean build
```

---

## License

Apache License 2.0
