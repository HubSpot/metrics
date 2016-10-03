package com.yammer.metrics.jetty;

import java.util.concurrent.TimeUnit;

/**
 * A RequestDurationTracker is used to track queue and request
 * times as they occur.
 * DO NOT BLOCK IN ConnectionMetricTracker CALLS AS THEY WILL BLOCK THE REQUEST THREAD
 */
public interface RequestDurationTracker {
  void acceptQueueTime(long time, TimeUnit timeUnit);
  void acceptRequestAndQueueTime(long time, TimeUnit timeUnit);
}
