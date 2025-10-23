package com.poly.bezbe.dto.response.product;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateVariantRequestDTO {
    @NotEmpty(message = "SKU không được để trống")
    private String sku;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn không được để trống")
    @Min(value = 0, message = "Số lượng tồn không được âm")
    private Integer stockQuantity;

    private String imageUrl;
    // Lưu ý: Không cho sửa thuộc tính (attributeValueIds) khi update
}