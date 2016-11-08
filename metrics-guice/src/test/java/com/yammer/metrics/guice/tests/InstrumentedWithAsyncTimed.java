package com.yammer.metrics.guice.tests;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.yammer.metrics.annotation.AsyncTimed;

public class InstrumentedWithAsyncTimed {
  @AsyncTimed(name = "things", rateUnit = TimeUnit.MINUTES, durationUnit = TimeUnit.MICROSECONDS)
  public CompletableFuture<String> doAThing() {
    return CompletableFuture.supplyAsync(() -> "poop");
  }

  @AsyncTimed
  CompletableFuture<String> doAThingWithDefaultScope() {
    return CompletableFuture.supplyAsync(() -> "defaultResult");
  }

  @AsyncTimed
  protected CompletableFuture<String> doAThingWithProtectedScope() {
    return CompletableFuture.supplyAsync(() -> "defaultProtected");
  }

  @AsyncTimed(group="g", type="t", name="n")
  protected CompletableFuture<String> doAThingWithCustomGroupTypeAndName() {
    return CompletableFuture.supplyAsync(() -> "defaultProtected");
  }

  @AsyncTimed(group="g", type="t", name="synchronous")
  public String doASynchronousThing() {
    return "synchronous";
  }

  @AsyncTimed(group="g", type="t", name="slow")
  public CompletableFuture<String> doASlowThing() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignored) {}
      return "slow";
    });
  }
}
