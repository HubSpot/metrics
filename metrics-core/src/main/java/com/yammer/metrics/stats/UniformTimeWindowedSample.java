package com.yammer.metrics.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.yammer.metrics.core.Clock;

public class UniformTimeWindowedSample implements Sample {
  private static final long ROTATE_INTERVAL = TimeUnit.MINUTES.toNanos(1);

  private final UniformSample[] allSamples;
  private final AtomicInteger activeSampleIndex;
  private final Clock clock;
  private final AtomicLong rotateAt;

  public UniformTimeWindowedSample(int reservoirSize, int sampleCount) {
    this(reservoirSize, sampleCount, Clock.defaultClock());
  }

  public UniformTimeWindowedSample(int reservoirSize, int sampleCount, Clock clock) {
    this.allSamples = createSamples(reservoirSize, sampleCount);
    this.activeSampleIndex = new AtomicInteger(0);
    this.clock = clock;
    this.rotateAt = new AtomicLong(clock.tick() + ROTATE_INTERVAL);
  }

  @Override
  public void clear() {
    for (UniformSample sample : allSamples) {
      sample.clear();
    }
  }

  @Override
  public int size() {
    rotateSamplesIfNeeded();

    int size = 0;
    for (UniformSample sample : allSamples) {
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

    for (UniformSample sample : allSamples) {
      values.addAll(sample.getValues());
    }

    return new Snapshot(values);
  }

  private static UniformSample[] createSamples(int reservoirSize, int sampleCount) {
    int individualSize = reservoirSize / sampleCount;
    UniformSample[] samples = new UniformSample[sampleCount];
    for (int i = 0; i < samples.length; i++) {
      samples[i] = new UniformSample(individualSize);
    }

    return samples;
  }

  private void rotateSamplesIfNeeded() {
    long now = clock.tick();
    long rotateAt = this.rotateAt.get();

    if (now >= rotateAt && this.rotateAt.compareAndSet(rotateAt, now + ROTATE_INTERVAL)) {
      long samplesToClear = Math.min(1 + ((now - rotateAt) / ROTATE_INTERVAL), allSamples.length);

      int nextIndex = index(activeSampleIndex.get() + 1);
      for (int i = 0; i < samplesToClear; i++) {
        int indexToClear = index(nextIndex + i);
        allSamples[indexToClear].clear();
      }
      activeSampleIndex.set(nextIndex);
    }
  }

  private int index(int i) {
    return i % allSamples.length;
  }
}
