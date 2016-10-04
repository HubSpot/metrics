package com.yammer.metrics.core;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.yammer.metrics.core.Histogram.SampleType;

/**
 * Gauge subclass that rather than reporting a single value at each report period, it reads the value at a
 * configurable poll interval and updates a histogram to give a better sense of the distribution of values over time
 */
class PollingGauge<T extends Number> extends Gauge<T> implements Stoppable {
  private final Gauge<T> delegate;
  private final Histogram histogram;
  private final ScheduledFuture<?> future;

  PollingGauge(final Gauge<T> delegate, ScheduledExecutorService pollThread, long pollInterval, TimeUnit pollIntervalUnit) {
    this.delegate = delegate;
    this.histogram = new Histogram(SampleType.BIASED);
    this.future = pollThread.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        final long value;
        try {
          value = delegate.value().longValue();
        } catch (Throwable ignored) {
          return;
        }

        histogram.update(value);
      }
    }, pollInterval, pollInterval, pollIntervalUnit);
  }

  @Override
  public T value() {
    return delegate.value();
  }

  @Override
  public <U> void processWith(MetricProcessor<U> processor, MetricName name, U context) throws Exception {
    processor.processHistogram(name, histogram, context);
  }

  @Override
  public void stop() {
    future.cancel(false);
  }
}
