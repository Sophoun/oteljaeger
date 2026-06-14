# OpenTelemetry Jaeger Spring Boot Starter

A Spring Boot starter library for automatic distributed tracing using OpenTelemetry and Jaeger.

## Features

- Automatic tracing of inbound HTTP requests (via Servlet Filter)
- Automatic tracing of outbound RestTemplate calls (via ClientHttpRequestInterceptor)
- Request/response body capture (configurable)
- Request/response header capture (configurable)
- Context propagation across service calls
- Auto-configured RestTemplate with tracing interceptor

## Requirements

- Java 8+
- Spring Boot 2.0.x - 2.7.x
- Jaeger (via Docker or external)

## Quick Start

### 1. Add Dependency

**Gradle:**
```kotlin
implementation("com.sophoun:oteljaeger-spring-boot-starter:0.0.1-SNAPSHOT")
```

**Maven:**
```xml
<dependency>
    <groupId>com.sophoun</groupId>
    <artifactId>oteljaeger-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. Enable Tracing

Add `@EnableOtelJaeger` to your Spring Boot application class:

```java
@SpringBootApplication
@EnableOtelJaeger
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 3. Configure

Add to `application.properties`:

```properties
# Service name shown in Jaeger UI
oteljaeger.service-name=my-service

# OTLP HTTP endpoint (default: http://localhost:4318/v1/traces)
oteljaeger.exporter-endpoint=http://localhost:4318/v1/traces

# Enable/disable tracing (default: true)
oteljaeger.enabled=true

# Capture request/response headers (default: true)
oteljaeger.capture-headers=true

# Capture request/response bodies (default: true)
oteljaeger.capture-bodies=true

# Maximum body size in bytes, -1 for unlimited (default: 65536)
oteljaeger.max-body-size=65536
```

### 4. Start Jaeger

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

### 5. Access Jaeger UI

Open http://localhost:16686 in your browser.

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

## Auto-Configured Beans

The starter auto-configures:

| Bean | Description |
|------|-------------|
| `OpenTelemetry` | OTEL SDK instance |
| `Tracer` | OTEL Tracer |
| `OpenTelemetryFilter` | Inbound request tracing |
| `RestTemplateInterceptor` | Outbound call tracing |
| `RestTemplateBeanPostProcessor` | Auto-adds interceptor to RestTemplates |
| `RestTemplate` | Pre-configured with tracing interceptor |

All beans use `@ConditionalOnMissingBean`, so you can override any of them.

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

## Disabling Tracing

```properties
oteljaeger.enabled=false
```

Or exclude auto-configuration:

```java
@SpringBootApplication(exclude = {OtelJaegerAutoConfiguration.class})
```

## Project Structure

```
oteljaeger/
├── oteljaeger-spring-boot-starter/  # Library
│   └── src/main/java/.../starter/
│       ├── EnableOtelJaeger.java
│       ├── OtelJaegerProperties.java
│       ├── OtelJaegerAutoConfiguration.java
│       ├── OpenTelemetryFilter.java
│       ├── RestTemplateInterceptor.java
│       └── RestTemplateBeanPostProcessor.java
├── oteljaeger-demo/                 # Demo application
└── docker-compose.yml               # Jaeger setup
```

## Publishing to Maven

### Local Maven Repository

```bash
./gradlew :oteljaeger-spring-boot-starter:publishToMavenLocal
```

Then use in other projects:

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("com.sophoun:oteljaeger-spring-boot-starter:0.0.1-SNAPSHOT")
}
```

### Maven Central (via Sonatype OSSRH)

1. Create account at https://issues.sonatype.org and request access to `ossrh`

2. Create `~/.gradle/gradle.properties`:
```properties
ossrhUsername=your-username
ossrhPassword=your-password
signing.keyId=your-key-id
signing.password=your-key-password
signing.secretKeyRingFile=/path/to/secring.gpg
```

3. Publish:
```bash
./gradlew :oteljaeger-spring-boot-starter:publishMavenPublicationToOSSRHRepository
```

### Private Repository (Artifactory/Nexus)

Update `build.gradle.kts` and run:
```bash
./gradlew :oteljaeger-spring-boot-starter:publish
```

### Release Version

1. Update version in `build.gradle.kts`:
```kotlin
version = "1.0.0"
```

2. Publish:
```bash
./gradlew :oteljaeger-spring-boot-starter:publishToMavenLocal
```

## License

Apache License 2.0
