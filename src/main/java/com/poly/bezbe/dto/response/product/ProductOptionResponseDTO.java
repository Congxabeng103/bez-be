// (Bạn cần tạo file này trong package dto/response/product)
package com.poly.bezbe.dto.response.product;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProductOptionResponseDTO {
    private Long id;
    private String name;
    private List<ProductOptionValueResponseDTO> values;
}