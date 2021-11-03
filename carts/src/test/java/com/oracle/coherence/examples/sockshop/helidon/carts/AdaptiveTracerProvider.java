/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.examples.sockshop.helidon.carts;

import io.helidon.config.Config;
import io.helidon.tracing.TracerBuilder;
import io.helidon.tracing.spi.TracerProvider;
import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerFactory;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@Priority(1)
public class AdaptiveTracerProvider implements TracerProvider {

    private static AtomicReference<TracerFactory> TRACER_FACTORY = new AtomicReference<>();

    @Override
    public TracerBuilder<?> createBuilder() {
        return new TracerBuilder<>() {
            @Override
            public TracerBuilder serviceName(String name) {
                return this;
            }

            @Override
            public TracerBuilder collectorProtocol(String protocol) {
                return this;
            }

            @Override
            public TracerBuilder collectorPort(int port) {
                return this;
            }

            @Override
            public TracerBuilder collectorHost(String host) {
                return this;
            }

            @Override
            public TracerBuilder collectorPath(String path) {
                return this;
            }

            @Override
            public TracerBuilder addTracerTag(String key, String value) {
                return this;
            }

            @Override
            public TracerBuilder addTracerTag(String key, Number value) {
                return this;
            }

            @Override
            public TracerBuilder addTracerTag(String key, boolean value) {
                return this;
            }

            @Override
            public TracerBuilder config(Config config) {
                return this;
            }

            @Override
            public TracerBuilder enabled(boolean enabled) {
                return this;
            }

            @Override
            public TracerBuilder registerGlobal(boolean global) {
                return this;
            }

            @Override
            public Tracer build() {
                TracerFactory tracerFactory = TRACER_FACTORY.get();
                if (tracerFactory == null) {
                    tracerFactory = ServiceLoader.load(TracerFactory.class).findFirst().orElseThrow();
                    TRACER_FACTORY.compareAndSet(null, tracerFactory);
                }
                return tracerFactory.getTracer();
            }
        };
    }
}
