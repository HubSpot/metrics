package com.yammer.metrics.guice;

import com.yammer.metrics.annotation.AsyncTimed;
import com.yammer.metrics.annotation.Timed;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A method interceptor which creates a timer for the declaring class with the given name (or the
 * method's name, if none was provided), and which times the execution of the annotated method.
 */
class TimedInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimedInterceptor.class);

    static MethodInterceptor forMethod(MetricsRegistry metricsRegistry, Class<?> klass, Method method) {
        MethodInterceptor interceptor = forMethodSync(metricsRegistry, klass, method);
        return interceptor == null ? forMethodAsync(metricsRegistry, klass, method) : interceptor;
    }

    private static MethodInterceptor forMethodSync(MetricsRegistry metricsRegistry, Class<?> klass, Method method) {
        final Timed annotation = method.getAnnotation(Timed.class);
        if (annotation != null) {
            final String group = MetricName.chooseGroup(annotation.group(), klass);
            final String type = MetricName.chooseType(annotation.type(), klass);
            final String name = MetricName.chooseName(annotation.name(), method);
            final MetricName metricName = new MetricName(group, type, name);
            final Timer timer = metricsRegistry.newTimer(metricName,
                                                               annotation.durationUnit(),
                                                               annotation.rateUnit());
            return invocation -> {
                final TimerContext ctx = timer.time();
                try {
                    return invocation.proceed();
                } finally {
                    ctx.stop();
                }
            };
        }
        return null;
    }

    private static MethodInterceptor forMethodAsync(MetricsRegistry metricsRegistry, Class<?> klass, Method method) {
        final AsyncTimed annotation = method.getAnnotation(AsyncTimed.class);
        if (annotation != null) {
            Class<?> returnType = method.getReturnType();
            if (returnType == CompletableFuture.class) {
                final String group = MetricName.chooseGroup(annotation.group(), klass);
                final String type = MetricName.chooseType(annotation.type(), klass);
                final String name = MetricName.chooseName(annotation.name(), method);
                final MetricName metricName = new MetricName(group, type, name);
                final Timer timer = metricsRegistry.newTimer(metricName,
                                                               annotation.durationUnit(),
                                                               annotation.rateUnit());
                return invocation -> {
                    final TimerContext ctx = timer.time();
                    final CompletableFuture<?> future;
                    try {
                        future = (CompletableFuture<?>) invocation.proceed();
                    } catch (Throwable t) {
                        ctx.stop();
                        throw t;
                    }

                    if (future == null) {
                        ctx.stop();
                    } else {
                        future.whenComplete((result, exception) -> ctx.stop());
                    }

                    return future;
                };
            } else {
                LOGGER.warn("Method " + method + " is annotated with @AsyncTimed but does not return a CompletableFuture");
            }
        }
        return null;
    }
}
