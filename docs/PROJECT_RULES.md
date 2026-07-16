# Project Rules — LaptopHub

## 1. Mục tiêu

LaptopHub là backend thương mại điện tử cho một cửa hàng hoặc chuỗi cửa hàng bán laptop và phụ kiện máy tính.

Actor chính:

- `Guest`: xem, tìm kiếm, lọc, so sánh sản phẩm, dùng chatbot và đăng ký/đăng nhập.
- `Customer`: mua hàng, quản lý giỏ hàng, đơn hàng, địa chỉ và đánh giá.
- `Admin`: quản lý người dùng, catalog, kho, voucher, đơn hàng và dashboard.

Admin quản lý kho trong MVP. Chưa cần tách vai trò nhân viên kho.

## 2. Kiến trúc

- Spring Boot + MySQL, phát triển theo **modular monolith**.
- Chia package theo nghiệp vụ: `auth`, `user`, `catalog`, `inventory`, `cart`, `voucher`, `order`, `payment`, `review`, `dashboard`, `chatbot`.
- Có thể tách/gộp package khi giúp code rõ hơn, nhưng không tạo microservice hoặc abstraction phức tạp khi chưa cần.
- Module nên giao tiếp qua service; hạn chế module này sửa repository của module khác.

## 3. Quy tắc code

- Controller chỉ nhận request, kiểm tra đầu vào cơ bản và gọi service.
- Business logic đặt trong service/domain phù hợp.
- API dùng DTO, không trả Entity trực tiếp.
- Tiền dùng `BigDecimal`; thời gian audit ưu tiên `Instant` hoặc cách nhất quán toàn dự án.
- Nghiệp vụ cập nhật nhiều bảng phải dùng `@Transactional`.
- Secret, mật khẩu và API key không hardcode.
- Thay đổi schema phải có Flyway migration.
- Ưu tiên code dễ đọc, chạy đúng và dễ thay đổi; chỉ tối ưu khi có lý do.

## 4. Catalog

- `Product` chứa thông tin chung của sản phẩm.
- `ProductVariant` chứa cấu hình/SKU cụ thể và giá bán.
- Cart, Order và Inventory làm việc với `ProductVariant`.
- Danh mục, thương hiệu và sản phẩm đã phát sinh giao dịch nên ngừng hoạt động bằng status thay vì xóa cứng.
- Thông số kỹ thuật có thể thiết kế linh hoạt, miễn hỗ trợ hiển thị, lọc và so sánh.

## 5. Kho tối giản nhưng đúng nghiệp vụ

MVP dùng một kho chính và Admin quản lý.

Tối thiểu cần phân biệt:

```text
onHandQuantity     số lượng thực tế trong kho
reservedQuantity   số lượng đã giữ cho đơn
availableQuantity  onHandQuantity - reservedQuantity
```

Luồng chính:

```text
Nhập kho       → tăng onHand
Đặt hàng       → tăng reserved
Hủy đơn        → giảm reserved
Xuất hàng      → giảm onHand và reserved
Trả hàng tốt   → tăng onHand
Điều chỉnh     → thay đổi onHand và ghi rõ lý do
```

- Thêm vào giỏ không giữ hàng.
- Không sửa trực tiếp số tồn mà không ghi lịch sử.
- Mọi thay đổi kho cần có movement/reference để truy vết.
- Phải chống bán vượt tồn bằng locking, optimistic version hoặc update có điều kiện.
- Serial, hàng lỗi, nhiều kho và chuyển kho là phần mở rộng; có thể bổ sung sau khi luồng số lượng hoạt động ổn định.

## 6. Cart, Order và Payment

- Checkout phải đọc lại giá, trạng thái sản phẩm, voucher và tồn kho từ database.
- Frontend không quyết định tổng tiền cuối cùng.
- `OrderItem` lưu snapshot tên, SKU, giá và cấu hình tại thời điểm mua.
- Trạng thái đơn chỉ thay đổi theo luồng hợp lệ, không cập nhật tùy ý.
- Customer chỉ được xem và thao tác trên đơn của chính mình.
- COD nên được hoàn thiện trước; payment online triển khai sau.
- Callback/webhook phải xác minh và xử lý idempotent.

Luồng trạng thái có thể điều chỉnh, nhưng nên giữ đơn giản:

```text
PENDING → CONFIRMED → PREPARING → SHIPPING → DELIVERED
                     ↘ CANCELLED
DELIVERED → RETURN_REQUESTED → RETURNED
```

## 7. Voucher và Review

- Voucher được backend kiểm tra thời gian, điều kiện đơn, số lần sử dụng và phạm vi áp dụng.
- Review chỉ được tạo khi Customer đã mua và đơn đã hoàn thành.
- Admin có thể ẩn review vi phạm, không sửa nội dung thay khách hàng.

## 8. Dashboard và ChatbotAI

Dashboard MVP gồm doanh thu, đơn theo trạng thái, khách hàng, sản phẩm bán chạy và sản phẩm sắp hết hàng.

ChatbotAI:

- Tư vấn dựa trên dữ liệu sản phẩm thực tế.
- Không bịa sản phẩm, giá hoặc thông số.
- Dữ liệu động như giá, tồn, đơn và thống kê phải lấy qua service/tool backend.
- Chỉ gọi các hàm thống kê đã định nghĩa và kiểm tra role trước khi trả kết quả.
- Không cho AI tự chạy SQL tùy ý trên database.

## 9. Quy tắc cho AI Agent

AI Agent được phép:

- Điều chỉnh tên class, bảng hoặc package khi có lý do hợp lý.
- Bổ sung DTO, mapper, validation, index, migration và test cần thiết.
- Đề xuất giải pháp đơn giản hơn nếu vẫn giữ đúng nghiệp vụ.
- Refactor phần vừa làm khi phát hiện thiết kế gây lỗi hoặc khó mở rộng.

AI Agent không được:

- Tự ý bỏ nghiệp vụ chính.
- Tạo công nghệ hoặc kiến trúc vượt quá phạm vi MVP.
- Thay đổi migration đã chạy theo cách phá hủy dữ liệu mà không giải thích.
- Trộn business logic lớn vào Controller.
- Bỏ qua phân quyền, transaction hoặc kiểm tra tồn trong các luồng quan trọng.

Tài liệu là định hướng sống. Khi code và tài liệu khác nhau, cần cập nhật tài liệu hoặc ghi rõ lý do thay đổi.
