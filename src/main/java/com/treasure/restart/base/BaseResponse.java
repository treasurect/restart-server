package com.treasure.restart.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {

    private Integer code;

    private String msg;

    private T data;

    public static <T> BaseResponse<T> success(String msg) {
        return success(msg, null);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(200, message, data);
    }

    public static <T> BaseResponse<T> error(String message) {
        return error(-1, message);
    }

    public static <T> BaseResponse<T> error(Integer code, String message) {
        return new BaseResponse<>(code, message, null);
    }
}
