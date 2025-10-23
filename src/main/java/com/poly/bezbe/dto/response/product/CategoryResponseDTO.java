package com.poly.bezbe.dto.response.product;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponseDTO {
    private Long id;
    private String name;
    // Thêm các trường khác nếu cần
}