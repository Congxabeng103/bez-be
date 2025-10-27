package com.poly.bezbe.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CartResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private Long variantId;
    private String variantName;
    private Integer quantity;
    private BigDecimal price;
}
