package com.poly.bezbe.dto.response.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductImageResponseDTO {
    private Long id;
    private String imageUrl;
}