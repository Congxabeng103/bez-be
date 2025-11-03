package com.poly.bezbe.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CartResponseDTO {
    // (Các trường cartId, variantId, productId, productName, imageUrl, attributesDescription... giữ nguyên)
    private Long cartId;
    private Long variantId;
    private Long productId;
    private String productName;
    private String imageUrl;
    private String attributesDescription;

    // --- SỬA LOGIC GIÁ ---
    private BigDecimal currentPrice; // Giá HIỆN TẠI (live)
    private BigDecimal originalPrice; // Giá LÚC THÊM VÀO GIỎ
    private boolean priceChanged; // Cờ báo hiệu có thay đổi
    // --- KẾT THÚC SỬA ---

    private Integer quantity;
    private Integer stockQuantity;
}