package com.poly.bezbe.dto.request.product;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class VariantRequestDTO {
    @NotEmpty(message = "SKU không được để trống")
    private String sku;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Tồn kho không được để trống")
    @Min(value = 0, message = "Tồn kho không được âm")
    private Integer stockQuantity;

    private String imageUrl;

    @NotEmpty(message = "Phải có ít nhất 1 thuộc tính")
    private List<Long> optionValueIds; // <-- MỚI
}