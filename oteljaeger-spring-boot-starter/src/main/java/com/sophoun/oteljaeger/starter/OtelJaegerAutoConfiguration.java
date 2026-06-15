package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Auto-configuration for OpenTelemetry + Jaeger tracing.
 * Activated when {@code @EnableOtelJaeger} is present.
 *
 * <p>When the OTel Java agent is attached at runtime (via RuntimeAttachRunListener),
 * it provides its own OpenTelemetry instance and automatically instruments HTTP clients.
 * In that case, the custom OpenTelemetry SDK, RestTemplate interceptor, and WebClientFilter
 * are skipped to avoid conflicts.</p>
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "oteljaeger", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OtelJaegerProperties.class)
public class OtelJaegerAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(OtelJaegerAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public OpenTelemetry openTelemetry(OtelJaegerProperties properties) {
		if (isAgentActive()) {
			log.info("OTel Java agent detected. Using agent-provided OpenTelemetry instance.");
			return io.opentelemetry.api.GlobalOpenTelemetry.get();
		}
		OtelConfig config = new OtelConfig(properties);
		return config.openTelemetry();
	}

	@Bean
	@ConditionalOnMissingBean
	public Tracer tracer(OpenTelemetry openTelemetry, OtelJaegerProperties properties) {
		return openTelemetry.getTracer(properties.getServiceName());
	}

	@Bean
	@Conditional(AgentNotActiveCondition.class)
	@ConditionalOnMissingBean
	public OpenTelemetryFilter openTelemetryFilter(Tracer tracer, OtelJaegerProperties properties) {
		return new OpenTelemetryFilter(tracer, properties);
	}

	@Bean
	@Conditional(AgentNotActiveCondition.class)
	@ConditionalOnMissingBean
	public RestTemplateInterceptor restTemplateInterceptor(Tracer tracer, OtelJaegerProperties properties) {
		return new RestTemplateInterceptor(tracer, properties);
	}

	@Bean
	@Conditional(AgentNotActiveCondition.class)
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

	@Bean
	@Conditional(AgentNotActiveCondition.class)
	@ConditionalOnMissingBean(ExchangeFilterFunction.class)
	@ConditionalOnClass(WebClient.class)
	public ExchangeFilterFunction webClientFilter(Tracer tracer, OtelJaegerProperties properties) {
		return new WebClientFilter(tracer, properties);
	}

	@Bean
	@ConditionalOnMissingBean(WebClient.Builder.class)
	@ConditionalOnClass(WebClient.class)
	public WebClient.Builder webClientBuilder(ObjectProvider<ExchangeFilterFunction> filterProvider) {
		WebClient.Builder builder = WebClient.builder();
		filterProvider.ifAvailable(builder::filter);
		return builder;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(WebClient.class)
	public WebClient webClient(ObjectProvider<WebClient.Builder> builderProvider) {
		WebClient.Builder builder = builderProvider.getIfAvailable();
		if (builder == null) {
			builder = WebClient.builder();
		}
		return builder.build();
	}

	private static boolean isAgentActive() {
		return "true".equals(System.getProperty(RuntimeAttachRunListener.AGENT_ACTIVE_PROPERTY));
	}
}
