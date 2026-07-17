# Auth–Security–User Contract — Gói ASU-00

Tài liệu này chốt **ranh giới ownership** và **contract** giữa 3 module
`auth`, `security`, `user` trước khi viết code nghiệp vụ (gói ASU-01 trở đi).
Không chứa Java code, migration hay dependency — chỉ quyết định kiến trúc và
lý do. Khi code (ASU-01+) khác với tài liệu này, phải cập nhật tài liệu hoặc
ghi rõ lý do thay đổi (theo `PROJECT_RULES.md` mục 9).

Áp dụng cùng với `PROJECT_RULES.md`, `API_CONVENTION.md`, `DATABASE_DESIGN.md`
— tài liệu này không lặp lại, chỉ bổ sung phần chi tiết riêng cho Auth/Security/User.

## 1. Ranh giới ownership (module boundaries)

### `user` sở hữu
- Entity `User` (bảng `users`): `id`, `email`, `passwordHash`, `fullName`,
  `phone`, `role`, `status`, kế thừa `BaseEntity` (`createdAt`/`updatedAt`).
- Entity `Address` (bảng `addresses`).
- `UserRepository`.
- `UserService` — tạo user mới (được `auth` gọi lúc đăng ký), tìm theo email,
  đổi mật khẩu (được `auth` gọi lúc change-password), đổi `status`
  (Admin khóa/mở — thuộc gói code User, không phải ASU-00), cập nhật hồ sơ.
- `auth` và `security` **không được** tự ý query/sửa `UserRepository` — phải
  đi qua `UserService`, đúng `PROJECT_RULES.md` mục 2 ("Module nên giao tiếp
  qua service; hạn chế module này sửa repository của module khác").

### `auth` sở hữu
- Entity `RefreshToken` (bảng `refresh_tokens`) — thuộc luồng phiên đăng nhập,
  không phải hồ sơ user nên không đặt trong `user`.
- `AuthService`: register, login, refresh, logout, logout-all, change-password
  (điều phối — gọi `UserService` để đổi mật khẩu thật, gọi `security` để sinh
  access token).
- Toàn bộ endpoint `/auth/**`.

### `security` sở hữu
- `SecurityConfig` (đã có ở `com.laptophub.security.SecurityConfig`, sẽ mở
  rộng thêm JWT filter khi code ASU-01).
- `JwtTokenProvider`/`JwtService` — sinh & verify JWT bằng JJWT, đọc claims.
- `JwtAuthenticationFilter` — parse header `Authorization`, set `SecurityContext`.
- `CustomUserDetailsService` (implements `UserDetailsService` của Spring
  Security) — gọi `UserService` để load user theo email, không tự query DB.
- Cấu hình `@PreAuthorize`/method security.
- Không chứa business logic đăng ký/đăng nhập — chỉ hạ tầng xác thực/phân quyền.

### Chiều phụ thuộc

```text
security --> user      (đọc user để authenticate/authorize)
auth     --> user      (đăng ký tạo user, đổi mật khẩu)
auth     --> security  (sinh JWT sau khi xác thực thành công)
user     --> (không phụ thuộc auth/security)
```

`user` độc lập, không biết gì về JWT/token/refresh token — tránh phụ thuộc vòng.

## 2. Role và Status

- `Role`: `ADMIN`, `CUSTOMER`. Guest không lưu DB, không có role trong hệ
  thống (khớp `DATABASE_DESIGN.md` mục 2 "Users hỗ trợ tối thiểu ADMIN và CUSTOMER").
- `UserStatus`: `ACTIVE`, `BLOCKED`.
  - `BLOCKED`: không login được — trả lỗi `ACCOUNT_BLOCKED` (mục 8), không
    phải `UNAUTHENTICATED` chung chung, để FE hiển thị đúng lý do.
  - Refresh token của user `BLOCKED` cũng bị từ chối ở bước `/auth/refresh`.
  - Đổi `status` là việc của gói code User (Admin thao tác), ASU-00 chỉ chốt
    2 giá trị enum để `auth`/`security` dùng khi kiểm tra điều kiện login.

## 3. JWT — JJWT 0.12.6

- **JJWT là thư viện JWT duy nhất trong project** (đã có trong `pom.xml`:
  `jjwt-api`/`jjwt-impl`/`jjwt-jackson` phiên bản `0.12.6`) — không thêm thư
  viện JWT nào khác.
- Thuật toán: **HS256** (HMAC-SHA256), secret lấy từ biến môi trường
  `JWT_SECRET` (đã khai trong `.env.example`).
  - ⚠️ Giá trị `JWT_SECRET=changeme` hiện tại **không đủ dài** cho HS256 (yêu
    cầu tối thiểu 256-bit / 32 byte). Phải thay bằng secret ngẫu nhiên đủ dài
    trước khi test/triển khai thật — việc này để lại cho ASU-01, ASU-00 chỉ
    ghi nhận yêu cầu.
- **Access token claims**:
  - `sub`: userId (string)
  - `email`: email đã normalize (không phải dữ liệu bí mật — tiện cho client
    hiển thị mà không cần gọi thêm API; cập nhật lại so với bản ASU-00 gốc,
    khớp `AccessTokenService` đã triển khai và đã có test khóa hành vi)
  - `role`: `ADMIN` | `CUSTOMER`
  - `jti`: UUID định danh token (phục vụ audit/log, **không** dùng để
    blacklist ở ASU-00)
  - `iat`, `exp`: chuẩn JWT
  - Không nhét password, phone, address hay dữ liệu nhạy cảm khác vào claims.
- **TTL**:
  - Access token: **15 phút**.
  - Refresh token: **30 ngày**.
- Access token là stateless — **không có cơ chế blacklist** ở ASU-00 (xem lý
  do ở mục 7).

## 4. Refresh token — opaque, chỉ lưu hash

- Refresh token **không phải JWT** — là chuỗi ngẫu nhiên (256-bit random,
  encode base64url).
- Server chỉ lưu **hash** (SHA-256) của refresh token trong bảng
  `refresh_tokens`, **không bao giờ lưu plaintext**.
- Schema bảng `refresh_tokens` (thuộc `auth`, bổ sung chi tiết cho
  `DATABASE_DESIGN.md`; đã triển khai ở migration `V2`, nhiều hơn bản tối
  thiểu ASU-00 gốc — thêm `family_id`/`replaced_by_token_id`/`revoke_reason`
  để hỗ trợ reuse detection theo family thay vì chỉ revoke từng token lẻ):

```text
id
user_id
token_hash            UNIQUE
family_id             nhóm các token cùng 1 chuỗi rotation
expires_at
revoked_at            (null nếu còn hiệu lực)
replaced_by_token_id  self-FK, set khi bị rotate
revoke_reason         LOGOUT | LOGOUT_ALL | ROTATED | REUSE_DETECTED | NEW_LOGIN
created_at
```

- **Quyết định: chỉ 1 phiên tại 1 thời điểm** (không multi-device ở ASU-00) —
  mỗi user tối đa 1 refresh token có `revoked_at IS NULL`. Login mới phải
  revoke token cũ của user đó trong cùng transaction trước khi tạo token mới.
- **Quyết định: truyền qua httpOnly Cookie** — refresh token set qua
  `Set-Cookie` với `HttpOnly; Secure; SameSite=Lax`, `Path=/api/v1/auth` để
  giới hạn phạm vi gửi cookie. Access token vẫn trả trong JSON body (không
  cookie) để FE tự đính `Authorization: Bearer <token>`.
  - Khớp với `CorsConfig` hiện tại đã bật `allowCredentials(true)`.
  - Lưu ý dev: FE (`localhost:3000`/`5173`) và BE (`localhost:8080`) khác
    port → trình duyệt coi là cross-site, `SameSite=Lax` vẫn gửi cookie được
    với request cùng site cha (`localhost`) nhưng `Secure` yêu cầu HTTPS —
    localhost thường không có HTTPS. Cách xử lý cụ thể (đổi `SameSite=None`
    tạm thời cho dev, hay proxy FE qua BE) để lại quyết định lúc code ASU-01.

## 5. Logout / Logout-all / Rotation / Reuse detection

- **Rotation**: mỗi lần `/auth/refresh` thành công, refresh token cũ bị
  revoke (`revoked_at = now()`) ngay lập tức, tạo refresh token mới thay thế.
  Vì chỉ hỗ trợ 1 phiên nên đây là revoke-and-replace đơn giản, không cần giữ
  full rotation chain.
- **Reuse detection**: nếu refresh token gửi lên có `revoked_at` khác null
  (đã bị dùng/rotate hoặc đã logout trước đó) → coi là dấu hiệu token bị lộ →
  từ chối request, đồng thời revoke nốt token hiện có (nếu có) của user đó,
  buộc đăng nhập lại. Trả `UNAUTHENTICATED` (401), không tiết lộ chi tiết lý
  do cho client.
- **Logout**: revoke refresh token hiện tại của user, xóa cookie
  (`Set-Cookie` với `Max-Age=0`). Access token không bị thu hồi — hết hạn tự
  nhiên theo TTL 15 phút.
- **Logout-all**: vì chỉ tối đa 1 refresh token/user nên hiện tại tương đương
  `logout`. Vẫn giữ endpoint `POST /auth/logout-all` riêng để tương thích khi
  sau này mở multi-device (ASU sau) mà không phải đổi contract FE.

## 6. Access token sau khi đổi mật khẩu

- **Quyết định: access token hết hiệu lực tự nhiên theo TTL (15 phút),
  không blacklist.**
- Lý do: JWT stateless; thêm blacklist cần thêm storage (Redis/bảng DB riêng)
  — tăng độ phức tạp không cần thiết ở MVP. TTL 15 phút đủ ngắn để giới hạn
  rủi ro trong lúc chờ hết hạn tự nhiên.
- Bù lại: đổi mật khẩu **phải** revoke refresh token hiện tại của user (giống
  `logout`) — ngăn việc lấy access token mới sau khi mật khẩu đã đổi. Access
  token đang hiệu lực (nếu đã bị lộ trước đó) vẫn dùng được tối đa 15 phút còn
  lại — rủi ro được chấp nhận ở mức MVP, không cần cơ chế khác.

## 7. Endpoint, quyền truy cập, HTTP status, error code

Base path thật: `/api/v1/auth/**` (xem mục 8 — controller chỉ khai `/auth/**`).

| Method | Path | Quyền | Success | Error chính |
|---|---|---|---|---|
| POST | `/auth/register` | public | 201 | `VALIDATION_ERROR` 400, `EMAIL_ALREADY_EXISTS` 409 |
| POST | `/auth/login` | public | 200 | `VALIDATION_ERROR` 400, `INVALID_CREDENTIALS` 401, `ACCOUNT_BLOCKED` 403 |
| POST | `/auth/refresh` | public (đọc cookie) | 200 | `UNAUTHENTICATED` 401 (thiếu/hết hạn/reuse) |
| POST | `/auth/logout` | authenticated | 204 | `UNAUTHENTICATED` 401 |
| POST | `/auth/logout-all` | authenticated | 204 | `UNAUTHENTICATED` 401 |
| POST | `/auth/change-password` | authenticated | 200 | `VALIDATION_ERROR` 400, `INVALID_CREDENTIALS` 401 (sai mật khẩu cũ) |

Error code mới cần bổ sung vào `ErrorCode.java` khi code ASU-01 (chỉ chốt tên
ở đây, chưa sửa code):

```text
EMAIL_ALREADY_EXISTS   409
INVALID_CREDENTIALS    401
ACCOUNT_BLOCKED         403
```

(`UNAUTHENTICATED`, `VALIDATION_ERROR` đã có sẵn trong `ErrorCode.java` hiện tại.)

Response body `login`/`refresh` (JSON, không gồm refresh token vì đã nằm ở cookie):

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "accessToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "timestamp": "2026-07-16T12:00:00Z"
}
```

## 8. Quy tắc không lặp `/api/v1` trong controller

- `application.yml` đã set `server.servlet.context-path: /api/v1` → context
  path bị cắt trước khi request vào `DispatcherServlet`.
- Controller chỉ khai phần sau, ví dụ `@RequestMapping("/auth")` — **không**
  viết `@RequestMapping("/api/v1/auth")` (sẽ thành `/api/v1/api/v1/auth`, sai).
- Áp dụng tương tự cho `SecurityConfig.requestMatchers(...)` — matcher cũng
  không có tiền tố `/api/v1` (đã đúng ở `SecurityConfig` hiện tại:
  `"/health", "/auth/**", "/public/**"`).

## 9. Còn để mở cho ASU-01 (implementation, không thuộc phạm vi ASU-00)

- Entity `User`, `RefreshToken` và migration Flyway tương ứng.
- `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetailsService`.
- `AuthController`, `AuthService`, DTO đăng ký/đăng nhập/đổi mật khẩu.
- Bổ sung 3 mã lỗi mới vào `ErrorCode.java` (mục 7).
- Thay `JWT_SECRET=changeme` bằng secret đủ dài trong `.env` local trước khi
  test thật (mục 3).
- Chốt `SameSite`/`Secure` cụ thể cho cookie refresh token khi biết domain
  dev/prod thật (mục 4).
