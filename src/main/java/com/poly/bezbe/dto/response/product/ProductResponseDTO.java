package com.poly.bezbe.dto.response.product;


import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data // Lombok
@Builder // Lombok: Hỗ trợ tạo đối tượng dễ dàng
public class ProductResponseDTO {
    private Long id; // ID sản phẩm
    private String name; // Tên sản phẩm
    private String description; // Mô tả
    private BigDecimal price; // Giá gốc
    private String imageUrl; // URL ảnh
    private String categoryName; // Tên danh mục (lấy từ join)
    private String brandName; // Tên thương hiệu (lấy từ join)
    private Long promotionId; // ID của khuyến mãi đang áp dụng (hoặc null)
    private String promotionName; // Tên của khuyến mãi đang áp dụng (hoặc null)
    private BigDecimal salePrice; // Giá bán sau khi đã áp dụng khuyến mãi (hoặc null)
    private LocalDateTime createdAt; // Ngày tạo
}