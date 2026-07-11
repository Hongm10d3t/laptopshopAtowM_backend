# Project Rules — LaptopHub

Tài liệu này là bộ nguyên tắc định hướng cho dự án. Mục tiêu là giữ code thống nhất nhưng vẫn đủ linh hoạt để phát triển nhanh bằng AI Agent.

## 1. Mục tiêu dự án

LaptopHub là website bán laptop, linh kiện và phụ kiện máy tính, tích hợp ChatbotAI.

Actor chính:

- Guest.
- Customer.
- Admin.

Hệ thống giữ các nhóm chức năng chính:

- Xác thực và quản lý người dùng.
- Danh mục, thương hiệu, sản phẩm và sản phẩm con.
- Tìm kiếm và lọc sản phẩm.
- Giỏ hàng.
- Voucher.
- Đặt hàng.
- COD và thanh toán online.
- Theo dõi trạng thái đơn.
- Review sản phẩm.
- Dashboard thống kê cơ bản.
- ChatbotAI cho khách hàng và Admin.

## 2. Kiến trúc

- Sử dụng Spring Boot, MySQL và React.
- Backend phát triển theo modular monolith.
- Chia code theo nghiệp vụ thay vì gom toàn bộ controller, service hoặc repository vào một package chung.
- Có thể thay đổi cấu trúc package khi phát hiện cách tổ chức rõ ràng hơn.
- Không chuyển sang microservice khi chưa có nhu cầu thực tế.

## 3. Quy tắc code chung

- API sử dụng DTO, không trả trực tiếp Entity.
- Validation đặt ở request và service theo mức phù hợp.
- Lỗi được xử lý theo một format tương đối thống nhất.
- Không để business logic lớn trong Controller.
- Nghiệp vụ ghi nhiều dữ liệu liên quan cần dùng transaction.
- Tiền sử dụng `BigDecimal`.
- Secret và thông tin môi trường không hardcode trong source.
- Thay đổi database phải đi qua Flyway migration.
- Ưu tiên code dễ hiểu và chạy đúng trước khi tối ưu quá sớm.

## 4. Catalog và tồn kho

- `Product` đại diện cho sản phẩm chung.
- `ProductVariant` đại diện cho sản phẩm con hoặc cấu hình cụ thể.
- Giá và số lượng tồn được quản lý ở sản phẩm con.
- Giỏ hàng và đơn hàng làm việc với sản phẩm con.

Tồn kho được tối giản:

```text
Admin nhập thêm hàng
→ tăng số lượng

Checkout
→ kiểm tra còn hàng
→ giảm số lượng

Hủy đơn hoặc thanh toán thất bại khi cần
→ hoàn số lượng
```

Không xây dựng nghiệp vụ kho chi tiết như nhà cung cấp, nhiều kho, phiếu nhập xuất, vị trí kho hoặc kiểm kê phức tạp.

Giải pháp kỹ thuật cụ thể có thể thay đổi, miễn không để tồn kho âm hoặc bán vượt số lượng hiện có.

## 5. Cart, Voucher và Order

- Thêm vào giỏ không giữ hàng.
- Checkout luôn đọc lại giá, trạng thái sản phẩm, voucher và tồn kho từ database.
- Frontend không được tự quyết định tổng tiền cuối cùng.
- Voucher được backend kiểm tra điều kiện trước khi áp dụng.
- Order lưu đủ thông tin để đơn cũ vẫn đúng khi sản phẩm thay đổi.
- Customer chỉ được thao tác trên đơn của chính mình.
- Trạng thái đơn được thay đổi qua service và theo luồng nghiệp vụ hợp lệ.

Các trạng thái hoặc enum cụ thể có thể điều chỉnh trong quá trình triển khai.

## 6. Payment

- Hỗ trợ COD và ít nhất một phương thức thanh toán online.
- Order và Payment nên được quản lý tách biệt về mặt nghiệp vụ.
- Không tin trạng thái thanh toán do frontend gửi lên.
- Callback hoặc webhook cần được xác minh theo tài liệu của nhà cung cấp.
- Xử lý callback lặp không được làm thay đổi dữ liệu nhiều lần.
- Khi giao dịch thất bại hoặc hết hạn, hệ thống xử lý Order và tồn kho theo quy tắc đã chọn.

Cách tích hợp cụ thể phụ thuộc cổng thanh toán và có thể thay đổi mà không làm ảnh hưởng lớn đến module Order.

## 7. Review

- Customer chỉ được đánh giá sản phẩm đã mua theo điều kiện của hệ thống.
- Backend kiểm tra quyền đánh giá dựa trên dữ liệu đơn hàng.
- Rating và nội dung phải được validation.
- Có thể cho Admin ẩn hoặc quản lý review nếu cần.

Chi tiết như đánh giá theo OrderItem hay theo cặp User–Product có thể được lựa chọn khi triển khai, miễn chứng minh được khách đã mua hàng và tránh đánh giá trùng không hợp lệ.

## 8. Dashboard

Dashboard chỉ cần các thống kê bán hàng cơ bản:

- Doanh thu.
- Đơn hàng theo trạng thái.
- Doanh thu theo ngày hoặc tháng.
- Giá trị đơn trung bình.
- Sản phẩm bán chạy.

Ưu tiên dùng truy vấn tổng hợp trực tiếp trên `orders` và `order_items`. Chưa cần tạo data warehouse, bảng thống kê riêng hoặc hệ thống báo cáo phức tạp.

## 9. ChatbotAI

- RAG phù hợp với chính sách, tài liệu và nội dung ít thay đổi.
- Giá, tồn kho, đơn hàng và thống kê nên lấy từ service hoặc tool nghiệp vụ.
- Chatbot phải tuân thủ quyền của Guest, Customer và Admin.
- Customer không được xem đơn hàng của người khác.
- Admin chỉ được gọi các chức năng thống kê đã cho phép.
- Không cho AI tự sinh và chạy SQL tùy ý trên database production.

Cách chọn model, vector store, prompt và framework AI có thể thay đổi trong quá trình thử nghiệm.

## 10. Quy tắc làm việc với AI Agent

AI Agent được phép chủ động:

- Tạo class, DTO, repository, service, controller và migration cần thiết.
- Refactor khi phát hiện cấu trúc hiện tại chưa phù hợp.
- Đề xuất cách đơn giản hơn so với tài liệu nếu vẫn giữ đúng nghiệp vụ.
- Thêm test, index, validation hoặc cấu hình phục vụ tính năng đang làm.
- Chọn thư viện phổ biến và tương thích với phiên bản Spring Boot của dự án.

AI Agent không nên:

- Tự ý bỏ chức năng chính đã thống nhất.
- Tạo kiến trúc phức tạp hơn nhu cầu hiện tại.
- Hardcode secret, mật khẩu hoặc API key.
- Thay đổi dữ liệu hoặc migration cũ một cách phá hủy mà không giải thích.
- Viết quá nhiều abstraction khi chưa có nhu cầu sử dụng.

Khi có nhiều cách triển khai hợp lệ, ưu tiên cách:

```text
đơn giản
→ dễ đọc
→ dễ chạy thử
→ dễ thay đổi về sau
```

## 11. Tài liệu là tài liệu sống

Ba file tài liệu trong thư mục `docs` chỉ là định hướng hiện tại.

Trong quá trình vibe code:

- Có thể cập nhật tài liệu sau mỗi thay đổi lớn.
- Không cần bám cứng tên class, tên bảng hoặc thứ tự module nếu thực tế cần thay đổi.
- Quyết định cuối cùng dựa trên code chạy được, dữ liệu đúng và luồng nghiệp vụ hợp lý.
