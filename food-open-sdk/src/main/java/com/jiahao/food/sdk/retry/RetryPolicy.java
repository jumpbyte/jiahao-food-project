package com.jiahao.food.sdk.retry;

import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.exception.NetworkException;
import com.jiahao.food.sdk.log.FoodOpenLogger;

/**
 * 重试策略：指数退避，仅对 NetworkException 重试。
 */
public class RetryPolicy {

    private final int maxRetries;
    private final FoodOpenLogger logger;

    public RetryPolicy(int maxRetries, FoodOpenLogger logger) {
        this.maxRetries = maxRetries;
        this.logger = logger;
    }

    /**
     * 执行可重试操作。指数退避：200ms, 400ms, 800ms...
     */
    public <T> T execute(Callable<T> callable) throws FoodOpenException {
        int attempt = 0;
        while (true) {
            try {
                return callable.call();
            } catch (NetworkException e) {
                if (attempt >= maxRetries) {
                    throw e;
                }
                long delay = 200L * (1L << attempt);
                logger.warn("Retry attempt " + (attempt + 1) + "/" + maxRetries + " after " + delay + "ms");
                sleep(delay);
                attempt++;
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Java 1.8 兼容的 Callable 接口。
     */
    public interface Callable<T> {
        T call() throws FoodOpenException;
    }
}
