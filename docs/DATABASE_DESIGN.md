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

MVP dùng một kho chính nhưng vẫn có thể tạo bảng kho để dễ mở rộng:

```text
warehouses
inventory_balances
stock_receipts
stock_receipt_items
inventory_movements
inventory_reservations
```

`inventory_balances` tối thiểu:

```text
warehouse_id
product_variant_id
on_hand_quantity
reserved_quantity
version
```

Ràng buộc gợi ý:

```text
UNIQUE(warehouse_id, product_variant_id)
on_hand_quantity >= 0
reserved_quantity >= 0
reserved_quantity <= on_hand_quantity
```

Tồn khả dụng được tính:

```text
available = on_hand - reserved
```

`inventory_movements` lưu lịch sử như `RECEIPT`, `RESERVE`, `RELEASE`, `SHIPMENT`, `RETURN`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`.

Serial, hàng lỗi và nhiều kho có thể thêm sau mà không bắt buộc trong giai đoạn đầu.

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

Warehouse  1 --- N InventoryBalance
Variant    1 --- N InventoryBalance
OrderItem  1 --- N InventoryReservation

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
