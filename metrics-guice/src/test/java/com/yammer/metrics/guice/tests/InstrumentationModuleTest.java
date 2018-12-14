package com.yammer.metrics.guice.tests;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.HealthChecks;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.HealthCheckRegistry;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.guice.InstrumentationModule;

public class InstrumentationModuleTest {
    private final InstrumentationModule module = new InstrumentationModule();
    private final Injector injector = Guice.createInjector(module);

    @Test
    public void defaultsToTheDefaultMetricsRegistry() throws Exception {
        assertThat(injector.getInstance(MetricsRegistry.class),
                   is(sameInstance(Metrics.defaultRegistry())));
    }

    @Test
    public void defaultsToTheDefaultHealthCheckRegistry() throws Exception {
        assertThat(injector.getInstance(HealthCheckRegistry.class),
                   is(sameInstance(HealthChecks.defaultRegistry())));
    }

    @Test
    public void moduleInstallIsIdempotent() {
        Injector injector = Guice.createInjector(new InstrumentationModule(), new InstrumentationModule());

        TestTimed instance = injector.getInstance(TestTimed.class);

        MetricName metricName = new MetricName(TestTimed.class, "method");
        Timer timer = (Timer) Metrics.defaultRegistry().allMetrics().get(metricName);

        assertThat(timer, is(notNullValue()));
        assertThat(timer.count(), is(0L));

        instance.method();

        assertThat(timer.count(), is(1L));
    }

    public static class TestTimed {

        @Timed
        public void method() {}
    }
}
