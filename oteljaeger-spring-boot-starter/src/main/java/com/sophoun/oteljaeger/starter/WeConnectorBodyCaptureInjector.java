package com.sophoun.oteljaeger.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;

/**
 * When the OTel Java agent is active, it instruments Reactor Netty automatically
 * but does NOT capture request/response bodies. This injector scans for
 * WeConnectorFactory instances and injects a body-only capture filter
 * into any WebClient fields found via reflection.
 */
class WeConnectorBodyCaptureInjector implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(WeConnectorBodyCaptureInjector.class);
    private static final String WE_CONNECTOR_FACTORY_CLASS = "com.we3rother.connector.factory.WeConnectorFactory";

    private final WebClientBodyCaptureFilter filter;
    private volatile boolean alreadyInjected = false;

    WeConnectorBodyCaptureInjector(WebClientBodyCaptureFilter filter) {
        this.filter = filter;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (alreadyInjected) {
            return;
        }
        alreadyInjected = true;

        ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) event.getApplicationContext();
        ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();

        int injected = 0;
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            try {
                Object bean = beanFactory.getBean(beanName);
                if (bean != null && WE_CONNECTOR_FACTORY_CLASS.equals(bean.getClass().getName())) {
                    log.info("[oteljaeger] Found WeConnectorFactory bean for body capture: {}", beanName);
                    if (injectFilterIntoObject(bean, beanName)) {
                        injected++;
                    }
                }
            } catch (Exception e) {
                // skip beans that can't be resolved
            }
        }

        if (injected > 0) {
            log.info("[oteljaeger] Injected body capture filter into {} WeConnectorFactory instance(s)", injected);
        } else {
            log.warn("[oteljaeger] No WeConnectorFactory instances found for body capture injection");
        }
    }

    private boolean injectFilterIntoObject(Object obj, String path) {
        boolean injected = false;
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) {
                    continue;
                }

                String fieldType = value.getClass().getName();

                if (value instanceof WebClient) {
                    WebClient original = (WebClient) value;
                    WebClient filtered = original.mutate().filter(filter).build();
                    field.set(obj, filtered);
                    log.info("[oteljaeger] Injected body capture filter into WebClient field '{}' in {}", field.getName(), path);
                    injected = true;
                } else if (value instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
                    for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getValue() instanceof WebClient) {
                            WebClient original = (WebClient) entry.getValue();
                            WebClient filtered = original.mutate().filter(filter).build();
                            ((java.util.Map<String, Object>) map).put((String) entry.getKey(), filtered);
                            log.info("[oteljaeger] Injected body capture filter into WebClient in map['{}'] in {}", entry.getKey(), path);
                            injected = true;
                        } else if (entry.getValue() != null && !entry.getValue().getClass().getName().startsWith("java.") && !entry.getValue().getClass().getName().startsWith("org.springframework.")) {
                            if (injectFilterIntoObject(entry.getValue(), path + "." + field.getName() + "[" + entry.getKey() + "]")) {
                                injected = true;
                            }
                        }
                    }
                } else if (value instanceof java.util.Collection) {
                    int i = 0;
                    for (Object item : (java.util.Collection<?>) value) {
                        if (item instanceof WebClient) {
                            WebClient original = (WebClient) item;
                            WebClient filtered = original.mutate().filter(filter).build();
                            // Can't replace in collection easily, log instead
                            log.info("[oteljaeger] Found WebClient in collection[{}] in {} - body capture may not work", i, path);
                        } else if (item != null && !item.getClass().getName().startsWith("java.") && !item.getClass().getName().startsWith("org.springframework.")) {
                            if (injectFilterIntoObject(item, path + "." + field.getName() + "[" + i + "]")) {
                                injected = true;
                            }
                        }
                        i++;
                    }
                } else if (!fieldType.startsWith("java.") && !fieldType.startsWith("org.springframework.")) {
                    if (injectFilterIntoObject(value, path + "." + field.getName())) {
                        injected = true;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[oteljaeger] Could not inject body capture into {}: {}", path, e.getMessage());
        }
        return injected;
    }
}
