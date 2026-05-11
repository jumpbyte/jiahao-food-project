package com.jiahao.food.sdk.log;

/**
 * SDK 日志器：SLF4J 存在时使用，否则降级 JDK Logger。
 */
public class FoodOpenLogger {

    private final boolean enabled;
    private final Object delegate;
    private final boolean useSlf4j;

    public FoodOpenLogger(boolean enabled) {
        this.enabled = enabled;
        boolean slf4j = false;
        Object logger = null;
        try {
            Class<?> factoryClass = Class.forName("org.slf4j.LoggerFactory");
            java.lang.reflect.Method method = factoryClass.getMethod("getLogger", String.class);
            logger = method.invoke(null, "com.jiahao.food.sdk");
            slf4j = true;
        } catch (Exception e) {
            logger = java.util.logging.Logger.getLogger("com.jiahao.food.sdk");
        }
        this.delegate = logger;
        this.useSlf4j = slf4j;
    }

    public void info(String msg) {
        if (!enabled) return;
        if (useSlf4j) {
            ((org.slf4j.Logger) delegate).info(msg);
        } else {
            ((java.util.logging.Logger) delegate).info(msg);
        }
    }

    public void warn(String msg) {
        if (!enabled) return;
        if (useSlf4j) {
            ((org.slf4j.Logger) delegate).warn(msg);
        } else {
            ((java.util.logging.Logger) delegate).warning(msg);
        }
    }

    public void debug(String msg) {
        if (!enabled) return;
        if (useSlf4j) {
            ((org.slf4j.Logger) delegate).debug(msg);
        } else {
            ((java.util.logging.Logger) delegate).fine(msg);
        }
    }

    public void error(String msg, Throwable t) {
        if (!enabled) return;
        if (useSlf4j) {
            ((org.slf4j.Logger) delegate).error(msg, t);
        } else {
            ((java.util.logging.Logger) delegate).severe(msg + " - " + t.getMessage());
        }
    }
}
