package com.sophoun.oteljaeger.starter;

import io.opentelemetry.contrib.attach.RuntimeAttach;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class RuntimeAttachRunListener implements SpringApplicationRunListener {

    public static final String AGENT_ACTIVE_PROPERTY = "oteljaeger.agent.active";

    private final String[] args;

    public RuntimeAttachRunListener(SpringApplication application, String[] args) {
        this.args = args;
    }

    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {
        // Bypass main() method check since we're called from SpringApplicationRunListener
        System.setProperty("otel.javaagent.testing.runtime-attach.main-method-check", "false");
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        // 1. Parse --oteljaeger.* CLI args (highest priority)
        parseArgs();

        // 2. Read all oteljaeger.* from application.yml and set otel.* system properties for the agent
        setAgentSystemProperty(environment, "oteljaeger.traces-exporter",        "otel.traces.exporter",             "otlp");
        setAgentSystemProperty(environment, "oteljaeger.exporter-protocol",      "otel.exporter.otlp.protocol",      "http/protobuf");
        setAgentSystemProperty(environment, "oteljaeger.metrics-exporter",       "otel.metrics.exporter",            "none");
        setAgentSystemProperty(environment, "oteljaeger.logs-exporter",          "otel.logs.exporter",               "none");

        setAgentSystemProperty(environment, "oteljaeger.instrumentation-tomcat-enabled",  "otel.instrumentation.tomcat.enabled",  "false");
        setAgentSystemProperty(environment, "oteljaeger.instrumentation-servlet-enabled", "otel.instrumentation.servlet.enabled", "false");
        setAgentSystemProperty(environment, "oteljaeger.instrumentation-netty-enabled",   "otel.instrumentation.netty-4.1.enabled", "true");
        setAgentSystemProperty(environment, "oteljaeger.instrumentation-reactor-enabled", "otel.instrumentation.reactor.enabled",  "true");

        // Map tracing scope flags to agent's instrumentation toggles
        setAgentSystemProperty(environment, "oteljaeger.trace-kafka",        "otel.instrumentation.kafka.enabled",          "true");
        setAgentSystemProperty(environment, "oteljaeger.trace-spring-kafka", "otel.instrumentation.spring-kafka.enabled",   "true");
        setAgentSystemProperty(environment, "oteljaeger.trace-aws-sdk",      "otel.instrumentation.aws-sdk.enabled",        "true");
        setAgentSystemProperty(environment, "oteljaeger.trace-mybatis",      "otel.instrumentation.mybatis.enabled",        "true");

        // 3. Resolve agent's service name
        //    Priority: -Dotel.service.name / OTEL_SERVICE_NAME > --oteljaeger.service-name > application.yml > default
        if (System.getProperty("otel.service.name") == null && System.getenv("OTEL_SERVICE_NAME") == null) {
            String serviceName = resolveValue(environment, "oteljaeger.service-name", "spring-boot-app");
            System.setProperty("otel.service.name", serviceName);
        }

        // 4. Resolve agent's OTLP endpoint
        //    Priority: -Dotel.exporter.otlp.endpoint > --oteljaeger.exporter-endpoint > application.yml > default
        if (System.getProperty("otel.exporter.otlp.endpoint") == null) {
            String endpoint = resolveValue(environment, "oteljaeger.exporter-endpoint", "http://localhost:4318/v1/traces");
            System.setProperty("otel.exporter.otlp.endpoint", toBaseUrl(endpoint));
        }

        System.out.println("[oteljaeger] Agent config:");
        System.out.println("  service.name         = " + System.getProperty("otel.service.name"));
        System.out.println("  endpoint             = " + System.getProperty("otel.exporter.otlp.endpoint"));
        System.out.println("  traces.exporter     = " + System.getProperty("otel.traces.exporter"));
        System.out.println("  exporter.protocol    = " + System.getProperty("otel.exporter.otlp.protocol"));
        System.out.println("  tomcat.enabled       = " + System.getProperty("otel.instrumentation.tomcat.enabled"));
        System.out.println("  netty.enabled        = " + System.getProperty("otel.instrumentation.netty-4.1.enabled"));

        // 5. Set extension JARs — extract bundled otel-netty-plugin.jar
        if (System.getProperty("otel.javaagent.extensions") == null) {
            try {
                String extensionName = "otel-netty-plugin.jar";
                Path resolvedPath = null;

                // Strategy 1: look for the JAR on filesystem next to the running JAR
                URL runningJarUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
                if (runningJarUrl != null) {
                    File runningJar = new File(runningJarUrl.toURI());
                    File siblingJar = new File(runningJar.getParentFile(), extensionName);
                    if (siblingJar.exists()) {
                        resolvedPath = siblingJar.toPath();
                        System.out.println("[oteljaeger] Extension JAR found next to running JAR: " + resolvedPath.toAbsolutePath());
                    }
                }

                // Strategy 2: extract from classpath resource to temp file
                if (resolvedPath == null) {
                    InputStream is = getClass().getClassLoader().getResourceAsStream(extensionName);
                    if (is != null) {
                        resolvedPath = Files.createTempFile("otel-netty-plugin-", ".jar");
                        resolvedPath.toFile().deleteOnExit();
                        Files.copy(is, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                        is.close();
                        System.out.println("[oteljaeger] Extension JAR extracted to: " + resolvedPath.toAbsolutePath());
                    }
                }

                if (resolvedPath != null) {
                    System.setProperty("otel.javaagent.extensions", resolvedPath.toAbsolutePath().toString());
                } else {
                    System.out.println("[oteljaeger] Extension JAR not found: " + extensionName);
                }
            } catch (Exception e) {
                System.out.println("[oteljaeger] Failed to resolve extension JAR: " + e.getMessage());
            }
        }

        // 6. Attach the agent — application.yml properties are now available
        try {
            RuntimeAttach.attachJavaagentToCurrentJVM();
            System.setProperty(AGENT_ACTIVE_PROPERTY, "true");
            System.out.println("[oteljaeger] OTel agent attached successfully");
        } catch (Exception e) {
            System.setProperty(AGENT_ACTIVE_PROPERTY, "false");
            System.out.println("[oteljaeger] Failed to attach OTel agent: " + e.getMessage());
            System.out.println("[oteljaeger] Falling back to manual OTel SDK");
        }
    }

    /**
     * Reads a value from environment (CLI args > system property > application.yml > default)
     * and sets it as a system property for the OTel agent.
     */
    private void setAgentSystemProperty(ConfigurableEnvironment environment, String yamlKey, String otelKey, String defaultValue) {
        // System property overrides YAML (set by CLI parseArgs or user via -D)
        if (System.getProperty(otelKey) != null) {
            return;
        }
        String value = environment.getProperty(yamlKey);
        if (value == null) {
            value = defaultValue;
        }
        System.setProperty(otelKey, value);
    }

    /**
     * Resolves a value: system property > YAML > default.
     */
    private String resolveValue(ConfigurableEnvironment environment, String yamlKey, String defaultValue) {
        String sysPropKey = yamlKey;
        String sysVal = System.getProperty(sysPropKey);
        if (sysVal != null) {
            return sysVal;
        }
        String yamlVal = environment.getProperty(yamlKey);
        return yamlVal != null ? yamlVal : defaultValue;
    }

    /**
     * Extracts base URL from a full OTLP endpoint URL.
     * e.g. "http://localhost:4318/v1/traces" -> "http://localhost:4318"
     */
    private String toBaseUrl(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (Exception e) {
            return "http://localhost:4318";
        }
    }

    private void parseArgs() {
        if (args == null) return;
        for (String arg : args) {
            if (arg != null && arg.startsWith("--oteljaeger.")) {
                int eq = arg.indexOf('=');
                if (eq > 0) {
                    String key = arg.substring(2, eq);   // strip "--"
                    String value = arg.substring(eq + 1);
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            }
        }
    }
}
