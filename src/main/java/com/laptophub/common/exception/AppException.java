package com.laptophub.common.exception;

import com.laptophub.common.ErrorCode;

public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    // trả về lỗi mặc định của ErrorCode
    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    // trả về lỗi với thông báo tùy chỉnh
    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    // trả về lỗi với thông báo tùy chỉnh và nguyên nhân gốc
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
