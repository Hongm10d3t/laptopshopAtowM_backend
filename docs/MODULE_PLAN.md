# Module Plan — LaptopHub

Mục tiêu là phát triển theo các **vertical slice chạy được**, không hoàn thiện quá sâu một module rồi mới kết nối toàn hệ thống.

## 1. Module dự kiến

```text
common
config
security
auth
user
catalog
inventory
cart
voucher
order
payment
review
dashboard
chatbot
```

Tên và cách chia nhỏ có thể thay đổi khi triển khai.

## 2. Lộ trình đề xuất

### Giai đoạn 1 — Nền tảng

- Chuẩn hóa cấu hình môi trường.
- MySQL, Flyway, validation, logging.
- Response và exception chung.
- Security skeleton và tài liệu API.

### Giai đoạn 2 — Auth và User

- Đăng ký, đăng nhập, refresh token nếu dùng.
- Role `ADMIN`, `CUSTOMER`.
- Hồ sơ và địa chỉ Customer.
- Admin xem/khóa/mở tài khoản ở mức cần thiết.

### Giai đoạn 3 — Catalog

- Category, Brand, Product, ProductVariant.
- Ảnh, giá, SKU và thông số kỹ thuật.
- Admin CRUD và thay đổi trạng thái.
- Public xem chi tiết, tìm kiếm, lọc, sắp xếp, phân trang.

### Giai đoạn 4 — Inventory Lite

- Một kho chính.
- Inventory balance: on-hand, reserved, available.
- Phiếu nhập và lịch sử movement.
- Điều chỉnh tồn có lý do.
- Chống bán vượt tồn.

Serial và nhiều kho chỉ bổ sung sau nếu tiến độ cho phép.

### Giai đoạn 5 — Cart và Checkout COD

- Cart cho Customer; Guest cart có thể lưu phía frontend.
- Tính lại giá và kiểm tra trạng thái sản phẩm khi checkout.
- Tạo Order/OrderItem snapshot.
- Reserve tồn khi đặt hàng thành công.
- Hoàn thiện luồng COD trước.

### Giai đoạn 6 — Xử lý đơn hàng

- Customer xem và hủy đơn hợp lệ.
- Admin xác nhận, chuẩn bị, xuất và hoàn tất đơn.
- Release tồn khi hủy; giảm on-hand khi xuất.
- Lưu lịch sử trạng thái.
- Bổ sung yêu cầu trả hàng ở mức MVP.

### Giai đoạn 7 — Voucher và Payment Online

- Voucher cơ bản: thời gian, giá trị tối thiểu, giới hạn sử dụng.
- Tích hợp một cổng thanh toán phù hợp.
- Callback/webhook xác minh và idempotent.
- Đồng bộ trạng thái Payment, Order và reservation.

### Giai đoạn 8 — Review và So sánh

- Review sản phẩm đã mua.
- Admin ẩn review vi phạm.
- So sánh sản phẩm cùng nhóm dựa trên thông số và giá.

### Giai đoạn 9 — Dashboard

- Doanh thu theo thời gian.
- Đơn theo trạng thái.
- Khách hàng.
- Sản phẩm bán chạy.
- Sản phẩm sắp hết hàng.

Ưu tiên query trực tiếp; chưa cần data warehouse.

### Giai đoạn 10 — ChatbotAI

- Tư vấn và gợi ý sản phẩm từ catalog.
- Nhận xét so sánh sản phẩm.
- Tool/hàm thống kê dành cho Admin.
- Kiểm tra role trước khi gọi tool.
- Không cho AI chạy SQL tùy ý.

### Giai đoạn 11 — Hoàn thiện

- Integration test các luồng chính.
- Tối ưu query có vấn đề.
- Rà soát security, transaction, idempotency.
- Chuẩn bị dữ liệu và kịch bản demo.

## 3. Vertical slice ưu tiên

Luồng đầu tiên nên chạy xuyên suốt:

```text
Admin tạo sản phẩm
→ Admin nhập kho
→ Guest xem/tìm sản phẩm
→ Customer thêm giỏ
→ Customer đặt đơn COD
→ hệ thống reserve tồn
→ Admin xác nhận và xuất đơn
→ hệ thống giảm tồn
→ Customer xem đơn và đánh giá
```

Sau khi luồng này ổn mới bổ sung voucher, payment online, return, dashboard và chatbot.

## 4. Tiêu chí hoàn thành một chức năng

- API và dữ liệu hoạt động đúng.
- Có validation, phân quyền và xử lý lỗi cần thiết.
- Không làm sai dữ liệu module khác.
- Transaction được dùng cho luồng nhiều bước.
- Có test hoặc kịch bản kiểm tra cho nghiệp vụ quan trọng.
- Migration và tài liệu được cập nhật nếu contract/schema thay đổi đáng kể.

## 5. Quyền linh hoạt của AI Agent

AI Agent có thể:

- Đổi thứ tự giai đoạn khi có phụ thuộc hợp lý hơn.
- Tách/gộp DTO, service, mapper hoặc package.
- Dùng giải pháp kỹ thuật khác nếu đơn giản, phổ biến và tương thích với project.
- Đề xuất bỏ một phần không quan trọng khỏi sprint hiện tại, nhưng không tự ý bỏ khỏi phạm vi tổng thể.

Mỗi thay đổi lớn nên kèm giải thích ngắn: vấn đề, lựa chọn và ảnh hưởng.
