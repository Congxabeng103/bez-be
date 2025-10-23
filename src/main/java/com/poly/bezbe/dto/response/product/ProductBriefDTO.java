package com.poly.bezbe.dto.response.product;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductBriefDTO {
    private Long id;
    private String name;
    private Integer variantCount;
}