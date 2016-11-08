package com.yammer.metrics.guice.tests;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.guice.InstrumentationModule;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class AsyncTimedTest {
  private InstrumentedWithAsyncTimed instance;
  private MetricsRegistry registry;

  @Before
  public void setup() {
    this.registry = new MetricsRegistry();
    final Injector injector = Guice.createInjector(new InstrumentationModule() {
      @Override
      protected MetricsRegistry createMetricsRegistry() {
        return registry;
      }
    });
    this.instance = injector.getInstance(InstrumentedWithAsyncTimed.class);
  }

  @After
  public void tearDown() throws Exception {
    registry.shutdown();
  }

  @Test
  public void anAsyncTimedAnnotatedMethod() throws Exception {

    instance.doAThing().join();

    final Metric metric = registry.allMetrics().get(new MetricName(InstrumentedWithAsyncTimed.class,
        "things"));

    assertMetricSetup(metric);

    assertThat("Guice creates a timer which records invocation length",
        ((Timer) metric).count(),
        is(1L));

    assertThat("Guice creates a timer with the given rate unit",
        ((Timer) metric).rateUnit(),
        is(TimeUnit.MINUTES));

    assertThat("Guice creates a timer with the given duration unit",
        ((Timer) metric).durationUnit(),
        is(TimeUnit.MICROSECONDS));
  }

  @Test
  public void anAsyncTimedAnnotatedMethodWithDefaultScope() throws Exception {

    instance.doAThing().join();

    final Metric metric = registry.allMetrics().get(new MetricName(InstrumentedWithAsyncTimed.class,
        "doAThingWithDefaultScope"));

    assertMetricSetup(metric);
  }

  @Test
  public void anAsyncTimedAnnotatedMethodWithProtectedScope() throws Exception {

    instance.doAThing().join();

    final Metric metric = registry.allMetrics().get(new MetricName(InstrumentedWithAsyncTimed.class,
        "doAThingWithProtectedScope"));

    assertMetricSetup(metric);
  }

  @Test
  public void anAsyncTimedAnnotatedMethodWithCustomGroupTypeAndName() throws Exception {

    instance.doAThingWithCustomGroupTypeAndName().join();

    final Metric metric = registry.allMetrics().get(new MetricName("g", "t", "n"));

    assertMetricSetup(metric);
  }

  @Test
  public void anAsyncTimedAnnotatedSynchronousMethod() {

    instance.doASynchronousThing();

    final Metric metric = registry.allMetrics().get(new MetricName("g", "t", "synchronous"));

    assertThat("Guice does not create a metric", metric, is(nullValue()));
  }

  @Test
  public void anAsyncTimedAnnotatedSlowMethod() {

    instance.doASlowThing().join();

    final Metric metric = registry.allMetrics().get(new MetricName("g", "t", "slow"));

    assertMetricSetup(metric);

    assertThat("Guice creates a timer which records invocation length",
        ((Timer) metric).count(),
        is(1L));

    assertThat("Guice creates a timer with the given rate unit",
        ((Timer) metric).min(),
        greaterThanOrEqualTo(100d));
  }

  private void assertMetricSetup(final Metric metric) {
    assertThat("Guice creates a metric",
        metric,
        is(notNullValue()));

    assertThat("Guice creates a timer",
        metric,
        is(instanceOf(Timer.class)));
  }
}
