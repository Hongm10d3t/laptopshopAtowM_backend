package com.laptophub.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

// Không có cleanup scheduler ở đây — dọn token hết hạn/đã revoke lâu ngày
// (nếu cần) là việc của một gói riêng sau này, không phải phạm vi ASU-20.
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Tra theo HASH — không có cột nào lưu token gốc nên không thể (và
    // không được phép) query theo plaintext dù có muốn.
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);

    // clearAutomatically=true: UPDATE hàng loạt đi thẳng xuống DB, bỏ qua
    // persistence context của Hibernate — nếu không clear, một RefreshToken
    // đã load trước đó trong cùng transaction sẽ tiếp tục trả revokedAt cũ
    // (stale) dù DB đã đổi thật.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revokedAt = :revokedAt, r.revokeReason = :reason "
            + "WHERE r.userId = :userId AND r.revokedAt IS NULL")
    int revokeActiveByUserId(@Param("userId") Long userId,
                              @Param("revokedAt") Instant revokedAt,
                              @Param("reason") RevokeReason reason);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revokedAt = :revokedAt, r.revokeReason = :reason "
            + "WHERE r.familyId = :familyId AND r.revokedAt IS NULL")
    int revokeActiveByFamilyId(@Param("familyId") String familyId,
                                @Param("revokedAt") Instant revokedAt,
                                @Param("reason") RevokeReason reason);
}
