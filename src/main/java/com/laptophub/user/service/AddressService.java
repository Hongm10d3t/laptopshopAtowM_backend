package com.laptophub.user.service;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.user.dto.AddressCreateRequest;
import com.laptophub.user.dto.AddressUpdateRequest;
import com.laptophub.user.entity.Address;
import com.laptophub.user.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<Address> list(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    // Không phân biệt "không tồn tại" và "không phải của user này" — cả 2
    // đều trả RESOURCE_NOT_FOUND, tránh lộ thông tin địa chỉ của người khác.
    public Address getOwned(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // Địa chỉ đầu tiên của user luôn là mặc định, bất kể cờ isDefault gửi lên —
    // Customer cần tối thiểu 1 địa chỉ mặc định để checkout sau này.
    @Transactional
    public Address create(Long userId, AddressCreateRequest request) {
        boolean shouldBeDefault = Boolean.TRUE.equals(request.isDefault()) || addressRepository.countByUserId(userId) == 0;

        if (shouldBeDefault) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(Address::unmarkAsDefault);
        }

        Address address = Address.create(userId, request.recipientName(), request.phone(), request.province(),
                request.district(), request.ward(), request.streetAddress(), shouldBeDefault);
        return addressRepository.save(address);
    }

    @Transactional
    public Address update(Long userId, Long addressId, AddressUpdateRequest request) {
        Address address = getOwned(userId, addressId);
        address.update(request.recipientName(), request.phone(), request.province(), request.district(),
                request.ward(), request.streetAddress());
        return address;
    }

    // Địa chỉ chưa gắn với Order nào ở giai đoạn này nên hard delete là đủ,
    // không cần soft-delete.
    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = getOwned(userId, addressId);
        addressRepository.delete(address);
    }

    @Transactional
    public Address setDefault(Long userId, Long addressId) {
        Address target = getOwned(userId, addressId);

        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .filter(current -> !current.getId().equals(target.getId()))
                .ifPresent(Address::unmarkAsDefault);

        target.markAsDefault();
        return target;
    }
}
