# LaptopHub Backend

Backend REST API cho website bán laptop và phụ kiện máy tính LaptopHub.

## Stack

- Java 25, Spring Boot 3.5, Maven
- Spring Data JPA, MySQL, Flyway
- Spring Security + JWT (sẽ cấu hình ở giai đoạn `auth`/`security`)

## Yêu cầu môi trường

- JDK 25. Nếu máy có nhiều JDK, đảm bảo `JAVA_HOME` trỏ đúng JDK 25 trước khi build/run (ví dụ trên máy dev hiện tại: `JAVA_HOME="C:\Program Files\Java\jdk-25.0.2"`).

## Cấu hình biến môi trường

Copy `.env.example` thành `.env` và điền giá trị thật (không commit `.env`):

```bash
cp .env.example .env
```

`DB_PASSWORD` bắt buộc phải có (không còn giá trị mặc định trong `application.yml`). Nạp file `.env` vào shell trước khi chạy, ví dụ Git Bash:

```bash
export $(grep -v '^#' .env | xargs)
```

hoặc set từng biến thủ công / qua run configuration của IDE.

## Chạy MySQL bằng Docker (tùy chọn)

Nếu chưa có MySQL sẵn trên máy, dùng `docker-compose.yml` đã cấu hình sẵn:

```bash
docker compose up -d
```

Container đọc `DB_NAME`/`DB_PASSWORD`/`DB_PORT` từ file `.env` cùng thư mục (docker compose tự nạp `.env`).

## Chạy dự án

```bash
./mvnw spring-boot:run
```

## Test

```bash
./mvnw clean test
```

Test hiện tại (`LaptopHubApplicationTests`) loại trừ auto-config DataSource/JPA/Flyway nên chạy được mà không cần MySQL — dùng để xác nhận Spring context load thành công. Từ giai đoạn có entity/repository trở đi sẽ cần MySQL thật để test tích hợp.

## Cấu trúc package

Chia theo module nghiệp vụ dưới gốc `com.laptophub`: `common`, `config`, `security`, `auth`, `user`, `catalog`, `cart`, `order`, `payment`, `voucher`, `review`, `inventory`, `dashboard`, `chatbot`.

## Tài liệu

- [docs/API_CONVENTION.md](docs/API_CONVENTION.md) — quy ước response, error handling, base URL.
- [docs/PROJECT_RULES.md](docs/PROJECT_RULES.md) — bối cảnh nghiệp vụ, ràng buộc kiến trúc, quy tắc làm việc.
- [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) — quy ước schema, kế hoạch bảng theo module.
- [docs/MODULE_PLAN.md](docs/MODULE_PLAN.md) — roadmap các giai đoạn tiếp theo.
