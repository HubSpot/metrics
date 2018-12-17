package com.yammer.metrics.guice;

import java.util.Objects;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import com.yammer.metrics.HealthChecks;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.HealthCheckRegistry;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.reporting.JmxReporter;

/**
 * A Guice module which instruments methods annotated with the {@link com.yammer.metrics.annotation.Metered} and {@link com.yammer.metrics.annotation.Timed}
 * annotations.
 *
 * @see com.yammer.metrics.annotation.Gauge
 * @see com.yammer.metrics.annotation.Metered
 * @see com.yammer.metrics.annotation.Timed
 * @see MeteredInterceptor
 * @see TimedInterceptor
 * @see GaugeInjectionListener
 */
public class InstrumentationModule extends AbstractModule {
    private final HealthCheckRegistry healthCheckRegistry = createHealthCheckRegistry();
    private final MetricsRegistry metricsRegistry = createMetricsRegistry();

    @Override
    protected void configure() {
        bind(MetricsRegistry.class).toInstance(metricsRegistry);
        bind(HealthCheckRegistry.class).toInstance(healthCheckRegistry);
        bindJmxReporter();
        bindListener(Matchers.any(), new MeteredListener(metricsRegistry));
        bindListener(Matchers.any(), new TimedListener(metricsRegistry));
        bindListener(Matchers.any(), new GaugeListener(metricsRegistry));
        bindListener(Matchers.any(), new ExceptionMeteredListener(metricsRegistry));
    }

    /**
     * Override to provide a custom binding for {@link JmxReporter}
     */
    protected void bindJmxReporter() {
        bind(JmxReporter.class).toProvider(JmxReporterProvider.class).in(Scopes.SINGLETON);
    }

    /**
     * Override to provide a custom {@link HealthCheckRegistry}
     */
    protected HealthCheckRegistry createHealthCheckRegistry() {
        return HealthChecks.defaultRegistry();
    }

    /**
     * Override to provide a custom {@link MetricsRegistry}
     */
    protected MetricsRegistry createMetricsRegistry() {
        return Metrics.defaultRegistry();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InstrumentationModule that = (InstrumentationModule) o;
        return Objects.equals(healthCheckRegistry, that.healthCheckRegistry) &&
            Objects.equals(metricsRegistry, that.metricsRegistry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(healthCheckRegistry, metricsRegistry);
    }
}
