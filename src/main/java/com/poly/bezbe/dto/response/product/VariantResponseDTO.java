package com.poly.bezbe.dto.response.product;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class VariantResponseDTO {
    private Long id;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private boolean active; // Trạng thái
    private LocalDateTime createdAt;

    // Map<Tên Thuộc tính, Giá trị>, vd: {"Màu sắc": "Đỏ", "Size": "XL"}
    private Map<String, String> attributes;
    private long orderCount; // <-- THÊM DÒNG NÀY
    // --- THÊM 2 TRƯỜNG NÀY ---
    private BigDecimal salePrice; // Giá sau khi giảm (nếu có)
    private Boolean isPromotionStillValid; // (Trạng thái của KM)
    // --- KẾT THÚC THÊM ---
}