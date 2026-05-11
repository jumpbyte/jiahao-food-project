package com.jiahao.food.sdk.log;

/**
 * SDK 日志器：SLF4J 存在时使用，否则降级 JDK Logger。
 */
public class FoodOpenLogger {

    private static final boolean SLF4J_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("org.slf4j.LoggerFactory");
            available = true;
        } catch (ClassNotFoundException e) {
            // ignore
        }
        SLF4J_AVAILABLE = available;
    }

    private final boolean enabled;
    private final Object delegate;

    public FoodOpenLogger(boolean enabled) {
        this.enabled = enabled;
        if (SLF4J_AVAILABLE) {
            this.delegate = org.slf4j.LoggerFactory.getLogger("com.jiahao.food.sdk");
        } else {
            this.delegate = java.util.logging.Logger.getLogger("com.jiahao.food.sdk");
        }
    }

    public void info(String msg) {
        if (!enabled) return;
        if (SLF4J_AVAILABLE) {
            ((org.slf4j.Logger) delegate).info(msg);
        } else {
            ((java.util.logging.Logger) delegate).info(msg);
        }
    }

    public void warn(String msg) {
        if (!enabled) return;
        if (SLF4J_AVAILABLE) {
            ((org.slf4j.Logger) delegate).warn(msg);
        } else {
            ((java.util.logging.Logger) delegate).warning(msg);
        }
    }

    public void debug(String msg) {
        if (!enabled) return;
        if (SLF4J_AVAILABLE) {
            ((org.slf4j.Logger) delegate).debug(msg);
        } else {
            ((java.util.logging.Logger) delegate).fine(msg);
        }
    }

    public void error(String msg, Throwable t) {
        if (!enabled) return;
        if (SLF4J_AVAILABLE) {
            ((org.slf4j.Logger) delegate).error(msg, t);
        } else {
            ((java.util.logging.Logger) delegate).log(java.util.logging.Level.SEVERE, msg, t);
        }
    }
}
