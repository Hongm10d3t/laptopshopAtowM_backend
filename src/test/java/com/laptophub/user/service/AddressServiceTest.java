package com.laptophub.user.service;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.user.dto.AddressCreateRequest;
import com.laptophub.user.dto.AddressUpdateRequest;
import com.laptophub.user.entity.Address;
import com.laptophub.user.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressRepository);
    }

    private AddressCreateRequest createRequest(Boolean isDefault) {
        return new AddressCreateRequest("Nguyen Van A", "0900000000", "HN", "CG", "DV", "123 Duong ABC", isDefault);
    }

    @Test
    void getOwned_throwsResourceNotFound_whenAddressNotOwnedByUser() {
        when(addressRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getOwned(1L, 10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void create_firstAddressForUser_becomesDefault_regardlessOfRequestedFlag() {
        when(addressRepository.countByUserId(1L)).thenReturn(0L);
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.empty());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address created = addressService.create(1L, createRequest(false));

        assertThat(created.isDefault()).isTrue();
    }

    @Test
    void create_secondAddressWithDefaultFlag_unmarksPreviousDefault() {
        Address previousDefault = Address.create(1L, "Old", "0900000001", "HN", "CG", "DV", "456", true);
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(previousDefault));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address created = addressService.create(1L, createRequest(true));

        assertThat(previousDefault.isDefault()).isFalse();
        assertThat(created.isDefault()).isTrue();
    }

    @Test
    void create_secondAddressWithoutDefaultFlag_staysNonDefault_previousUnaffected() {
        when(addressRepository.countByUserId(1L)).thenReturn(1L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address created = addressService.create(1L, createRequest(false));

        assertThat(created.isDefault()).isFalse();
        verify(addressRepository, org.mockito.Mockito.never()).findByUserIdAndIsDefaultTrue(any());
    }

    @Test
    void update_replacesFieldsOnOwnedAddress() {
        Address address = Address.create(1L, "Old", "0900000001", "HN", "CG", "DV", "456", false);
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));

        Address updated = addressService.update(1L, 5L,
                new AddressUpdateRequest("New", "0922222222", "DN", "HC", "TK", "789"));

        assertThat(updated.getRecipientName()).isEqualTo("New");
        assertThat(updated.getPhone()).isEqualTo("0922222222");
    }

    @Test
    void delete_removesOwnedAddress() {
        Address address = Address.create(1L, "Old", "0900000001", "HN", "CG", "DV", "456", false);
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));

        addressService.delete(1L, 5L);

        verify(addressRepository).delete(address);
    }

    @Test
    void setDefault_unmarksPreviousDefault_andMarksTarget() {
        Address previousDefault = Address.create(1L, "Old", "0900000001", "HN", "CG", "DV", "456", true);
        previousDefault.setId(7L);
        Address target = Address.create(1L, "New", "0900000002", "HN", "CG", "DV", "789", false);
        target.setId(6L);
        when(addressRepository.findByIdAndUserId(6L, 1L)).thenReturn(Optional.of(target));
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(previousDefault));

        Address result = addressService.setDefault(1L, 6L);

        assertThat(previousDefault.isDefault()).isFalse();
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    void setDefault_targetAlreadyDefault_doesNotUnmarkItself() {
        Address target = Address.create(1L, "New", "0900000002", "HN", "CG", "DV", "789", true);
        target.setId(6L);
        when(addressRepository.findByIdAndUserId(6L, 1L)).thenReturn(Optional.of(target));
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(target));

        Address result = addressService.setDefault(1L, 6L);

        assertThat(result.isDefault()).isTrue();
    }
}
