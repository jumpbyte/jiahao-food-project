package com.jiahao.food.sdk.exception;

/**
 * 网络异常：连接超时、IO 异常。
 */
public class NetworkException extends FoodOpenException {

    public NetworkException(String message, Throwable cause) {
        super("NETWORK_ERROR", message, cause);
    }
}
