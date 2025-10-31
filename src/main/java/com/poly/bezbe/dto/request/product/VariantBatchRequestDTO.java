package com.poly.bezbe.dto.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class VariantBatchRequestDTO {
    @NotNull
    private Long productId;

    @Valid // Quan trọng: Báo cho Spring Boot kiểm tra các DTO lồng bên trong
    @NotEmpty
    private List<VariantRequestDTO> variants;
}