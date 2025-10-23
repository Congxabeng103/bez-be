package com.poly.bezbe.dto.response.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttributeValueResponseDTO {
    private Long id;
    private String value;
}