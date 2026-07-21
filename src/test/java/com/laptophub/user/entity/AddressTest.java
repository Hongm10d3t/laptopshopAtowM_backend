package com.laptophub.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressTest {

    private Address newAddress() {
        return Address.create(1L, "Nguyen Van A", "0900000000", "Ha Noi", "Cau Giay", "Dich Vong",
                "123 Duong ABC", false);
    }

    @Test
    void create_setsAllFields() {
        Address address = newAddress();

        assertThat(address.getUserId()).isEqualTo(1L);
        assertThat(address.getRecipientName()).isEqualTo("Nguyen Van A");
        assertThat(address.getPhone()).isEqualTo("0900000000");
        assertThat(address.getProvince()).isEqualTo("Ha Noi");
        assertThat(address.getDistrict()).isEqualTo("Cau Giay");
        assertThat(address.getWard()).isEqualTo("Dich Vong");
        assertThat(address.getStreetAddress()).isEqualTo("123 Duong ABC");
        assertThat(address.isDefault()).isFalse();
    }

    @Test
    void create_rejectsNullUserId() {
        assertThatThrownBy(() -> Address.create(null, "A", "0900000000", "HN", "CG", "DV", "123", false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejectsNullRecipientName() {
        assertThatThrownBy(() -> Address.create(1L, null, "0900000000", "HN", "CG", "DV", "123", false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void update_replacesEditableFields() {
        Address address = newAddress();

        address.update("Tran Thi B", "0911111111", "Da Nang", "Hai Chau", "Thanh Khe", "456 Duong XYZ");

        assertThat(address.getRecipientName()).isEqualTo("Tran Thi B");
        assertThat(address.getPhone()).isEqualTo("0911111111");
        assertThat(address.getProvince()).isEqualTo("Da Nang");
        assertThat(address.getDistrict()).isEqualTo("Hai Chau");
        assertThat(address.getWard()).isEqualTo("Thanh Khe");
        assertThat(address.getStreetAddress()).isEqualTo("456 Duong XYZ");
    }

    @Test
    void markAsDefault_and_unmarkAsDefault_toggleFlag() {
        Address address = newAddress();

        address.markAsDefault();
        assertThat(address.isDefault()).isTrue();

        address.unmarkAsDefault();
        assertThat(address.isDefault()).isFalse();
    }
}
