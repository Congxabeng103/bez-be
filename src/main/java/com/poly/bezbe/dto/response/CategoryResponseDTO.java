package com.poly.bezbe.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String imageUrl; // <-- THÊM DÒNG NÀY
    private long productCount; // <-- THÊM DÒNG NÀY
    private boolean active;
}