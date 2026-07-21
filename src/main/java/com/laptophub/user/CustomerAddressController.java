package com.laptophub.user;

import com.laptophub.common.ApiResponse;
import com.laptophub.security.CurrentUserProvider;
import com.laptophub.user.dto.AddressCreateRequest;
import com.laptophub.user.dto.AddressResponse;
import com.laptophub.user.dto.AddressUpdateRequest;
import com.laptophub.user.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer/addresses")
public class CustomerAddressController {

    private final AddressService addressService;
    private final CurrentUserProvider currentUserProvider;

    public CustomerAddressController(AddressService addressService, CurrentUserProvider currentUserProvider) {
        this.addressService = addressService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> list() {
        Long userId = currentUserProvider.getCurrentUser().userId();
        List<AddressResponse> responses = addressService.list(userId).stream()
                .map(AddressResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> create(@Valid @RequestBody AddressCreateRequest request) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        var address = addressService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(AddressResponse.from(address)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getOne(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        var address = addressService.getOwned(userId, id);
        return ResponseEntity.ok(ApiResponse.success(AddressResponse.from(address)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody AddressUpdateRequest request) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        var address = addressService.update(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(AddressResponse.from(address)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        addressService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUser().userId();
        var address = addressService.setDefault(userId, id);
        return ResponseEntity.ok(ApiResponse.success(AddressResponse.from(address)));
    }
}
