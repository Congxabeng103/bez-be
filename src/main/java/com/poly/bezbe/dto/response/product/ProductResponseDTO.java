package com.poly.bezbe.dto.response.product;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime; // Use LocalDateTime if your entity uses it

@Data
@Builder
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price; // Giá gốc
    private String imageUrl;

    // IDs remain the same
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;

    private Long promotionId;
    private String promotionName;
    private BigDecimal salePrice; // Only has value if promotion is valid
    private LocalDateTime createdAt; // Ensure this matches your entity's type
    private boolean active; // Product's active status

    // --- ADD THESE 3 FIELDS ---
    private Boolean isCategoryActive; // null if no category
    private Boolean isBrandActive;    // null if no brand
    private Boolean isPromotionStillValid; // null if no promotion, false if inactive/expired

    private long variantCount;
    private Integer galleryImageCount; // Đếm số ảnh trong album
}