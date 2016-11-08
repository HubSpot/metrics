package com.yammer.metrics.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * An annotation for marking an async method of a Guice-provided object as timed.
 * <p/>
 * Given a method like this:
 * <pre><code>
 *     \@AsyncTimed(name = "fancyName", rateUnit = TimeUnit.SECONDS, durationUnit =
 * TimeUnit.MICROSECONDS)
 *     public CompletableFuture<String> fancyName(String name) {
 *         return CompletableFuture.supplyAsync(() -> "Sir Captain " + name);
 *     }
 * </code></pre>
 * <p/>
 * A timer for the defining class with the name {@code fancyName} will be created and each time the
 * {@code #fancyName(String)} method is invoked, the method's execution will be timed. However, the timer
 * won't be stopped until the returned future resolves.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AsyncTimed {
  /**
   * The group of the timer.
   */
  String group() default "";

  /**
   * The type of the timer.
   */
  String type() default "";

  /**
   * The name of the timer.
   */
  String name() default "";

  /**
   * The time unit of the timer's rate.
   */
  TimeUnit rateUnit() default TimeUnit.SECONDS;

  /**
   * The time unit of the timer's duration.
   */
  TimeUnit durationUnit() default TimeUnit.MILLISECONDS;
}
