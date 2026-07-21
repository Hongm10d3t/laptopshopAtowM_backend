package com.laptophub.user.entity;

import com.laptophub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

// userId là FK dạng Long phẳng (không @ManyToOne), giống RefreshToken.userId —
// tránh lazy-loading/N+1, khớp open-in-view=false.
@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipient_name", nullable = false, length = 255)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "province", nullable = false, length = 255)
    private String province;

    @Column(name = "district", nullable = false, length = 255)
    private String district;

    @Column(name = "ward", nullable = false, length = 255)
    private String ward;

    @Column(name = "street_address", nullable = false, length = 500)
    private String streetAddress;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    private Address(Long userId, String recipientName, String phone, String province, String district,
                     String ward, String streetAddress, boolean isDefault) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.recipientName = Objects.requireNonNull(recipientName, "recipientName must not be null");
        this.phone = Objects.requireNonNull(phone, "phone must not be null");
        this.province = Objects.requireNonNull(province, "province must not be null");
        this.district = Objects.requireNonNull(district, "district must not be null");
        this.ward = Objects.requireNonNull(ward, "ward must not be null");
        this.streetAddress = Objects.requireNonNull(streetAddress, "streetAddress must not be null");
        this.isDefault = isDefault;
    }

    public static Address create(Long userId, String recipientName, String phone, String province,
                                  String district, String ward, String streetAddress, boolean isDefault) {
        return new Address(userId, recipientName, phone, province, district, ward, streetAddress, isDefault);
    }

    public void update(String recipientName, String phone, String province, String district, String ward,
                        String streetAddress) {
        this.recipientName = Objects.requireNonNull(recipientName, "recipientName must not be null");
        this.phone = Objects.requireNonNull(phone, "phone must not be null");
        this.province = Objects.requireNonNull(province, "province must not be null");
        this.district = Objects.requireNonNull(district, "district must not be null");
        this.ward = Objects.requireNonNull(ward, "ward must not be null");
        this.streetAddress = Objects.requireNonNull(streetAddress, "streetAddress must not be null");
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
    }
}
