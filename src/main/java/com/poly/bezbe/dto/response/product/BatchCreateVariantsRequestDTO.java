package com.poly.bezbe.dto.response.product;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BatchCreateVariantsRequestDTO {
    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @NotEmpty(message = "Danh sách biến thể không được để trống")
    @Valid // Validate từng phần tử trong list
    private List<CreateVariantRequestDTO> variants;
}