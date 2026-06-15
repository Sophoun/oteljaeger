package com.sophoun.oteljaeger.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * BeanPostProcessor that automatically adds the OpenTelemetry filter
 * to all {@link WebClient.Builder} instances.
 */
class WebClientBeanPostProcessor implements BeanPostProcessor {

	private final ExchangeFilterFunction filter;

	WebClientBeanPostProcessor(ExchangeFilterFunction filter) {
		this.filter = filter;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof WebClient.Builder) {
			WebClient.Builder builder = (WebClient.Builder) bean;
			builder.filter(filter);
		}
		return bean;
	}
}
