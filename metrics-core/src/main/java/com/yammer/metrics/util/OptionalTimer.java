package com.yammer.metrics.util;

import java.io.Closeable;
import java.util.Optional;

import com.yammer.metrics.core.Timer;

public class OptionalTimer {
  private static final Closeable NULL_CLOSEABLE = () -> {};
  private static final OptionalTimer EMPTY = new OptionalTimer();
  private final Optional<Timer> timer;

  private OptionalTimer(Timer timer) {
    this.timer = Optional.of(timer);
  }

  private OptionalTimer() {
    this.timer = Optional.empty();
  }

  public Closeable time() {
    return timer.<Closeable>map(Timer::time).orElse(NULL_CLOSEABLE);
  }

  public static OptionalTimer of(Timer timer) {
    return new OptionalTimer(timer);
  }

  public static OptionalTimer of(Optional<Timer> timer) {
    return timer.map(OptionalTimer::new).orElse(EMPTY);
  }

  public static OptionalTimer empty() {
    return EMPTY;
  }
}
