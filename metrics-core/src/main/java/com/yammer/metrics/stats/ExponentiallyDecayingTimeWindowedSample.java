package com.yammer.metrics.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.yammer.metrics.core.Clock;

public class ExponentiallyDecayingTimeWindowedSample implements Sample {
  private static final int SAMPLE_COUNT = 4;
  private static final long ROTATE_INTERVAL = TimeUnit.MINUTES.toNanos(1);

  private final ExponentiallyDecayingSample[] allSamples;
  private final AtomicInteger activeSampleIndex;
  private final Clock clock;
  private final AtomicLong rotateAt;

  public ExponentiallyDecayingTimeWindowedSample() {
    this.allSamples = new ExponentiallyDecayingSample[SAMPLE_COUNT];
    for (int i = 0; i < allSamples.length; i++) {
      allSamples[i] = new ExponentiallyDecayingSample(257, 0.015);
    }
    this.activeSampleIndex = new AtomicInteger(0);
    this.clock = Clock.defaultClock();
    this.rotateAt = new AtomicLong(nextRotateTime());
  }

  @Override
  public void clear() {
    for (ExponentiallyDecayingSample sample : allSamples) {
      sample.clear();
    }
  }

  @Override
  public int size() {
    rotateSamplesIfNeeded();

    int size = 0;
    for (ExponentiallyDecayingSample sample : allSamples) {
      size += sample.size();
    }
    return size;
  }

  @Override
  public void update(long value) {
    rotateSamplesIfNeeded();

    allSamples[activeSampleIndex.get()].update(value);
  }

  @Override
  public Snapshot getSnapshot() {
    rotateSamplesIfNeeded();

    List<Long> values = new ArrayList<Long>();

    for (ExponentiallyDecayingSample sample : allSamples) {
      values.addAll(sample.getValues());
    }

    return new Snapshot(values);
  }

  private void rotateSamplesIfNeeded() {
    long now = clock.tick();
    long rotateAt = this.rotateAt.get();

    if (now >= rotateAt && this.rotateAt.compareAndSet(rotateAt, now + ROTATE_INTERVAL)) {
      long samplesToClear = Math.min(1 + ((now - rotateAt) / ROTATE_INTERVAL), SAMPLE_COUNT);

      int nextIndex = index(activeSampleIndex.get() + 1);
      for (int i = 0; i < samplesToClear; i++) {
        int indexToClear = index(nextIndex + i);
        allSamples[indexToClear].clear();
      }
      activeSampleIndex.set(nextIndex);
    }
  }

  private int index(int i ) {
    return i % SAMPLE_COUNT;
  }

  private long nextRotateTime() {
    return clock.tick() + ROTATE_INTERVAL;
  }
}
