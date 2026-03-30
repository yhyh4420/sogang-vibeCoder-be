package com.k.medtour.global.common;

import com.k.medtour.global.exception.ErrorCode;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ErrorDetail error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getMessage(), null,
                new ErrorDetail(errorCode.getCode(), errorCode.getMessage()));
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, message, null,
                new ErrorDetail(errorCode.getCode(), message));
    }

    public record ErrorDetail(String code, String message) {
    }
}
