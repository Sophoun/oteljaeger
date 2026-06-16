package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Conditional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "oteljaeger", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OtelJaegerProperties.class)
public class OtelJaegerAutoConfiguration {

	private static final Logger log = LoggerFactory.getLogger(OtelJaegerAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public OpenTelemetry openTelemetry(OtelJaegerProperties properties) {
		OtelConfig config = new OtelConfig(properties);
		return config.openTelemetry();
	}

	@Bean
	@ConditionalOnMissingBean
	public Tracer tracer(OpenTelemetry openTelemetry, OtelJaegerProperties properties) {
		return openTelemetry.getTracer(properties.getServiceName());
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

	/**
	 * When the OTel agent is active, it instruments Reactor Netty (WebClient) automatically.
	 * We only create the manual WebClientFilter when the agent is NOT active to avoid duplicate spans.
	 */
	@Bean
	@ConditionalOnMissingBean(ExchangeFilterFunction.class)
	@ConditionalOnClass(WebClient.class)
	@Conditional(WhenAgentInactiveCondition.class)
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

	/**
	 * When the OTel Java agent is active, it instruments Reactor Netty (WebClient) automatically.
	 * We only inject the manual filter when the agent is NOT active to avoid duplicate spans.
	 */
	@Bean
	@ConditionalOnClass(WebClient.class)
	@Conditional(WhenAgentInactiveCondition.class)
	public BeanPostProcessor webClientFilterInjector(ObjectProvider<ExchangeFilterFunction> filterProvider) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof WebClient) {
					ExchangeFilterFunction filter = filterProvider.getIfAvailable();
					if (filter != null) {
						log.info("[oteljaeger] Injecting OTel filter into WebClient bean: {}", beanName);
						return ((WebClient) bean).mutate().filter(filter).build();
					}
				}
				return bean;
			}
		};
	}

	// ── Agent-active body capture ──────────────────────────────────────

	/**
	 * When the OTel agent is active, it creates CLIENT spans for outbound requests
	 * but does NOT capture request/response bodies. This filter only captures bodies
	 * as events on the current (agent's) span, without creating a new span.
	 */
	@Bean
	@Conditional(WhenAgentActiveCondition.class)
	public WebClientBodyCaptureFilter webClientBodyCaptureFilter(OtelJaegerProperties properties) {
		return new WebClientBodyCaptureFilter(properties);
	}

	/**
	 * When the OTel agent is active, inject the body-only capture filter into
	 * WeConnectorFactory's WebClient fields so outbound bodies are captured.
	 */
	@Bean
	@ConditionalOnProperty(prefix = "oteljaeger", name = "traceExternalApi", havingValue = "true", matchIfMissing = true)
	@Conditional(WhenAgentActiveCondition.class)
	@ConditionalOnClass(name = "com.we3rother.connector.factory.WeConnectorFactory")
	public WeConnectorBodyCaptureInjector weConnectorBodyCaptureInjector(ObjectProvider<WebClientBodyCaptureFilter> filterProvider) {
		WebClientBodyCaptureFilter filter = filterProvider.getIfAvailable();
		if (filter != null) {
			return new WeConnectorBodyCaptureInjector(filter);
		}
		return null;
	}

	@Bean
	@ConditionalOnProperty(prefix = "oteljaeger", name = "traceExternalApi", havingValue = "true", matchIfMissing = true)
	@ConditionalOnClass(name = "com.we3rother.exterface.factory.WeExterfaceServiceFactory")
	public BeanPostProcessor weExterfaceBeanPostProcessor(OtelJaegerProperties properties) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean != null && "com.we3rother.exterface.factory.WeExterfaceServiceFactory".equals(bean.getClass().getName())) {
					WeExterfaceInstrumentation.instrument(bean);
				}
				return bean;
			}
		};
	}

	/**
	 * When the OTel agent is active, it instruments Reactor Netty (WebClient) automatically.
	 * We only inject the filter into WeConnectorFactory when the agent is NOT active.
	 */
	@Bean
	@ConditionalOnProperty(prefix = "oteljaeger", name = "traceExternalApi", havingValue = "true", matchIfMissing = true)
	@Conditional(WhenAgentInactiveCondition.class)
	@ConditionalOnClass(name = "com.we3rother.connector.factory.WeConnectorFactory")
	public WeConnectorWebClientInjector weConnectorWebClientInjector(ObjectProvider<ExchangeFilterFunction> filterProvider) {
		ExchangeFilterFunction filter = filterProvider.getIfAvailable();
		if (filter != null) {
			return new WeConnectorWebClientInjector(filter);
		}
		return null;
	}
}
