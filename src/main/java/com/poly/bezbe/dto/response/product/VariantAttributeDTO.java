package com.poly.bezbe.dto.response.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VariantAttributeDTO {
    private String attributeName;
    private String value;
}