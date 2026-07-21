package com.laptophub.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Bulk replace-style: Admin gửi toàn bộ danh sách giá trị mong muốn, service
// thay hẳn set cũ bằng set này (xem ProductSpecValueService.upsertValues).
public record ProductSpecValuesUpsertRequest(

        @NotNull
        List<@Valid SpecValueItem> values) {

    public record SpecValueItem(

            @NotNull(message = "specificationDefinitionId không được để trống")
            Long specificationDefinitionId,

            @NotBlank(message = "Giá trị không được để trống")
            String value) {
    }
}
