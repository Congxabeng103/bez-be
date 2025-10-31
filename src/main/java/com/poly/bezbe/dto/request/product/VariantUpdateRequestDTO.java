package com.poly.bezbe.dto.request.product;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantUpdateRequestDTO {
    @NotEmpty(message = "SKU không được để trống")
    private String sku;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull(message = "Tồn kho không được để trống")
    @Min(0)
    private Integer stockQuantity;

    private String imageUrl;

    private boolean active; // Cho phép Ngừng HĐ / Kích hoạt lại
}
