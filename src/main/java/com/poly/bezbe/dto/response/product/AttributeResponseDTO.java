package com.poly.bezbe.dto.response.product;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AttributeResponseDTO {
    private Long id;
    private String name;
    private List<AttributeValueResponseDTO> values; // Danh sách các giá trị con
}