package com.laptophub.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Đã được gắn mã lỗi và thông báo mặc định
    // Tức là khi truyền vào AppException chỉ cần truyền ErrorCode chấm tên lỗi
    // ví dụ: new AppException(ErrorCode.VALIDATION_ERROR)
    // Thay vì phải truyền mã lỗi và thông báo lỗi

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Chưa xác thực"),
    UNAUTHORIZED(HttpStatus.FORBIDDEN, "Không có quyền truy cập"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu đã tồn tại hoặc xung đột"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được đăng ký"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    ACCOUNT_BLOCKED(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Không đủ tồn kho khả dụng"),
    INVALID_STOCK_RECEIPT_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái phiếu nhập không hợp lệ cho thao tác này"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
