package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;

class WebClientFilter implements ExchangeFilterFunction {

	private final Tracer tracer;
	private final OtelJaegerProperties properties;

	WebClientFilter(Tracer tracer, OtelJaegerProperties properties) {
		this.tracer = tracer;
		this.properties = properties;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		Span parentSpan = Span.current();
		HttpMethod method = request.method();
		URI url = request.url();
		String operationName = "HTTP " + method.name() + " " + url.getPath();
		Span span = tracer.spanBuilder(operationName)
				.setParent(Context.current().with(parentSpan))
				.startSpan();

		try {
			span.setAttribute("http.method", method.name());
			span.setAttribute("http.url", url.toString());
			span.setAttribute("http.host", url.getHost());
			span.setAttribute("operation.name", url.getPath());

			if (properties.isCaptureHeaders()) {
				request.headers().forEach((key, values) ->
						span.setAttribute("http.request.header." + key, String.join(", ", values))
				);
			}

			return next.exchange(request)
					.doOnNext(response -> {
						int statusCode = response.statusCode().value();
						span.setAttribute("http.status_code", statusCode);

						response.headers().asHttpHeaders().forEach((key, values) ->
								span.setAttribute("http.response.header." + key, String.join(", ", values))
						);

						if (statusCode >= 400) {
							span.setStatus(StatusCode.ERROR, "HTTP " + statusCode);
						} else {
							span.setStatus(StatusCode.OK);
						}
						span.setAttribute("trace.status", statusCode < 400 ? "success" : "error");
					})
					.doOnError(e -> {
						span.setStatus(StatusCode.ERROR, e.getMessage());
						span.recordException(e);
						span.setAttribute("trace.status", "error");
					})
					.doFinally(signalType -> span.end());
		} catch (Exception e) {
			span.setStatus(StatusCode.ERROR, e.getMessage());
			span.recordException(e);
			span.setAttribute("trace.status", "error");
			span.end();
			return Mono.error(e);
		}
	}
}
