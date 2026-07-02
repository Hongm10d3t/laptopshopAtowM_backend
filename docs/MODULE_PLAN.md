# Module Plan — LaptopHub Backend

Thứ tự các giai đoạn dự kiến, mỗi giai đoạn làm độc lập theo đúng [PROJECT_RULES.md](PROJECT_RULES.md). Đánh dấu lại trạng thái khi hoàn thành từng giai đoạn.

| # | Giai đoạn | Package liên quan | Trạng thái |
|---|---|---|---|
| 1 | Khởi tạo project skeleton (build tooling, common, cấu trúc package, docs) | `common`, toàn bộ package rỗng | Xong (2026-07-01) |
| 2 | Chuẩn hóa hạ tầng: env/secret, docker-compose, docs quy ước | — | Xong (2026-07-02) |
| 3 | Security + Auth: entity `User`, đăng ký/đăng nhập, JWT, phân quyền theo role | `security`, `auth`, `user` | Chưa làm |
| 4 | Catalog: sản phẩm, danh mục, thương hiệu, tìm kiếm/lọc cho Guest | `catalog` | Chưa làm |
| 5 | Cart: giỏ hàng cho Customer | `cart` | Chưa làm |
| 6 | Voucher: tạo/áp dụng voucher | `voucher` | Chưa làm |
| 7 | Order + Payment: đặt hàng, chọn COD/chuyển khoản, xem/hủy đơn | `order`, `payment` | Chưa làm |
| 8 | Inventory: quản lý tồn kho, trừ kho khi đặt hàng | `inventory` | Chưa làm |
| 9 | Review: đánh giá sản phẩm | `review` | Chưa làm |
| 10 | Dashboard: thống kê cho Admin (doanh thu, top sản phẩm, nhóm khách hàng) | `dashboard` | Chưa làm |
| 11 | ChatbotAI: tư vấn sản phẩm cho khách, hỗ trợ Admin tra cứu dữ liệu | `chatbot` | Chưa làm |

## Ghi chú

- Giai đoạn 3 (Auth/Security) nên làm trước Catalog vì các API quản trị (Admin) ở mọi module sau đều cần phân quyền.
- Order/Payment phụ thuộc Catalog, Cart, Voucher, Inventory — nên làm sau các module đó.
- Dashboard và Chatbot phụ thuộc dữ liệu thật từ Order/Catalog/User — nên làm cuối cùng.

## Lịch sử thay đổi

- 2026-07-02: Khởi tạo roadmap, ghi nhận giai đoạn 1–2 đã hoàn thành.
