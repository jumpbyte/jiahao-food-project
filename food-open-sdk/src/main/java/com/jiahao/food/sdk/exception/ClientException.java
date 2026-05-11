package com.jiahao.food.sdk.exception;

/**
 * 客户端异常：参数错误、配置错误。
 */
public class ClientException extends FoodOpenException {

    public ClientException(String message) {
        super("CLIENT_ERROR", message, null);
    }
}
