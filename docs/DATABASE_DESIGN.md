# Database Design — LaptopHub

Tài liệu mô tả hướng dữ liệu tổng quát, không phải schema bất biến. AI Agent có thể điều chỉnh khi triển khai miễn giữ đúng nghiệp vụ và có Flyway migration.

## 1. Nguyên tắc

- MySQL + Flyway.
- Tiền dùng `DECIMAL`; Java dùng `BigDecimal`.
- Dữ liệu đã phát sinh giao dịch ưu tiên status/soft delete.
- Thêm index, unique constraint và foreign key theo truy vấn thực tế.
- Không thiết kế quá sâu cho chức năng chưa triển khai.

## 2. Nhóm bảng chính

### User và Auth

```text
users
addresses
refresh_tokens        (nếu dùng)
```

`users` hỗ trợ tối thiểu `ADMIN` và `CUSTOMER`. Guest không cần lưu thành role trong database.

Ranh giới ownership giữa module `auth`/`security`/`user`, schema chi tiết
`refresh_tokens`, JWT claims/TTL và quy tắc logout/rotation/reuse detection đã
chốt ở `AUTH_SECURITY_USER_CONTRACT.md` (gói ASU-00) — không lặp lại chi tiết
đó ở đây.

### Catalog

```text
categories
brands
products
product_variants
product_images
specification_groups  (tùy thiết kế)
specifications        (tùy thiết kế)
product_spec_values   (tùy thiết kế)
```

- `Product` lưu thông tin chung.
- `ProductVariant` lưu SKU/cấu hình và giá bán.
- Cart, Order và Inventory tham chiếu variant.

### Inventory

Giai đoạn 4 (Inventory Lite) đã triển khai, lệch so với gợi ý ban đầu ở 2
điểm — quyết định có chủ đích, không phải thiếu sót:

- **Không có bảng `warehouses`** — MVP chỉ 1 kho ngầm định, `inventory_balances`
  khoá 1-1 theo `product_variant_id` (UNIQUE), không có `warehouse_id`. Thêm
  multi-warehouse sau là additive migration (thêm cột `warehouse_id`).
- **Không có bảng `inventory_reservations` riêng** — `reserved_quantity`
  (aggregate trên `inventory_balances`) cộng `inventory_movements` (log
  `RESERVE`/`RELEASE` với `reference_type`/`reference_id`) là đủ để chống bán
  vượt tồn và truy vết. Chưa có `order_items` (Giai đoạn 5/6) để 1 dòng
  reservation trỏ vào nên hoãn tới khi có nhu cầu cụ thể (TTL giữ hàng,
  release từng phần theo đơn).

```text
inventory_balances
stock_receipts
stock_receipt_items
inventory_movements
```

`inventory_balances`:

```text
product_variant_id   (UNIQUE)
on_hand_quantity
reserved_quantity
```

Ràng buộc:

```text
UNIQUE(product_variant_id)
on_hand_quantity >= 0
reserved_quantity >= 0
reserved_quantity <= on_hand_quantity
```

Tồn khả dụng được tính (derived, không lưu cột):

```text
available = on_hand - reserved
```

**Không có cột `version`** — chống bán vượt tồn dùng update có điều kiện ở
tầng repository (`UPDATE ... WHERE <điều kiện đủ tồn>`, kiểm tra số dòng bị
ảnh hưởng) thay vì optimistic locking.

`inventory_movements` lưu lịch sử immutable, append-only: `RECEIPT`,
`RESERVE`, `RELEASE`, `SHIPMENT`, `RETURN`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`
— kèm `reference_type`/`reference_id` (nullable, không FK — tham chiếu
polymorphic tới `stock_receipts` hoặc, ở giai đoạn sau, `order_items`) và
snapshot `on_hand_after`/`reserved_after` sau khi áp dụng.

`stock_receipts` có state machine `DRAFT` → `CONFIRMED`/`CANCELLED`: chỉ
`confirm` mới thực sự tăng `on_hand` (gọi `InventoryService.receiveStock` cho
từng dòng `stock_receipt_items`) và ghi movement; `DRAFT` bị hủy không ảnh
hưởng tồn.

`InventoryService.reserve/release/fulfill/receiveReturn` đã có sẵn (dùng
cùng cơ chế update có điều kiện) nhưng **chưa có endpoint HTTP** — Order
module (Giai đoạn 5/6) sẽ gọi trực tiếp qua service bean khi `order_items`
tồn tại.

Serial, hàng lỗi và nhiều kho vẫn là phần mở rộng, chưa triển khai.

### Cart

```text
carts
cart_items
```

- Giỏ Customer lưu trong database.
- Giỏ Guest có thể lưu phía frontend và hợp nhất sau đăng nhập.
- Thêm giỏ không giữ tồn.

### Order

```text
orders
order_items
order_status_history
```

Có thể bổ sung:

```text
return_requests
return_request_items
shipments
```

`order_items` cần lưu snapshot tối thiểu:

```text
product_variant_id
product_name
variant_name
sku
unit_price
quantity
discount_amount
```

Địa chỉ giao hàng nên được snapshot trong order hoặc bảng riêng gắn với order.

### Payment

```text
payments
payment_transactions   (nếu cần)
```

Lưu phương thức, trạng thái, số tiền, mã giao dịch và dữ liệu đối soát cần thiết. Không dùng dữ liệu frontend làm nguồn xác nhận thanh toán.

### Voucher

```text
vouchers
voucher_usages
```

Có thể mở rộng điều kiện voucher theo sản phẩm, danh mục hoặc người dùng khi nghiệp vụ cần.

### Review

```text
reviews
```

Review phải liên kết được với người mua và dữ liệu đơn hàng để kiểm tra quyền đánh giá.

### Chatbot

```text
chat_conversations  (nếu lưu lịch sử)
chat_messages       (nếu lưu lịch sử)
```

Vector store hoặc dữ liệu RAG có thể nằm ngoài MySQL.

## 3. Quan hệ tổng quát

```text
User       1 --- N Address
User       1 --- N Order
User       1 --- 1 Cart

Category   1 --- N Product
Brand      1 --- N Product
Product    1 --- N ProductVariant
Product    1 --- N ProductImage

ProductVariant  1 --- 1 InventoryBalance
ProductVariant  1 --- N InventoryMovement
StockReceipt    1 --- N StockReceiptItem

Cart       1 --- N CartItem
Order      1 --- N OrderItem
Order      1 --- N Payment
Voucher    1 --- N VoucherUsage
Product    1 --- N Review
```

Quan hệ cụ thể có thể thay đổi khi code, miễn tránh dữ liệu mồ côi và giữ đúng ownership.

## 4. Quy tắc dữ liệu quan trọng

- Tạo đơn và reserve tồn phải cùng transaction.
- Hủy đơn chỉ release phần tồn đang giữ; không cộng `on_hand` nếu hàng chưa xuất.
- Xuất hàng giảm cả `on_hand` và `reserved`.
- Mọi thay đổi tồn phải có movement hoặc reference tương ứng.
- Không cho tồn âm hoặc bán vượt tồn.
- Trạng thái Order/Payment/Reservation nên dùng enum rõ nghĩa và kiểm soát chuyển trạng thái.
- Migration mới không nên sửa phá hủy migration đã chạy; dùng migration bổ sung để thay đổi schema.

## 5. Dashboard

Giai đoạn đầu chưa cần bảng thống kê riêng. Dashboard có thể query tổng hợp từ:

```text
orders
order_items
payments
inventory_balances
users
```

Chỉ thêm bảng tổng hợp/cache khi truy vấn thực tế cho thấy cần thiết.
