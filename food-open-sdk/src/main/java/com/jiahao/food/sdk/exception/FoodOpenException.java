package com.jiahao.food.sdk.exception;

/**
 * SDK 异常基类。
 */
public abstract class FoodOpenException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String errorMessage;

    protected FoodOpenException(String errorCode, String errorMessage, Throwable cause) {
        super("[" + errorCode + "] " + errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
