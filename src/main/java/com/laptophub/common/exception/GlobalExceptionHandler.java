package com.laptophub.common.exception;

import com.laptophub.common.ApiResponse;
import com.laptophub.common.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Phải khai riêng, không để NoResourceFoundException rơi vào
    // handleUnexpected() bên dưới — nếu không, mọi URL không tồn tại
    // (route sai, bot dò quét...) sẽ trả 500 "Lỗi hệ thống" thay vì 404
    // bình thường, gây nhiễu log/alert không cần thiết.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND.name(), ErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage()));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.name(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(), message));
    }

    // Phát sinh khi client truyền ?sort= với tên field không tồn tại trên
    // entity (Admin category/brand/product list dùng Pageable/Sort trực
    // tiếp) — không để rơi vào handleUnexpected() (500), đây là lỗi input
    // của client (400), không phải lỗi hệ thống.
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<Void>> handlePropertyReference(PropertyReferenceException ex) {
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.getDefaultMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
