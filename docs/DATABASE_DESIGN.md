# Database Design — LaptopHub Backend

Trạng thái hiện tại: **chưa có bảng nào**. File này là khung quy ước + kế hoạch bảng dữ liệu, sẽ được cập nhật dần cùng với từng file Flyway migration ở các giai đoạn sau.

## Công cụ

- MySQL, quản lý schema bằng **Flyway** (`src/main/resources/db/migration`, đặt tên `V{n}__mo_ta.sql`).
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate **không** tự tạo/sửa bảng, mọi thay đổi schema phải qua migration Flyway.

## Quy ước chung cho mọi bảng

- Primary key: `id BIGINT AUTO_INCREMENT`.
- Cột thời gian: `created_at`, `updated_at` (`DATETIME`, mặc định `CURRENT_TIMESTAMP`).
- Soft delete: cột `status` (`ACTIVE`/`INACTIVE`) thay vì xóa vật lý — áp dụng cho dữ liệu quan trọng (user, product, order, voucher...).
- Tiền: `DECIMAL(15,2)` (khớp `BigDecimal` phía Java), không dùng `FLOAT`/`DOUBLE`.
- Tên bảng: số nhiều, snake_case (`products`, `order_items`).
- Khóa ngoại: đặt tên `fk_<bảng>_<bảng_tham_chiếu>`.

## Kế hoạch bảng theo module

Danh sách dự kiến — sẽ chốt chi tiết cột khi thực sự code từng module, không tạo migration trước khi có entity đi kèm.

| Module | Bảng dự kiến |
|---|---|
| `user` | `users`, `addresses` |
| `catalog` | `products`, `categories`, `brands`, `product_images` |
| `cart` | `carts`, `cart_items` |
| `order` | `orders`, `order_items` |
| `payment` | `payments` |
| `voucher` | `vouchers`, `voucher_usages` |
| `review` | `reviews` |
| `inventory` | `inventory_transactions` (hoặc cột tồn kho trực tiếp trên `products`, quyết định khi code module `inventory`) |

## Lịch sử thay đổi

- 2026-07-02: Khởi tạo tài liệu quy ước + kế hoạch bảng. Chưa có migration nào.
