package com.poly.bezbe.dto.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

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
    @Valid
    private List<OptionRequestDTO> options; // Đây là list các thuộc tính
}