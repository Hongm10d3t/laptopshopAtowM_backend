package com.laptophub.user.repository;

import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.user.EmailNormalizer;
import com.laptophub.user.entity.Address;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Chạy trên MySQL thật (Replace.NONE), giống UserRepositoryTest — project
// chưa có embedded test DB và migration dùng cú pháp riêng của MySQL.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    private Long newUserId(String email) {
        User user = userRepository.saveAndFlush(
                User.create(EmailNormalizer.normalize(email), "hash", "Name", null, UserRole.CUSTOMER));
        return user.getId();
    }

    @Test
    void findByUserIdOrderByIsDefaultDescCreatedAtDesc_returnsDefaultFirst() {
        Long userId = newUserId("addr-order@example.com");
        Address nonDefault = addressRepository.saveAndFlush(
                Address.create(userId, "A", "0900000000", "HN", "CG", "DV", "123", false));
        Address defaultAddress = addressRepository.saveAndFlush(
                Address.create(userId, "B", "0900000001", "HN", "CG", "DV", "456", true));

        List<Address> found = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);

        assertThat(found).extracting(Address::getId)
                .containsExactly(defaultAddress.getId(), nonDefault.getId());
    }

    @Test
    void findByIdAndUserId_returnsEmpty_whenAddressBelongsToAnotherUser() {
        Long ownerId = newUserId("owner@example.com");
        Long strangerId = newUserId("stranger@example.com");
        Address address = addressRepository.saveAndFlush(
                Address.create(ownerId, "A", "0900000000", "HN", "CG", "DV", "123", false));

        Optional<Address> asOwner = addressRepository.findByIdAndUserId(address.getId(), ownerId);
        Optional<Address> asStranger = addressRepository.findByIdAndUserId(address.getId(), strangerId);

        assertThat(asOwner).isPresent();
        assertThat(asStranger).isEmpty();
    }

    @Test
    void countByUserId_reflectsSavedAddresses() {
        Long userId = newUserId("count@example.com");
        assertThat(addressRepository.countByUserId(userId)).isZero();

        addressRepository.saveAndFlush(Address.create(userId, "A", "0900000000", "HN", "CG", "DV", "123", false));

        assertThat(addressRepository.countByUserId(userId)).isEqualTo(1L);
    }

    @Test
    void findByUserIdAndIsDefaultTrue_returnsOnlyDefaultAddress() {
        Long userId = newUserId("default@example.com");
        addressRepository.saveAndFlush(Address.create(userId, "A", "0900000000", "HN", "CG", "DV", "123", false));
        Address defaultAddress = addressRepository.saveAndFlush(
                Address.create(userId, "B", "0900000001", "HN", "CG", "DV", "456", true));

        Optional<Address> found = addressRepository.findByUserIdAndIsDefaultTrue(userId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(defaultAddress.getId());
    }
}
