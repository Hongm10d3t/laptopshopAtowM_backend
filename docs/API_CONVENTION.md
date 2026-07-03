# API Convention — LaptopHub Backend

## Base URL

Tất cả API có prefix `/api/v1` (cấu hình qua `server.servlet.context-path`).

Ví dụ: `GET /api/v1/products`

## Response format

Mọi response (thành công lẫn lỗi) đều bọc trong `ApiResponse<T>` (`com.laptophub.common.ApiResponse`):

```json
{
  "success": true,
  "message": "OK",
  "data": { },
  "timestamp": "2026-07-02T02:30:00Z"
}
```

Khi lỗi:

```json
{
  "success": false,
  "message": "Không tìm thấy dữ liệu",
  "errorCode": "RESOURCE_NOT_FOUND",
  "timestamp": "2026-07-02T02:30:00Z"
}
```

`data` và `errorCode` bị ẩn khỏi JSON khi null (`@JsonInclude(NON_NULL)`).

## Error handling

Toàn bộ exception được xử lý tập trung tại `com.laptophub.common.exception.GlobalExceptionHandler`:

| Exception | HTTP Status | errorCode |
|---|---|---|
| `AppException` (business exception, ném theo `ErrorCode`) | tùy `ErrorCode` | tên `ErrorCode` |
| `MethodArgumentNotValidException` (lỗi `@Valid` trên body) | 400 | `VALIDATION_ERROR` |
| `ConstraintViolationException` (lỗi `@Validated` trên param/path) | 400 | `VALIDATION_ERROR` |
| `Exception` khác (fallback) | 500 | `INTERNAL_ERROR` |

Các `ErrorCode` hiện có (`com.laptophub.common.ErrorCode`): `VALIDATION_ERROR`, `BAD_REQUEST`, `UNAUTHENTICATED`, `UNAUTHORIZED`, `RESOURCE_NOT_FOUND`, `RESOURCE_CONFLICT`, `INTERNAL_ERROR`. Bổ sung thêm khi nghiệp vụ cần.

## Phân trang

API trả danh sách phân trang bọc `PageResponse<T>` (`com.laptophub.common.dto.PageResponse`) vào trong `data` của `ApiResponse`:

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "content": [ ],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "last": false
  },
  "timestamp": "2026-07-03T02:30:00Z"
}
```

`page` bắt đầu từ 0, khớp `Pageable`/`Page` của Spring Data. Tạo qua `PageResponse.of(page)`.

## Health check

`GET /api/v1/health` — không cần xác thực, dùng để kiểm tra server đang chạy.

```json
{
  "success": true,
  "message": "OK",
  "data": { "status": "UP", "service": "laptophub-backend", "timestamp": "..." },
  "timestamp": "2026-07-03T02:30:00Z"
}
```

## Quy ước khác

- Không trả Entity trực tiếp — mọi request/response dùng DTO riêng theo module.
- Tiền tệ dùng `BigDecimal`.
- Nghiệp vụ ghi nhiều bảng dùng `@Transactional`.
- Xóa dữ liệu quan trọng dùng soft-delete qua status `ACTIVE`/`INACTIVE`, không xóa vật lý.

## Lịch sử thay đổi

- 2026-07-02: Khởi tạo convention — response format, error handling, base URL.
- 2026-07-03: Bổ sung quy ước phân trang (`PageResponse<T>`) và `GET /health`.
