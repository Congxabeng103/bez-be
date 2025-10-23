package com.poly.bezbe.dto.response.product;


import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime; // Thêm import
import java.util.Map; // Thêm import

@Data
@Builder
public class VariantResponseDTO {
    private Long id;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity; // Đổi tên cho khớp Entity
    private String imageUrl;
    // Map<Tên Thuộc Tính, Giá Trị>, ví dụ: { "Màu sắc": "Đỏ", "Kích cỡ": "L" }
    private Map<String, String> attributes;
    private LocalDateTime createdAt; // Thêm ngày tạo nếu cần
}