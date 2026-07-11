# Module Plan — LaptopHub

Tài liệu này mô tả **hướng phát triển tổng quát** của dự án. Thứ tự có thể thay đổi theo tiến độ, mức độ phụ thuộc và kết quả trong quá trình code.

Mục tiêu là xây dựng từng luồng có thể chạy và demo được, thay vì hoàn thiện toàn bộ chi tiết của một module trước khi kết nối với phần còn lại.

## 1. Các module chính

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

Tên package hoặc cách chia nhỏ bên trong có thể được điều chỉnh khi triển khai.

## 2. Lộ trình đề xuất

### Giai đoạn 1 — Nền tảng dự án

- Cấu trúc Spring Boot.
- Cấu hình môi trường.
- Kết nối MySQL.
- Flyway.
- Response và exception chung.
- Logging và validation cơ bản.

### Giai đoạn 2 — Auth và User

- Đăng ký, đăng nhập.
- JWT và phân quyền Customer/Admin.
- Quản lý thông tin cá nhân và địa chỉ.
- Admin quản lý tài khoản khách hàng ở mức cần thiết.

### Giai đoạn 3 — Catalog và tồn kho tối giản

- Danh mục và thương hiệu.
- Sản phẩm và sản phẩm con.
- Hình ảnh, giá và thông số sản phẩm.
- Admin CRUD sản phẩm.
- Tìm kiếm, lọc, sắp xếp và phân trang.
- Mỗi sản phẩm con có trường số lượng tồn.
- Admin có thể nhập thêm số lượng.

### Giai đoạn 4 — Cart và Voucher

- Thêm, sửa, xóa sản phẩm trong giỏ.
- Tính tổng tiền tạm thời.
- Áp dụng voucher theo các điều kiện được hỗ trợ.
- Kiểm tra lại giá, voucher và tồn kho khi checkout.

### Giai đoạn 5 — Order

- Tạo đơn hàng.
- Lưu thông tin sản phẩm và giá tại thời điểm đặt.
- Kiểm tra và giảm tồn kho.
- Theo dõi trạng thái đơn.
- Customer xem và hủy đơn khi được phép.
- Admin quản lý và cập nhật trạng thái đơn.

Nên làm luồng COD trước để hoàn thiện quy trình đặt hàng cơ bản.

### Giai đoạn 6 — Payment online

- Tạo yêu cầu thanh toán.
- Tích hợp một cổng thanh toán phù hợp.
- Xử lý callback hoặc webhook.
- Đồng bộ trạng thái Order và Payment.
- Hoàn tồn khi giao dịch thất bại, bị hủy hoặc hết hạn nếu nghiệp vụ yêu cầu.

### Giai đoạn 7 — Review

- Customer đánh giá sản phẩm đã mua.
- Hiển thị điểm và nội dung đánh giá.
- Admin có thể quản lý các đánh giá không phù hợp.

### Giai đoạn 8 — Dashboard cơ bản

- Tổng doanh thu.
- Số lượng đơn theo trạng thái.
- Doanh thu theo thời gian.
- Giá trị đơn trung bình.
- Sản phẩm bán chạy.

Các số liệu được lấy trực tiếp bằng truy vấn từ dữ liệu đơn hàng, chưa cần hệ thống báo cáo riêng.

### Giai đoạn 9 — ChatbotAI

- Tư vấn chọn sản phẩm.
- So sánh sản phẩm.
- Trả lời chính sách bằng RAG.
- Hỗ trợ Customer tra cứu thông tin được phép.
- Hỗ trợ Admin hỏi một số thống kê cơ bản.
- Lưu lịch sử hội thoại nếu cần.

### Giai đoạn 10 — Hoàn thiện

- Kiểm thử các luồng chính.
- Chuẩn hóa lỗi và validation.
- Bổ sung logging, security và tài liệu API.
- Tối ưu truy vấn cần thiết.
- Chuẩn bị dữ liệu và kịch bản demo.

## 3. Cách phát triển khuyến nghị

Ưu tiên các vertical slice có thể chạy xuyên suốt:

```text
Admin tạo sản phẩm
→ Guest xem sản phẩm
→ Customer thêm giỏ hàng
→ đặt hàng
→ thanh toán
→ Admin xử lý đơn
→ Customer nhận hàng và đánh giá
```

Sau khi luồng cơ bản hoạt động, tiếp tục bổ sung voucher, dashboard và chatbot.

## 4. Mức độ linh hoạt

AI Agent được phép:

- Thay đổi thứ tự triển khai nếu phát hiện phụ thuộc hợp lý hơn.
- Tách hoặc gộp service, DTO, mapper và package để code dễ bảo trì.
- Đề xuất thư viện hoặc cách triển khai khác khi phù hợp với Spring Boot.
- Bổ sung migration, test hoặc cấu hình khi một tính năng cần đến.
- Chọn giải pháp đơn giản trước, sau đó refactor khi nghiệp vụ rõ hơn.

Không cần triển khai tất cả chi tiết ngay từ đầu. Mỗi giai đoạn chỉ cần đủ để chạy đúng luồng và tạo nền cho bước tiếp theo.

## 5. Tiêu chí hoàn thành chung

Một chức năng được xem là hoàn thành khi:

- Có luồng API hoạt động.
- Dữ liệu được lưu và đọc đúng.
- Có validation và xử lý lỗi cơ bản.
- Có phân quyền nếu chức năng yêu cầu.
- Không làm sai dữ liệu của module khác.
- Có ít nhất kiểm thử hoặc kịch bản kiểm tra cho luồng quan trọng.
- Tài liệu được cập nhật khi có thay đổi đáng kể.
