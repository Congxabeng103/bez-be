package com.poly.bezbe.dto.request.product;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequestDTO {
    @NotEmpty(message = "Tên sản phẩm không được để trống")
    private String name;
    private String description;
    private String imageUrl;
    private Long categoryId;
    private Long brandId;
    private Long promotionId;

    private boolean active = true; // <-- THÊM TRƯỜNG NÀY
}