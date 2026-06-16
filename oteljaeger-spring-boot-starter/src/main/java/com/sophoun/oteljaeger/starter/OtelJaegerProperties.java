package com.sophoun.oteljaeger.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry + Jaeger tracing.
 *
 * <p>All properties are prefixed with {@code oteljaeger}.</p>
 */
@ConfigurationProperties(prefix = "oteljaeger")
public class OtelJaegerProperties {

	// ── General ──────────────────────────────────────────────────────

	/** Enable or disable the tracing library. */
	private boolean enabled = true;

	/** The service name reported to Jaeger. */
	private String serviceName = "oteljaeger";

	// ── OTLP Exporter ───────────────────────────────────────────────

	/** The OTLP HTTP exporter endpoint URL (full URL with path). */
	private String exporterEndpoint = "http://localhost:4318/v1/traces";

	/** OTLP protocol: {@code http/protobuf} or {@code grpc}. */
	private String exporterProtocol = "http/protobuf";

	/** Exporter timeout in seconds. */
	private int exporterTimeoutSeconds = 10;

	/** Trace exporter: {@code otlp}, {@code none}, {@code jaeger}, etc. */
	private String tracesExporter = "otlp";

	/** Metrics exporter: {@code none}, {@code otlp}, {@code prometheus}, etc. */
	private String metricsExporter = "none";

	/** Logs exporter: {@code none}, {@code otlp}, etc. */
	private String logsExporter = "none";

	// ── Instrumentation ──────────────────────────────────────────────

	/** Enable Tomcat server instrumentation. Disable to avoid deadlocks with Spring Boot 2.6 + Reactor Netty. */
	private boolean instrumentationTomcatEnabled = false;

	/** Enable Servlet instrumentation. Disable together with Tomcat. */
	private boolean instrumentationServletEnabled = false;

	/** Enable Reactor Netty (WebClient) instrumentation. */
	private boolean instrumentationNettyEnabled = true;

	/** Enable Reactor instrumentation (Project Reactor). */
	private boolean instrumentationReactorEnabled = true;

	// ── Span Capture ─────────────────────────────────────────────────

	/** Whether to capture request headers in traces. */
	private boolean captureHeaders = true;

	/** Whether to capture request/response bodies in traces. */
	private boolean captureBodies = true;

	/** Maximum body size to capture (in bytes). -1 for unlimited. */
	private int maxBodySize = 65536;

	// ── Tracing Scope ────────────────────────────────────────────────

	/** Enable MyBatis SQL query tracing (maps to otel.instrumentation.mybatis.enabled). */
	private boolean traceMyBatis = true;

	/** Enable WeExterfaceServiceFactory external API call tracing. */
	private boolean traceExternalApi = true;

	/** Enable AWS SDK (S3, etc.) tracing (maps to otel.instrumentation.aws-sdk.enabled). */
	private boolean traceAwsSdk = true;

	/** Enable Kafka producer/consumer tracing (maps to otel.instrumentation.kafka.enabled). */
	private boolean traceKafka = true;

	/** Enable Spring Kafka tracing (maps to otel.instrumentation.spring-kafka.enabled). */
	private boolean traceSpringKafka = true;

	// ── Getters & Setters ────────────────────────────────────────────

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }

	public String getServiceName() { return serviceName; }
	public void setServiceName(String serviceName) { this.serviceName = serviceName; }

	public String getExporterEndpoint() { return exporterEndpoint; }
	public void setExporterEndpoint(String exporterEndpoint) { this.exporterEndpoint = exporterEndpoint; }

	public String getExporterProtocol() { return exporterProtocol; }
	public void setExporterProtocol(String exporterProtocol) { this.exporterProtocol = exporterProtocol; }

	public int getExporterTimeoutSeconds() { return exporterTimeoutSeconds; }
	public void setExporterTimeoutSeconds(int exporterTimeoutSeconds) { this.exporterTimeoutSeconds = exporterTimeoutSeconds; }

	public String getTracesExporter() { return tracesExporter; }
	public void setTracesExporter(String tracesExporter) { this.tracesExporter = tracesExporter; }

	public String getMetricsExporter() { return metricsExporter; }
	public void setMetricsExporter(String metricsExporter) { this.metricsExporter = metricsExporter; }

	public String getLogsExporter() { return logsExporter; }
	public void setLogsExporter(String logsExporter) { this.logsExporter = logsExporter; }

	public boolean isInstrumentationTomcatEnabled() { return instrumentationTomcatEnabled; }
	public void setInstrumentationTomcatEnabled(boolean instrumentationTomcatEnabled) { this.instrumentationTomcatEnabled = instrumentationTomcatEnabled; }

	public boolean isInstrumentationServletEnabled() { return instrumentationServletEnabled; }
	public void setInstrumentationServletEnabled(boolean instrumentationServletEnabled) { this.instrumentationServletEnabled = instrumentationServletEnabled; }

	public boolean isInstrumentationNettyEnabled() { return instrumentationNettyEnabled; }
	public void setInstrumentationNettyEnabled(boolean instrumentationNettyEnabled) { this.instrumentationNettyEnabled = instrumentationNettyEnabled; }

	public boolean isInstrumentationReactorEnabled() { return instrumentationReactorEnabled; }
	public void setInstrumentationReactorEnabled(boolean instrumentationReactorEnabled) { this.instrumentationReactorEnabled = instrumentationReactorEnabled; }

	public boolean isCaptureHeaders() { return captureHeaders; }
	public void setCaptureHeaders(boolean captureHeaders) { this.captureHeaders = captureHeaders; }

	public boolean isCaptureBodies() { return captureBodies; }
	public void setCaptureBodies(boolean captureBodies) { this.captureBodies = captureBodies; }

	public int getMaxBodySize() { return maxBodySize; }
	public void setMaxBodySize(int maxBodySize) { this.maxBodySize = maxBodySize; }

	public boolean isTraceMyBatis() { return traceMyBatis; }
	public void setTraceMyBatis(boolean traceMyBatis) { this.traceMyBatis = traceMyBatis; }

	public boolean isTraceExternalApi() { return traceExternalApi; }
	public void setTraceExternalApi(boolean traceExternalApi) { this.traceExternalApi = traceExternalApi; }

	public boolean isTraceAwsSdk() { return traceAwsSdk; }
	public void setTraceAwsSdk(boolean traceAwsSdk) { this.traceAwsSdk = traceAwsSdk; }

	public boolean isTraceKafka() { return traceKafka; }
	public void setTraceKafka(boolean traceKafka) { this.traceKafka = traceKafka; }

	public boolean isTraceSpringKafka() { return traceSpringKafka; }
	public void setTraceSpringKafka(boolean traceSpringKafka) { this.traceSpringKafka = traceSpringKafka; }
}
