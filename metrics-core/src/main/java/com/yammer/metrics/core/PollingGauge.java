package com.yammer.metrics.core;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gauge subclass that rather than reporting a single value at each report period, it reads the value at a
 * configurable poll interval and updates a histogram to give a better sense of the distribution of values over time
 */
class PollingGauge<T extends Number> extends Gauge<T> implements Stoppable {
  private final Gauge<T> delegate;
  private final ScheduledFuture<?> future;

  private PollingGauge(final Gauge<T> delegate, final Histogram histogram, ScheduledExecutorService pollThread, long pollInterval, TimeUnit pollIntervalUnit) {
    this.delegate = delegate;
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

  static <T extends Number> void poll(Gauge<T> delegate,
                                      Histogram histogram, ScheduledExecutorService pollThread,
                                      long pollInterval,
                                      TimeUnit pollIntervalUnit) {
    new PollingGauge<T>(delegate, histogram, pollThread, pollInterval, pollIntervalUnit);
  }

  @Override
  public T value() {
    return delegate.value();
  }

  @Override
  public void stop() {
    future.cancel(false);
  }
}
