package com.laptophub.user.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

// Không equals/hashCode: dùng identity mặc định của Object để tránh rủi ro
// so sánh field trên entity JPA (proxy, trạng thái transient) — cũng đảm bảo
// passwordHash không thể lọt vào so sánh bằng nhau.
@Entity
@Table(name = "users")
@Getter
@ToString(exclude = "passwordHash")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// kế thừa BaseEntity
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    private User(String email, String passwordHash, String fullName, String phone, UserRole role) {
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.phone = phone;
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.status = UserStatus.ACTIVE;
    }

    public static User create(String email, String passwordHash, String fullName, String phone, UserRole role) {
        return new User(email, passwordHash, fullName, phone, role);
    }

    // Phục vụ mở/khóa tài khoản
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void block() {
        this.status = UserStatus.BLOCKED;
    }

    // Phuc vụ thay đổi mật khẩu (hash) của người dùng. Lưu ý: không lưu mật khẩu
    // gốc.
    public void changePasswordHash(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("newPasswordHash must not be null or blank");
        }
        this.passwordHash = newPasswordHash;
    }

    // Customer tự sửa hồ sơ (fullName/phone) — không đổi email/role/status qua
    // đường này. phone nullable giống cột DB nên không requireNonNull.
    public void updateProfile(String fullName, String phone) {
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.phone = phone;
    }
}
