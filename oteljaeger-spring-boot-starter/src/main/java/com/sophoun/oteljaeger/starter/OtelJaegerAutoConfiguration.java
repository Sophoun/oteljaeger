package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Auto-configuration for OpenTelemetry + Jaeger tracing.
 * Activated when {@code @EnableOtelJaeger} is present.
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "oteljaeger", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OtelJaegerProperties.class)
public class OtelJaegerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public OpenTelemetry openTelemetry(OtelJaegerProperties properties) {
		OtelConfig config = new OtelConfig(properties);
		return config.openTelemetry();
	}

	@Bean
	@ConditionalOnMissingBean
	public Tracer tracer(OpenTelemetry openTelemetry, OtelJaegerProperties properties) {
		OtelConfig config = new OtelConfig(properties);
		return config.tracer(openTelemetry);
	}

	@Bean
	@ConditionalOnMissingBean
	public OpenTelemetryFilter openTelemetryFilter(Tracer tracer, OtelJaegerProperties properties) {
		return new OpenTelemetryFilter(tracer, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public RestTemplateInterceptor restTemplateInterceptor(Tracer tracer, OtelJaegerProperties properties) {
		return new RestTemplateInterceptor(tracer, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public RestTemplateBeanPostProcessor restTemplateBeanPostProcessor(RestTemplateInterceptor interceptor) {
		return new RestTemplateBeanPostProcessor(interceptor);
	}

	@Bean
	@ConditionalOnMissingBean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(5))
				.build();
	}
}
