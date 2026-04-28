package com.jihao.food.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;
    private String traceId;

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data, System.currentTimeMillis(), null);
    }

    public static <T> Result<T> success(T data, String traceId) {
        return new Result<>(0, "success", data, System.currentTimeMillis(), traceId);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis(), null);
    }

    public static <T> Result<T> error(int code, String message, String traceId) {
        return new Result<>(code, message, null, System.currentTimeMillis(), traceId);
    }
}
