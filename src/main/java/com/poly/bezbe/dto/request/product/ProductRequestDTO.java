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
    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price; // <-- GIÁ GỐC CỦA SẢN PHẨM CHA
    private String description;
    private String imageUrl;
    private Long categoryId;
    private Long brandId;
    private Long promotionId;

    private boolean active = true; // <-- THÊM TRƯỜNG NÀY
    @Valid
    private List<OptionRequestDTO> options; // Đây là list các thuộc tính
}