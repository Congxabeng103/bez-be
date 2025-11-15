package com.poly.bezbe.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductImageRequestDTO {
    @NotBlank
    private String imageUrl;
}