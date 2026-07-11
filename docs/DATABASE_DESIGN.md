# Database Design — LaptopHub

Tài liệu này mô tả **hướng thiết kế dữ liệu ở mức tổng quan** cho dự án LaptopHub. Đây không phải schema cố định. Trong quá trình phát triển, có thể thêm, bớt hoặc tách bảng nếu nghiệp vụ thực tế yêu cầu.

## 1. Nguyên tắc chung

- Sử dụng MySQL và quản lý thay đổi schema bằng Flyway.
- Entity chỉ mô tả dữ liệu cần thiết cho nghiệp vụ đang triển khai.
- Tiền sử dụng `BigDecimal` trong Java và kiểu `DECIMAL` trong database.
- API không trả trực tiếp Entity.
- Các quan hệ, index và constraint được bổ sung khi bắt đầu triển khai module tương ứng.
- Không thiết kế quá sâu cho những nghiệp vụ chưa cần dùng.

## 2. Các nhóm dữ liệu chính

### User và xác thực

Dùng để quản lý:

- Tài khoản khách hàng và Admin.
- Thông tin cá nhân.
- Địa chỉ nhận hàng.
- Thông tin phục vụ đăng nhập và phân quyền.

Các bảng có thể gồm:

```text
users
addresses
refresh_tokens (nếu cần)
```

### Catalog

Dùng để quản lý sản phẩm bán trên hệ thống.

Các bảng chính:

```text
categories
brands
products
product_variants
product_images
```

Quy ước:

- `Product` mô tả sản phẩm chung.
- `ProductVariant` mô tả sản phẩm con hoặc cấu hình cụ thể.
- Giá bán và số lượng tồn được quản lý ở `ProductVariant`.
- Các thuộc tính cần tìm kiếm hoặc lọc có thể được lưu thành cột riêng hoặc cấu trúc phù hợp với cách triển khai.

Ví dụ:

```text
Product: Lenovo ThinkPad T14

ProductVariant:
- RAM 16GB
- SSD 512GB
- Giá bán
- Số lượng tồn
```

### Cart

Dùng để lưu giỏ hàng của Customer.

Các bảng có thể gồm:

```text
carts
cart_items
```

`CartItem` tham chiếu tới sản phẩm con mà khách thực sự muốn mua.

### Order

Dùng để quản lý quá trình đặt hàng và theo dõi đơn.

Các bảng chính:

```text
orders
order_items
order_status_history (có thể bổ sung nếu cần)
```

`OrderItem` nên lưu lại thông tin cần thiết tại thời điểm mua như tên sản phẩm, cấu hình, giá và số lượng. Nhờ đó đơn cũ không bị thay đổi khi sản phẩm được cập nhật sau này.

### Payment

Dùng để hỗ trợ COD và thanh toán online.

Các bảng có thể gồm:

```text
payments
payment_transactions (nếu cần)
```

Thiết kế cụ thể phụ thuộc vào cổng thanh toán được chọn. Hệ thống cần lưu được trạng thái thanh toán, mã giao dịch và thông tin cần thiết để đối soát.

### Voucher

Dùng để quản lý mã giảm giá và lịch sử sử dụng.

Các bảng có thể gồm:

```text
vouchers
voucher_usages
```

Voucher có thể hỗ trợ giảm theo phần trăm hoặc số tiền cố định. Các điều kiện chi tiết được bổ sung theo phạm vi triển khai thực tế.

### Review

Dùng để lưu đánh giá sản phẩm của khách hàng.

Các bảng có thể gồm:

```text
reviews
```

Review cần liên kết được với người mua và sản phẩm đã mua để backend kiểm tra quyền đánh giá.

### Chatbot

Tùy cách triển khai, có thể có:

```text
chat_conversations
chat_messages
documents
```

Dữ liệu vector có thể lưu ở dịch vụ hoặc vector store riêng, không bắt buộc nằm trong MySQL.

## 3. Tồn kho tối giản

Dự án không xây dựng hệ thống quản lý kho đầy đủ.

Nguồn số lượng tồn chính là:

```text
product_variants.stock_quantity
```

Luồng xử lý:

```text
Admin nhập thêm hàng
→ tăng stockQuantity

Khách đặt hàng
→ backend kiểm tra đủ hàng
→ giảm stockQuantity

Đơn bị hủy hợp lệ hoặc thanh toán thất bại
→ hoàn lại stockQuantity
```

Thêm sản phẩm vào giỏ không giữ hàng. Hệ thống kiểm tra lại số lượng tại thời điểm checkout.

Cách xử lý đồng thời có thể dùng câu lệnh update có điều kiện, locking hoặc giải pháp tương đương miễn bảo đảm số lượng không bị âm và không bán vượt tồn.

## 4. Thống kê cơ bản

Không cần tạo bảng thống kê riêng trong giai đoạn đầu.

Dashboard Admin có thể truy vấn trực tiếp từ:

```text
orders
order_items
payments
```

Các thống kê dự kiến:

- Tổng doanh thu trong một khoảng thời gian.
- Số đơn hoàn thành, đang xử lý hoặc đã hủy.
- Doanh thu theo ngày hoặc tháng.
- Giá trị đơn trung bình.
- Sản phẩm bán chạy.

Doanh thu chủ yếu được tính từ các đơn hoàn thành theo quy tắc nghiệp vụ của hệ thống.

## 5. Quan hệ tổng quát

```text
User       1 --- N Address
User       1 --- N Order
User       1 --- 1 Cart

Category   1 --- N Product
Brand      1 --- N Product
Product    1 --- N ProductVariant
Product    1 --- N ProductImage

Cart       1 --- N CartItem
Order      1 --- N OrderItem
Order      1 --- N Payment
Voucher    1 --- N VoucherUsage
Product    1 --- N Review
```

Số lượng bảng và cách đặt foreign key có thể thay đổi khi code, miễn vẫn giữ đúng mục tiêu nghiệp vụ.

## 6. Nguyên tắc khi mở rộng

AI Agent hoặc lập trình viên được phép:

- Thêm bảng hoặc cột mới khi một tính năng cần dữ liệu riêng.
- Tách một bảng lớn thành nhiều bảng nếu giúp code rõ ràng hơn.
- Bổ sung index, unique constraint hoặc bảng lịch sử khi xuất hiện nhu cầu thực tế.
- Thay đổi chi tiết schema trước khi module được phát hành.

Mọi thay đổi database cần có Flyway migration và không được làm mất dữ liệu đang sử dụng.
