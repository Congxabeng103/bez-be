package com.poly.bezbe.dto.response.product;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BrandResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String imageUrl; // <-- THÊM DÒNG NÀY
    private boolean active;
    private long productCount; // <-- THÊM DÒNG NÀY
}