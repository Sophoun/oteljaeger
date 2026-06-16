package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

class OpenTelemetryFilter extends OncePerRequestFilter {

	private final Tracer tracer;
	private final OtelJaegerProperties properties;

	OpenTelemetryFilter(Tracer tracer, OtelJaegerProperties properties) {
		this.tracer = tracer;
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
		ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

		String operationName = request.getMethod() + " " + request.getRequestURI();
		Span parentSpan = Span.current();
		Span span = tracer.spanBuilder(operationName)
				.setParent(Context.current().with(parentSpan))
				.startSpan();

		Scope scope = span.makeCurrent();
		request.setAttribute("otel.span", span);
		request.setAttribute("otel.scope", scope);

		try {
			span.setAttribute("http.method", request.getMethod());
			span.setAttribute("http.url", request.getRequestURL().toString());
			span.setAttribute("http.scheme", request.getScheme());
			span.setAttribute("http.host", request.getServerName());
			span.setAttribute("http.target", request.getRequestURI());
			span.setAttribute("operation.name", request.getRequestURI());

			if (properties.isCaptureHeaders()) {
				Enumeration<String> headerNames = request.getHeaderNames();
				while (headerNames.hasMoreElements()) {
					String headerName = headerNames.nextElement();
					span.setAttribute("http.request.header." + headerName, request.getHeader(headerName));
				}
			}

			filterChain.doFilter(cachedRequest, cachedResponse);

			if (properties.isCaptureBodies()) {
				byte[] requestBody = cachedRequest.getContentAsByteArray();
				if (requestBody.length > 0) {
					String body = truncate(new String(requestBody, getEncoding(cachedRequest.getCharacterEncoding())));
					span.addEvent("http.request.body", Attributes.of(AttributeKey.stringKey("body"), body));
				}

				byte[] responseBody = cachedResponse.getContentAsByteArray();
				if (responseBody.length > 0) {
					String body = truncate(new String(responseBody, getEncoding(cachedResponse.getCharacterEncoding())));
					span.addEvent("http.response.body", Attributes.of(AttributeKey.stringKey("body"), body));
				}
			}

			for (String headerName : cachedResponse.getHeaderNames()) {
				span.setAttribute("http.response.header." + headerName, cachedResponse.getHeader(headerName));
			}

			int statusCode = cachedResponse.getStatusCode();
			span.setAttribute("http.status_code", statusCode);

			if (statusCode >= 400) {
				span.setStatus(StatusCode.ERROR, "HTTP " + statusCode);
			} else {
				span.setStatus(StatusCode.OK);
			}
			span.setAttribute("trace.status", statusCode < 400 ? "success" : "error");

		} catch (Exception e) {
			span.setStatus(StatusCode.ERROR, e.getMessage());
			span.recordException(e);
			span.setAttribute("trace.status", "error");
			throw e;
		} finally {
			if (scope != null) {
				scope.close();
			}
			span.end();
			cachedResponse.copyBodyToResponse();
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

	private String getEncoding(String encoding) {
		return encoding != null ? encoding : "UTF-8";
	}
}
