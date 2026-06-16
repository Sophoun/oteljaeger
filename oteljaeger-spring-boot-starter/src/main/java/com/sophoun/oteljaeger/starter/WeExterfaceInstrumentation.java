package com.sophoun.oteljaeger.starter;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class WeExterfaceInstrumentation {

    private static final String INSTRUMENTATION_NAME = "com.sophoun.oteljaeger.we-exterface";
    private static final Tracer TRACER = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME);
    private static final String RPC_SYSTEM_ATTR = "rpc.system";

    public static void instrument(Object factory) {
        if (factory == null) {
            return;
        }

        Class<?> factoryClass = factory.getClass();
        if (Proxy.isProxyClass(factoryClass)) {
            factoryClass = factoryClass.getInterfaces()[0];
        }

        try {
            Method executeMethod = factoryClass.getMethod("service", String.class);
            if (executeMethod != null) {
                wrapServiceMethod(factory, executeMethod);
            }
        } catch (NoSuchMethodException e) {
        }
    }

    private static void wrapServiceMethod(Object factory, Method serviceMethod) {
        try {
            Object proxy = Proxy.newProxyInstance(
                    factory.getClass().getClassLoader(),
                    factory.getClass().getInterfaces(),
                    (proxyObj, method, args) -> {
                        if (!method.getName().equals("service") || args.length == 0) {
                            return method.invoke(factory, args);
                        }

                        String serviceId = (String) args[0];
                        String operationName = "WeExterface " + serviceId;

                        Span span = TRACER.spanBuilder(operationName)
                                .setAttribute("we.exterface.service_id", serviceId)
                                .setAttribute(RPC_SYSTEM_ATTR, "we-exterface")
                                .startSpan();

                        try (Scope scope = span.makeCurrent()) {
                            Object result = method.invoke(factory, args);

                            if (result != null) {
                                span.setAttribute("we.exterface.result_class", result.getClass().getName());
                            }
                            span.setStatus(StatusCode.OK);
                            return result;
                        } catch (Exception e) {
                            span.setStatus(StatusCode.ERROR, e.getMessage());
                            span.recordException(e);
                            throw e;
                        } finally {
                            span.end();
                        }
                    }
            );

            java.lang.reflect.Field field = factory.getClass().getDeclaredField("target");
            field.setAccessible(true);
            field.set(factory, proxy);

        } catch (Exception e) {
        }
    }
}