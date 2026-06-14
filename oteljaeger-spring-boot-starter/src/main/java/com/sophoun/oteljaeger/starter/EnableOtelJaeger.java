package com.sophoun.oteljaeger.starter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable OpenTelemetry + Jaeger tracing for Spring Boot applications.
 *
 * <p>Add this annotation to your Spring Boot application class to enable
 * automatic tracing of inbound HTTP requests and outbound RestTemplate calls.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableOtelJaeger
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 *
 * <p>Configure via application.properties:</p>
 * <pre>
 * oteljaeger.service-name=my-service
 * oteljaeger.exporter-endpoint=http://localhost:4318/v1/traces
 * oteljaeger.enabled=true
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(OtelJaegerAutoConfiguration.class)
public @interface EnableOtelJaeger {
}
