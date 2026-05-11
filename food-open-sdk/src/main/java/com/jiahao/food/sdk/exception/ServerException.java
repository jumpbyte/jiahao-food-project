package com.jiahao.food.sdk.exception;

/**
 * 服务端异常：API 返回 code != 0。
 */
public class ServerException extends FoodOpenException {

    private final int statusCode;

    public ServerException(int statusCode, String message) {
        super(String.valueOf(statusCode), message, null);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
