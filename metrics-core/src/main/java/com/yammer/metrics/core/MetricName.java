package com.yammer.metrics.core;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import javax.management.ObjectName;

/**
 * A value class encapsulating a metric's owning class and name.
 */
public class MetricName implements Comparable<MetricName> {
    private final String group;
    private final String type;
    private final String name;
    private final String scope;
    private final Optional<String> mBeanName;
    private final Map<String, String> tags;

    /**
     * Creates a new {@link MetricName} without a scope.
     *
     * @param klass the {@link Class} to which the {@link Metric} belongs
     * @param name  the name of the {@link Metric}
     */
    public MetricName(Class<?> klass, String name) {
        this(klass, name, (String) null);
    }

    /**
     * Creates a new {@link MetricName} without a scope.
     *
     * @param group the group to which the {@link Metric} belongs
     * @param type  the type to which the {@link Metric} belongs
     * @param name  the name of the {@link Metric}
     */
    public MetricName(String group, String type, String name) {
        this(group, type, name, (String) null);
    }

    /**
     * Creates a new {@link MetricName} without a scope.
     *
     * @param klass the {@link Class} to which the {@link Metric} belongs
     * @param name  the name of the {@link Metric}
     * @param scope the scope of the {@link Metric}
     */
    public MetricName(Class<?> klass, String name, String scope) {
        this(packageName(klass), className(klass), name, scope);
    }

    /**
     * Creates a new {@link MetricName} without a scope.
     *
     * @param group the group to which the {@link Metric} belongs
     * @param type  the type to which the {@link Metric} belongs
     * @param name  the name of the {@link Metric}
     * @param scope the scope of the {@link Metric}
     */
    public MetricName(String group, String type, String name, String scope) {
        this(group, type, name, scope, Collections.emptyMap());
    }

    /**
     * Creates a new {@link MetricName} without a scope.
     *
     * @param group     the group to which the {@link Metric} belongs
     * @param type      the type to which the {@link Metric} belongs
     * @param name      the name of the {@link Metric}
     * @param scope     the scope of the {@link Metric}
     * @param mBeanName the 'ObjectName', represented as a string, to use when registering the
     *                  MBean.
     */
    public MetricName(String group, String type, String name, String scope, String mBeanName) {
        this(group, type, name, scope, mBeanName, Collections.emptyMap());
    }

    public MetricName(Class<?> klass, String name, Map<String, String> tags) {
        this(klass, name, null, tags);
    }

    /**
     * Creates a new {@link MetricName} without a scope.
     *
     * @param group the group to which the {@link Metric} belongs
     * @param type  the type to which the {@link Metric} belongs
     * @param name  the name of the {@link Metric}
     */
    public MetricName(String group, String type, String name, Map<String, String> tags) {
        this(group, type, name, null, tags);
    }

    /**
     * Creates a new {@link MetricName} without a scope.
     *
     * @param klass the {@link Class} to which the {@link Metric} belongs
     * @param name  the name of the {@link Metric}
     * @param scope the scope of the {@link Metric}
     */
    public MetricName(Class<?> klass, String name, String scope, Map<String, String> tags) {
        this(packageName(klass), className(klass), name, scope, tags);
    }

    /**
     * Creates a new {@link MetricName} without a scope.
     *
     * @param group the group to which the {@link Metric} belongs
     * @param type  the type to which the {@link Metric} belongs
     * @param name  the name of the {@link Metric}
     * @param scope the scope of the {@link Metric}
     */
    public MetricName(String group, String type, String name, String scope, Map<String, String> tags) {
        this(group, type, name, scope, Optional.empty(), tags);
    }

    /**
     * Creates a new {@link MetricName} without a scope.
     *
     * @param group     the group to which the {@link Metric} belongs
     * @param type      the type to which the {@link Metric} belongs
     * @param name      the name of the {@link Metric}
     * @param scope     the scope of the {@link Metric}
     * @param mBeanName the 'ObjectName', represented as a string, to use when registering the
     *                  MBean.
     */
    public MetricName(String group, String type, String name, String scope, String mBeanName, Map<String, String> tags) {
        this(group, type, name, scope, Optional.of(mBeanName), tags);
    }

    private MetricName(String group, String type, String name, String scope, Optional<String> mBeanName, Map<String, String> tags) {
        if (group == null || type == null) {
            throw new IllegalArgumentException("Both group and type need to be specified");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name needs to be specified");
        }
        this.group = group;
        this.type = type;
        this.name = name;
        this.scope = scope;
        this.mBeanName = mBeanName;
        this.tags = new HashMap<>(tags);
    }

    /**
     * Returns the group to which the {@link Metric} belongs. For class-based metrics, this will be
     * the package name of the {@link Class} to which the {@link Metric} belongs.
     *
     * @return the group to which the {@link Metric} belongs
     */
    public String getGroup() {
        return group;
    }

    /**
     * Returns the type to which the {@link Metric} belongs. For class-based metrics, this will be
     * the simple class name of the {@link Class} to which the {@link Metric} belongs.
     *
     * @return the type to which the {@link Metric} belongs
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the name of the {@link Metric}.
     *
     * @return the name of the {@link Metric}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the scope of the {@link Metric}.
     *
     * @return the scope of the {@link Metric}
     */
    public String getScope() {
        return scope;
    }

    /**
     * Returns {@code true} if the {@link Metric} has a scope, {@code false} otherwise.
     *
     * @return {@code true} if the {@link Metric} has a scope
     */
    public boolean hasScope() {
        return scope != null;
    }

    /**
     * Returns the MBean name for the {@link Metric} identified by this metric name.
     *
     * @return the MBean name
     */
    public String getMBeanName() {
        return mBeanName.orElseGet(this::createMBeanName);
    }

    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final MetricName that = (MetricName) o;
        return Objects.equals(group, that.group) &&
            Objects.equals(type, that.type) &&
            Objects.equals(name, that.name) &&
            Objects.equals(scope, that.scope) &&
            Objects.equals(mBeanName, that.mBeanName) &&
            Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, type, name, scope, mBeanName, tags);
    }

    @Override
    public String toString() {
        return mBeanName.orElseGet(this::createMBeanName);
    }

    @Override
    public int compareTo(MetricName o) {
        return toString().compareTo(o.toString());
    }

    private String createMBeanName() {
        final StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(ObjectName.quote(group));
        nameBuilder.append(":type=");
        nameBuilder.append(ObjectName.quote(type));
        if (scope != null) {
            nameBuilder.append(",scope=");
            nameBuilder.append(ObjectName.quote(scope));
        }
        if (name.length() > 0) {
            nameBuilder.append(",name=");
            nameBuilder.append(ObjectName.quote(name));
        }
        for (Entry<String, String> tag : new TreeMap<>(tags).entrySet()) {
            nameBuilder.append(",").append(tag.getKey()).append("=");
            nameBuilder.append(ObjectName.quote(tag.getValue()));
        }
        return nameBuilder.toString();
    }

    /**
     * If the group is empty, use the package name of the given class. Otherwise use group
     * @param group The group to use by default
     * @param klass The class being tracked
     * @return a group for the metric
     */
    public static String chooseGroup(String group, Class<?> klass) {
        if(group == null || group.isEmpty()) {
            group = packageName(klass);
        }
        return group;
    }

    /**
     * If the type is empty, use the simple name of the given class. Otherwise use type
     * @param type The type to use by default
     * @param klass The class being tracked
     * @return a type for the metric
     */
    public static String chooseType(String type, Class<?> klass) {
        if(type == null || type.isEmpty()) {
            type = className(klass);
        }
        return type;
    }

    /**
     * If name is empty, use the name of the given method. Otherwise use name
     * @param name The name to use by default
     * @param method The method being tracked
     * @return a name for the metric
     */
    public static String chooseName(String name, Method method) {
        if(name == null || name.isEmpty()) {
            name = method.getName();
        }
        return name;
    }

    private static String packageName(Class<?> klass) {
        klass = unwrap(klass);
        return klass.getPackage() == null ? "" : klass.getPackage().getName();
    }

    private static String className(Class<?> klass) {
        return unwrap(klass).getSimpleName().replaceAll("\\$$", "");
    }

    private static Class<?> unwrap(Class<?> klass) {
        Class<?> unwrapped = klass;
        while (unwrapped.getName().contains("$EnhancerByGuice$") && unwrapped.getSuperclass() != null) {
            unwrapped = unwrapped.getSuperclass();
        }

        return unwrapped;
    }
}
