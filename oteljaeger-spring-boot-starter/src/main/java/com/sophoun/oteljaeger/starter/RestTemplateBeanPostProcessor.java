package com.sophoun.oteljaeger.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * BeanPostProcessor that automatically adds the OpenTelemetry interceptor
 * to all {@link RestTemplate} instances.
 */
class RestTemplateBeanPostProcessor implements BeanPostProcessor {

	private final ClientHttpRequestInterceptor interceptor;

	RestTemplateBeanPostProcessor(ClientHttpRequestInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (interceptor != null && bean instanceof RestTemplate) {
			RestTemplate restTemplate = (RestTemplate) bean;
			restTemplate.getInterceptors().add(interceptor);
		}
		return bean;
	}
}
