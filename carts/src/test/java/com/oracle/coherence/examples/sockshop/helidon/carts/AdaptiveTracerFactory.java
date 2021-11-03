/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.examples.sockshop.helidon.carts;

import com.tangosol.net.CacheFactory;
import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.InMemoryReporter;
import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerFactory;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@Alternative
@Priority(1)
public class AdaptiveTracerFactory implements TracerFactory {

    private static AtomicReference<InMemoryReporter> REPORTER = new AtomicReference<>();

    public static InMemoryReporter getReporter() {
        return REPORTER.get();
    }

    @Override
    @Produces
    public synchronized Tracer getTracer() {
        JaegerTracer.Builder builder = Configuration.fromEnv().getTracerBuilder();

        InMemoryReporter reporter = REPORTER.get();
        if (reporter == null) {
            reporter = new SafeInMemoryReporter();
            REPORTER.compareAndSet(null, reporter);
        }
        builder.withReporter(reporter);

        builder.withTag("test.name", System.getProperty("test.name"));

        Tracer tracer = builder.build();
        CacheFactory.log("Initialized " + tracer.toString());
        return tracer;
    }

    protected static class SafeInMemoryReporter extends InMemoryReporter {
        @Override
        public synchronized List<JaegerSpan> getSpans() {
            List<JaegerSpan> spans = super.getSpans();
            int size = spans.size();
            List<JaegerSpan> copy = new ArrayList<>(size);
            for (int i = 0, len = spans.size(); i < len; i++) {
                copy.add(spans.get(i));
            }
            return copy;
        }
    }
}
