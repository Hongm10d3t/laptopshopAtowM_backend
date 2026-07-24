# API Convention — LaptopHub

## 1. Base path

API dùng prefix:

```text
/api/v1
```

Nhóm endpoint khuyến nghị:

```text
/api/v1/auth/**
/api/v1/public/**
/api/v1/customer/**
/api/v1/admin/**
```

Tên endpoint có thể điều chỉnh theo module, nhưng cần nhất quán và dùng danh từ số nhiều.

## 2. Response chung

Thành công:

```json
{
  "success": true,
  "message": "OK",
  "data": {},
  "timestamp": "2026-07-15T18:00:00Z"
}
```

Thất bại:

```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-07-15T18:00:00Z"
}
```

Có thể bỏ các field null. Không trả stack trace hoặc thông tin nội bộ cho client.

## 3. HTTP method và status

- `GET`: đọc dữ liệu — `200`.
- `POST`: tạo mới hoặc thực hiện action — `201` hoặc `200`.
- `PUT/PATCH`: cập nhật — `200`.
- `DELETE`: chỉ dùng khi thực sự xóa; dữ liệu nghiệp vụ thường đổi status — `204` hoặc `200`.
- Validation sai — `400`.
- Chưa đăng nhập — `401`.
- Không đủ quyền — `403`.
- Không tìm thấy — `404`.
- Xung đột dữ liệu — `409`.
- Lỗi hệ thống — `500`.

## 4. DTO và validation

- Request/response dùng DTO riêng; không expose Entity.
- Request dùng Bean Validation như `@NotBlank`, `@NotNull`, `@Positive` khi phù hợp.
- Service vẫn phải kiểm tra các điều kiện nghiệp vụ như tồn kho, trạng thái đơn, quyền sở hữu và voucher.
- Không tin giá, tổng tiền, role hoặc trạng thái do frontend gửi lên.

## 5. Phân trang, lọc và sắp xếp

Query gợi ý:

```text
?page=0&size=20&sort=createdAt,desc
```

Response phân trang:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

- `page` bắt đầu từ `0`.
- Có giới hạn `size` hợp lý để tránh query quá lớn.
- Filter chỉ thêm khi có nhu cầu thật; tránh tạo một API với quá nhiều tham số khó bảo trì.

## 6. Error code

Dùng error code ổn định, ví dụ:

```text
VALIDATION_ERROR
UNAUTHENTICATED
UNAUTHORIZED
RESOURCE_NOT_FOUND
RESOURCE_CONFLICT
INSUFFICIENT_STOCK
INVALID_STOCK_RECEIPT_STATUS
PRODUCT_VARIANT_UNAVAILABLE
INVALID_ORDER_STATUS
VOUCHER_NOT_APPLICABLE
PAYMENT_FAILED
INTERNAL_ERROR
```

Có thể bổ sung theo module. Message dành cho người dùng; error code dành cho frontend xử lý.

Danh sách endpoint, quyền truy cập, HTTP status và error code riêng cho
`auth`/`security`/`user` (đăng ký, đăng nhập, refresh, logout...) đã chốt ở
`AUTH_SECURITY_USER_CONTRACT.md` (gói ASU-00) — không lặp lại chi tiết đó ở đây.

## 7. Security

- API public chỉ gồm nội dung cần công khai.
- API Customer yêu cầu đăng nhập và kiểm tra quyền sở hữu tài nguyên.
- API Admin yêu cầu role Admin.
- Không dựa vào việc frontend ẩn nút để bảo vệ API.

Ví dụ:

```java
@PreAuthorize("hasRole('ADMIN')")
```

## 8. Transaction và idempotency

Các action sau phải chạy transaction khi cập nhật nhiều dữ liệu:

- Tạo đơn và giữ tồn.
- Hủy đơn và giải phóng tồn.
- Xuất đơn.
- Xác nhận phiếu nhập.
- Áp dụng/ghi nhận voucher.
- Xử lý callback thanh toán.

Callback thanh toán và action nhạy cảm nên có cơ chế chống xử lý lặp khi cần.

## 9. Quy ước action endpoint

Khi action không phù hợp với CRUD thuần, có thể dùng endpoint rõ nghĩa:

```text
POST /admin/stock-receipts/{id}/confirm
POST /admin/orders/{id}/confirm
POST /admin/orders/{id}/ship
POST /customer/orders/{id}/cancel
POST /customer/orders/{id}/return-requests
```

Không bắt buộc bám cứng ví dụ trên, nhưng tên action phải phản ánh đúng nghiệp vụ.

## 10. Tài liệu API

- Dùng OpenAPI/Swagger nếu thuận tiện.
- Mỗi endpoint cần mô tả request, quyền truy cập, lỗi nghiệp vụ chính và response mẫu.
- Khi thay đổi contract đáng kể, cập nhật tài liệu cùng pull request/commit.
