package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

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

			ClientRequest wrappedRequest = wrapRequestForBodyCapture(request, span);

			return next.exchange(wrappedRequest)
					.flatMap(response -> {
						captureResponseHeaders(response, span);

						if (properties.isCaptureBodies()) {
							return response.bodyToMono(byte[].class)
									.map(bodyBytes -> {
										if (bodyBytes != null && bodyBytes.length > 0) {
											String responseBody = truncate(
													new String(bodyBytes, StandardCharsets.UTF_8));
											span.setAttribute("http.response.body", responseBody);
										}
										return ClientResponse.create(response.statusCode())
												.headers(h -> h.putAll(response.headers().asHttpHeaders()))
												.body(Flux.just(
														DefaultDataBufferFactory.sharedInstance.wrap(
																bodyBytes != null ? bodyBytes : new byte[0])))
												.build();
									});
						}

						return Mono.just(response);
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

	@SuppressWarnings({"unchecked", "rawtypes"})
	private ClientRequest wrapRequestForBodyCapture(ClientRequest request, Span span) {
		if (!properties.isCaptureBodies()) {
			return request;
		}

		BodyInserter originalBody = request.body();
		if (originalBody == null) {
			return request;
		}

		BodyInserter wrappedBody = (outputMessage, context) -> {
					ClientHttpRequest clientHttpRequest = (ClientHttpRequest) outputMessage;
					ClientHttpRequestDecorator decorator = new ClientHttpRequestDecorator(clientHttpRequest) {
						@Override
						public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
							if (body instanceof Mono) {
								Mono<? extends DataBuffer> mono = (Mono<? extends DataBuffer>) body;
								return mono.flatMap(dataBuffer -> {
									byte[] bytes = new byte[dataBuffer.readableByteCount()];
									dataBuffer.read(bytes);
									DataBufferUtils.release(dataBuffer);
									String requestBody = truncate(new String(bytes, StandardCharsets.UTF_8));
									span.setAttribute("http.request.body", requestBody);
									return super.writeWith(Mono.just(
											DefaultDataBufferFactory.sharedInstance.wrap(bytes)));
								});
							}
							return super.writeWith(body);
						}
					};
					return originalBody.insert(decorator, context);
				};

		return ClientRequest.from(request)
				.body(wrappedBody)
				.build();
	}

	private void captureResponseHeaders(ClientResponse response, Span span) {
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
}
