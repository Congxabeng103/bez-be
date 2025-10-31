package com.poly.bezbe.dto.request.product;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BrandRequestDTO {
    @NotEmpty(message = "Tên danh mục không được để trống")
    private String name;
    private String description;
    private String imageUrl; // <-- THÊM DÒNG NÀY
    private boolean active = true; // Thêm/Sửa đều có thể set trạng thái
}