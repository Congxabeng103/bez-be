package com.poly.bezbe.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminOrderItemDTO {
    private Long variantId;
    private String productName;
    private String variantInfo; // (Vd: "Đen, Size L")
    private Integer quantity;
    private BigDecimal price; // Giá tại thời điểm bán
    private String imageUrl;
}