# Quy tắc dự án — LaptopHub Backend

Tài liệu này chốt lại các ràng buộc kiến trúc và quy tắc làm việc đã thống nhất, để mọi giai đoạn sau đều tuân theo nhất quán mà không cần lặp lại yêu cầu mỗi lần.

## Bối cảnh nghiệp vụ

LaptopHub là website bán laptop và phụ kiện máy tính, có 3 actor:

- **Guest** — khách vãng lai: xem, tìm kiếm, lọc sản phẩm.
- **Customer** — khách đã đăng nhập: đăng ký/đăng nhập, giỏ hàng, đặt hàng, áp voucher, thanh toán COD/chuyển khoản, xem/hủy đơn, đánh giá sản phẩm.
- **Admin** — quản trị: khách hàng, sản phẩm, danh mục, thương hiệu, đơn hàng, tồn kho, voucher, đánh giá, thống kê.

Hệ thống có ChatbotAI tư vấn sản phẩm cho khách và hỗ trợ Admin tra cứu dữ liệu (top sản phẩm bán chạy, doanh thu, nhóm tuổi khách hàng mua nhiều nhất).

## Ràng buộc kiến trúc

- Java 25, Spring Boot, Maven, REST API, Spring Data JPA, Spring Security + JWT, Validation.
- Database MySQL, migration bằng Flyway (`src/main/resources/db/migration`).
- Không trả Entity trực tiếp ra API — mọi request/response dùng DTO riêng theo module.
- Mọi response API dùng chung `com.laptophub.common.ApiResponse<T>`.
- Mọi lỗi xử lý tập trung qua `com.laptophub.common.exception.GlobalExceptionHandler`.
- Prefix API: `/api/v1` (cấu hình qua `server.servlet.context-path`).
- Tiền dùng `BigDecimal`, không dùng `double`/`float`.
- Nghiệp vụ ghi nhiều bảng dùng `@Transactional`.
- Không xóa vật lý dữ liệu quan trọng — ưu tiên cột trạng thái `ACTIVE`/`INACTIVE` (soft delete).
- Code chia theo module package dưới gốc `com.laptophub`, không chia theo layer dùng chung toàn hệ thống (không có package `controller`/`service`/`repository` cấp gốc).
- Không hardcode secret (password DB, JWT secret, API key) trong code hay `application.yml` — luôn qua biến môi trường, tham khảo `.env.example`.

## Cấu trúc package

```
com.laptophub.common      hạ tầng dùng chung: ApiResponse, ErrorCode, exception handler
com.laptophub.config      cấu hình bean hạ tầng (CORS, OpenAPI, ...)
com.laptophub.security    Spring Security + JWT filter, phân quyền theo actor
com.laptophub.auth        đăng ký, đăng nhập, refresh token
com.laptophub.user        hồ sơ khách hàng, quản lý khách hàng (Admin)
com.laptophub.catalog     sản phẩm, danh mục, thương hiệu
com.laptophub.cart        giỏ hàng
com.laptophub.order       đặt hàng, xem/hủy đơn, quản lý đơn (Admin)
com.laptophub.payment     thanh toán COD / chuyển khoản
com.laptophub.voucher     voucher giảm giá
com.laptophub.review      đánh giá sản phẩm
com.laptophub.inventory   tồn kho
com.laptophub.dashboard   thống kê cho Admin
com.laptophub.chatbot     ChatbotAI tư vấn + tra cứu dữ liệu
```

Mỗi module tự chứa các layer con của riêng nó (`controller`, `service`, `repository`, `entity`, `dto`) khi module đó bắt đầu có code.

## Quy tắc làm việc

- Không code toàn bộ hệ thống một lần — mỗi lần chỉ làm một giai đoạn nhỏ (xem [MODULE_PLAN.md](MODULE_PLAN.md) để biết thứ tự).
- Trước khi code, đọc các file trong `docs/` nếu có liên quan.
- Thay đổi API → cập nhật [API_CONVENTION.md](API_CONVENTION.md).
- Thay đổi database → tạo file Flyway migration mới, cập nhật [DATABASE_DESIGN.md](DATABASE_DESIGN.md).
- Không tự ý đổi kiến trúc nếu chưa giải thích lý do.
- Sau mỗi giai đoạn, liệt kê file đã tạo/sửa, API đã có, và cách test bằng Postman/cURL.
- Sau khi tạo hoặc sửa code, đảm bảo project compile được bằng `./mvnw clean test`.

## Lịch sử thay đổi

- 2026-07-02: Khởi tạo tài liệu, chốt lại ràng buộc kiến trúc và quy tắc làm việc.
