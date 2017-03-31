package com.yammer.metrics.core;

import java.util.concurrent.atomic.AtomicReference;

class StoppableHistogram extends Histogram implements Stoppable {
  private AtomicReference<Stoppable> stoppable;

  StoppableHistogram(SampleType type) {
    super(type);
    stoppable = new AtomicReference<>(() -> {});
  }

  void setStoppable(Stoppable stoppable) {
    this.stoppable.set(stoppable);
  }

  @Override
  public void stop() {
    stoppable.get().stop();
  }
}
