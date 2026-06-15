package com.sophoun.oteljaeger.starter;

import io.opentelemetry.contrib.attach.RuntimeAttach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;

/**
 * SpringApplicationRunListener that programmatically attaches the OpenTelemetry Java agent
 * at application startup.
 *
 * <p>When the agent attaches successfully, it provides its own OpenTelemetry SDK and
 * automatically instruments HTTP clients (RestTemplate, WebClient/Reactor Netty).</p>
 *
 * <p>Default agent configuration disables server-side instrumentations (Tomcat, Spring MVC)
 * to avoid deadlocks on macOS Apple Silicon + Netty 4.1.70. Only HTTP client instrumentations
 * (HttpURLConnection for RestTemplate, Reactor Netty for WebClient) are enabled.</p>
 *
 * <p>Users can override any setting via system properties ({@code -Dotel.*}) or
 * environment variables ({@code OTEL_*}).</p>
 *
 * <p>Limitations:
 * <ul>
 *   <li>Requires JDK (not JRE)</li>
 *   <li>Requires {@code -XX:+EnableDynamicAgentLoading} on Java 21+</li>
 *   <li>The agent cannot be updated independently of the application</li>
 * </ul>
 */
public class RuntimeAttachRunListener implements SpringApplicationRunListener {

    private static final Logger log = LoggerFactory.getLogger(RuntimeAttachRunListener.class);

    public static final String AGENT_ACTIVE_PROPERTY = "oteljaeger.agent.active";

    public RuntimeAttachRunListener(SpringApplication application, String[] args) {
    }

    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {
        try {
            setDefaultAgentConfig();
            log.info("Attempting runtime attach of OpenTelemetry Java agent...");
            RuntimeAttach.attachJavaagentToCurrentJvm();
            System.setProperty(AGENT_ACTIVE_PROPERTY, "true");
            log.info("OpenTelemetry Java agent attached successfully. "
                    + "Agent-provided instrumentation will handle HTTP client tracing.");
        } catch (Exception e) {
            System.setProperty(AGENT_ACTIVE_PROPERTY, "false");
            log.warn("Failed to attach OpenTelemetry Java agent at runtime. "
                    + "Library-created WebClient tracing may not work. "
                    + "Consider using -javaagent JVM argument instead. Error: {}", e.getMessage());
        }
    }

    /**
     * Set default agent configuration via system properties before the agent starts.
     * These are defaults that users can override via {@code -Dotel.*} system properties
     * or {@code OTEL_*} environment variables.
     *
     * <p>Disables server-side instrumentations to avoid deadlocks on macOS Apple Silicon.
     * Enables only HTTP client instrumentations for RestTemplate and WebClient tracing.</p>
     */
    private void setDefaultAgentConfig() {
        setIfNotSet("otel.instrumentation.common.default.enabled", "false");
        setIfNotSet("otel.instrumentation.httpclient.enabled", "true");
        setIfNotSet("otel.instrumentation.reactor-netty-client.enabled", "true");
        setIfNotSet("otel.instrumentation.spring-web.enabled", "false");
        setIfNotSet("otel.instrumentation.tomcat.enabled", "false");
    }

    private void setIfNotSet(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
