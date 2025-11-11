// (Bạn cần tạo file này trong package dto/response/product)
package com.poly.bezbe.dto.response.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductOptionValueResponseDTO {
    private Long id;
    private String value;
}