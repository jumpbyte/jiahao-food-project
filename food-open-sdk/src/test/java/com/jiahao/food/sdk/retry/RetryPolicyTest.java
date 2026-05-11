package com.jiahao.food.sdk.retry;

import com.jiahao.food.sdk.exception.FoodOpenException;
import com.jiahao.food.sdk.exception.NetworkException;
import com.jiahao.food.sdk.exception.ServerException;
import com.jiahao.food.sdk.log.FoodOpenLogger;
import org.junit.Test;

import static org.junit.Assert.*;

public class RetryPolicyTest {

    private final FoodOpenLogger logger = new FoodOpenLogger(false);

    @Test
    public void execute_onSuccess_shouldReturnValue() throws FoodOpenException {
        RetryPolicy policy = new RetryPolicy(3, logger);
        String result = policy.execute(new RetryPolicy.Callable<String>() {
            public String call() { return "ok"; }
        });
        assertEquals("ok", result);
    }

    @Test(expected = ServerException.class)
    public void execute_onServerException_shouldNotRetry() throws FoodOpenException {
        RetryPolicy policy = new RetryPolicy(3, logger);
        final int[] calls = new int[1];
        policy.execute(new RetryPolicy.Callable<String>() {
            public String call() throws FoodOpenException {
                calls[0]++;
                throw new ServerException(500, "error");
            }
        });
        assertEquals(1, calls[0]);
    }

    @Test
    public void execute_onNetworkException_shouldRetryThenSucceed() throws FoodOpenException {
        RetryPolicy policy = new RetryPolicy(3, logger);
        final int[] calls = new int[1];
        String result = policy.execute(new RetryPolicy.Callable<String>() {
            public String call() throws FoodOpenException {
                calls[0]++;
                if (calls[0] < 3) {
                    throw new NetworkException("timeout", null);
                }
                return "recovered";
            }
        });
        assertEquals("recovered", result);
        assertEquals(3, calls[0]);
    }

    @Test(expected = NetworkException.class)
    public void execute_onPersistentNetworkException_shouldExhaustRetries() throws FoodOpenException {
        RetryPolicy policy = new RetryPolicy(2, logger);
        policy.execute(new RetryPolicy.Callable<String>() {
            public String call() throws FoodOpenException {
                throw new NetworkException("persistent failure", null);
            }
        });
    }
}
