package com.poly.bezbe.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CartRequest {
    private Long userId;
    private Long variantId;
    private Integer quantity;
    private BigDecimal price;
}
