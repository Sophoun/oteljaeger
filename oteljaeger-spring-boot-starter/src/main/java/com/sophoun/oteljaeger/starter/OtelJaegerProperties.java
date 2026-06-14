package com.sophoun.oteljaeger.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenTelemetry + Jaeger tracing.
 *
 * <p>All properties are prefixed with {@code oteljaeger}.</p>
 */
@ConfigurationProperties(prefix = "oteljaeger")
public class OtelJaegerProperties {

	/**
	 * Enable or disable the tracing library.
	 */
	private boolean enabled = true;

	/**
	 * The service name reported to Jaeger.
	 */
	private String serviceName = "oteljaeger";

	/**
	 * The OTLP HTTP exporter endpoint URL.
	 */
	private String exporterEndpoint = "http://localhost:4318/v1/traces";

	/**
	 * Exporter timeout in seconds.
	 */
	private int exporterTimeoutSeconds = 10;

	/**
	 * Whether to capture request headers in traces.
	 */
	private boolean captureHeaders = true;

	/**
	 * Whether to capture request/response bodies in traces.
	 */
	private boolean captureBodies = true;

	/**
	 * Maximum body size to capture (in bytes). -1 for unlimited.
	 */
	private int maxBodySize = 65536;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getExporterEndpoint() {
		return exporterEndpoint;
	}

	public void setExporterEndpoint(String exporterEndpoint) {
		this.exporterEndpoint = exporterEndpoint;
	}

	public int getExporterTimeoutSeconds() {
		return exporterTimeoutSeconds;
	}

	public void setExporterTimeoutSeconds(int exporterTimeoutSeconds) {
		this.exporterTimeoutSeconds = exporterTimeoutSeconds;
	}

	public boolean isCaptureHeaders() {
		return captureHeaders;
	}

	public void setCaptureHeaders(boolean captureHeaders) {
		this.captureHeaders = captureHeaders;
	}

	public boolean isCaptureBodies() {
		return captureBodies;
	}

	public void setCaptureBodies(boolean captureBodies) {
		this.captureBodies = captureBodies;
	}

	public int getMaxBodySize() {
		return maxBodySize;
	}

	public void setMaxBodySize(int maxBodySize) {
		this.maxBodySize = maxBodySize;
	}
}
