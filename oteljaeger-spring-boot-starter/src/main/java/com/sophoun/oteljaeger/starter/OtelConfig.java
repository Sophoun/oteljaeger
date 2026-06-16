package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.util.concurrent.TimeUnit;

class OtelConfig {

	private final OtelJaegerProperties properties;

	OtelConfig(OtelJaegerProperties properties) {
		this.properties = properties;
	}

	OpenTelemetry openTelemetry() {
		// If the OTel agent is active, use its GlobalOpenTelemetry instance
		if (isAgentActive()) {
			return io.opentelemetry.api.GlobalOpenTelemetry.get();
		}

		// Otherwise, create a manual SDK (fallback when agent is not available)
		OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
				.setEndpoint(properties.getExporterEndpoint())
				.setTimeout(properties.getExporterTimeoutSeconds(), TimeUnit.SECONDS)
				.build();

		SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
				.setSampler(io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn())
				.addSpanProcessor(SimpleSpanProcessor.create(exporter))
				.setResource(io.opentelemetry.sdk.resources.Resource.builder()
						.put(AttributeKey.stringKey("service.name"), properties.getServiceName())
						.build())
				.build();

		return OpenTelemetrySdk.builder()
				.setTracerProvider(tracerProvider)
				.buildAndRegisterGlobal();
	}

	private boolean isAgentActive() {
		return "true".equals(System.getProperty(RuntimeAttachRunListener.AGENT_ACTIVE_PROPERTY));
	}
}
