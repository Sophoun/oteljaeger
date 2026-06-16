package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * An {@link ExchangeFilterFunction} that ONLY captures request/response bodies
 * as events on the current span. It does NOT create a new span.
 *
 * <p>Used alongside the OTel Java agent when the agent already creates CLIENT
 * spans for outbound requests but doesn't capture body content.</p>
 */
class WebClientBodyCaptureFilter implements ExchangeFilterFunction {

    private final OtelJaegerProperties properties;

    WebClientBodyCaptureFilter(OtelJaegerProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        if (!properties.isCaptureBodies()) {
            return next.exchange(request);
        }

        ClientRequest wrappedRequest = wrapRequestForBodyCapture(request);

        return next.exchange(wrappedRequest)
                .flatMap(response -> {
                    if (properties.isCaptureBodies()) {
                        return response.bodyToMono(byte[].class)
                                .flatMap(bodyBytes -> Mono.deferContextual(ctx -> {
                                    String responseBody = (bodyBytes != null && bodyBytes.length > 0)
                                            ? truncate(new String(bodyBytes, StandardCharsets.UTF_8))
                                            : null;

                                    io.opentelemetry.context.Context otelCtx = ctx.get(
                                            io.opentelemetry.context.Context.class);
                                    Span span = Span.fromContext(otelCtx);

                                    if (responseBody != null) {
                                        span.addEvent("http.response.body",
                                                Attributes.of(AttributeKey.stringKey("body"), responseBody));
                                    }

                                    return Mono.just(ClientResponse.create(response.statusCode())
                                            .headers(h -> h.putAll(response.headers().asHttpHeaders()))
                                            .body(Flux.just(
                                                    DefaultDataBufferFactory.sharedInstance.wrap(
                                                            bodyBytes != null ? bodyBytes : new byte[0])))
                                            .build());
                                }));
                    }
                    return Mono.just(response);
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ClientRequest wrapRequestForBodyCapture(ClientRequest request) {
        if (request.body() == null) {
            return request;
        }

        org.springframework.web.reactive.function.BodyInserter originalBody = request.body();
        org.springframework.web.reactive.function.BodyInserter wrappedBody = (outputMessage, inserterCtx) -> {
            ClientHttpRequestDecorator decorator = new ClientHttpRequestDecorator(
                    (org.springframework.http.client.reactive.ClientHttpRequest) outputMessage) {
                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    if (body instanceof Mono) {
                        Mono<? extends DataBuffer> mono = (Mono<? extends DataBuffer>) body;
                        return mono.flatMap(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            String requestBody = truncate(new String(bytes, StandardCharsets.UTF_8));
                            return Mono.deferContextual(ctx -> {
                                io.opentelemetry.context.Context otelCtx = ctx.get(
                                        io.opentelemetry.context.Context.class);
                                Span.fromContext(otelCtx).addEvent("http.request.body",
                                        Attributes.of(AttributeKey.stringKey("body"), requestBody));
                                return super.writeWith(Mono.just(
                                        DefaultDataBufferFactory.sharedInstance.wrap(bytes)));
                            });
                        });
                    }
                    return super.writeWith(body);
                }
            };
            return originalBody.insert(decorator, inserterCtx);
        };

        return ClientRequest.from(request)
                .body(wrappedBody)
                .build();
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
