-- Seed baseline thông số kỹ thuật phổ biến cho laptop, category_id = NULL
-- (áp dụng toàn cục) — chưa chắc đã có category nào tồn tại lúc migration
-- này chạy (category được Admin tạo qua API, không seed sẵn).

INSERT INTO specifications (category_id, code, label, unit, group_label, display_order, created_at, updated_at) VALUES
(NULL, 'cpu', 'Bộ xử lý (CPU)', NULL, 'Hiệu năng', 10, NOW(6), NOW(6)),
(NULL, 'gpu', 'Card đồ họa (GPU)', NULL, 'Hiệu năng', 20, NOW(6), NOW(6)),
(NULL, 'ram_type', 'Loại RAM', NULL, 'Hiệu năng', 30, NOW(6), NOW(6)),
(NULL, 'screen_size', 'Kích thước màn hình', 'inch', 'Màn hình', 40, NOW(6), NOW(6)),
(NULL, 'screen_resolution', 'Độ phân giải', NULL, 'Màn hình', 50, NOW(6), NOW(6)),
(NULL, 'battery', 'Dung lượng pin', 'Wh', 'Pin & Kết nối', 60, NOW(6), NOW(6)),
(NULL, 'weight', 'Khối lượng', 'kg', 'Thiết kế', 70, NOW(6), NOW(6)),
(NULL, 'os', 'Hệ điều hành', NULL, 'Thiết kế', 80, NOW(6), NOW(6)),
(NULL, 'ports', 'Cổng kết nối', NULL, 'Pin & Kết nối', 90, NOW(6), NOW(6));
