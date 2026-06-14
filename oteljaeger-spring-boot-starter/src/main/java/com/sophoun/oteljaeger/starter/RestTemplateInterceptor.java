package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class RestTemplateInterceptor implements ClientHttpRequestInterceptor {

	private final Tracer tracer;
	private final OtelJaegerProperties properties;

	RestTemplateInterceptor(Tracer tracer, OtelJaegerProperties properties) {
		this.tracer = tracer;
		this.properties = properties;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		Span parentSpan = Span.current();
		String operationName = "HTTP " + request.getMethod().name() + " " + request.getURI().getPath();
		Span span = tracer.spanBuilder(operationName)
				.setParent(Context.current().with(parentSpan))
				.startSpan();
		try {
			span.setAttribute("http.method", request.getMethod().name());
			span.setAttribute("http.url", request.getURI().toString());
			span.setAttribute("http.host", request.getURI().getHost());
			span.setAttribute("operation.name", request.getURI().getPath());

			if (properties.isCaptureHeaders()) {
				request.getHeaders().forEach((key, values) ->
						span.setAttribute("http.request.header." + key, String.join(", ", values))
				);
			}

			if (properties.isCaptureBodies() && body != null && body.length > 0) {
				String requestBody = truncate(new String(body));
				span.setAttribute("http.request.body", requestBody);
			}

			ClientHttpResponse response = execution.execute(request, body);

			byte[] responseBytes = StreamUtils.copyToByteArray(response.getBody());
			ClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(response, responseBytes);

			int statusCode = response.getStatusCode().value();
			span.setAttribute("http.status_code", statusCode);

			response.getHeaders().forEach((key, values) ->
					span.setAttribute("http.response.header." + key, String.join(", ", values))
			);

			if (properties.isCaptureBodies()) {
				String responseBody = truncate(new String(responseBytes));
				if (!responseBody.isEmpty()) {
					span.setAttribute("http.response.body", responseBody);
				}
			}

			if (statusCode >= 400) {
				span.setStatus(StatusCode.ERROR, "HTTP " + statusCode);
			} else {
				span.setStatus(StatusCode.OK);
			}

			span.setAttribute("trace.status", statusCode < 400 ? "success" : "error");
			return bufferedResponse;
		} catch (Exception e) {
			span.setStatus(StatusCode.ERROR, e.getMessage());
			span.recordException(e);
			span.setAttribute("trace.status", "error");
			throw e;
		} finally {
			span.end();
		}
	}

	private String truncate(String value) {
		if (properties.getMaxBodySize() < 0) {
			return value;
		}
		if (value.length() > properties.getMaxBodySize()) {
			return value.substring(0, properties.getMaxBodySize()) + "...(truncated)";
		}
		return value;
	}

	private static class BufferedClientHttpResponse implements ClientHttpResponse {

		private final ClientHttpResponse delegate;
		private final byte[] body;

		BufferedClientHttpResponse(ClientHttpResponse delegate, byte[] body) {
			this.delegate = delegate;
			this.body = body;
		}

		@Override
		public org.springframework.http.HttpStatus getStatusCode() throws IOException {
			return delegate.getStatusCode();
		}

		@Override
		@SuppressWarnings("deprecation")
		public int getRawStatusCode() throws IOException {
			return delegate.getRawStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return delegate.getStatusText();
		}

		@Override
		public HttpHeaders getHeaders() {
			return delegate.getHeaders();
		}

		@Override
		public InputStream getBody() throws IOException {
			return new ByteArrayInputStream(body);
		}

		@Override
		public void close() {
			delegate.close();
		}
	}
}
