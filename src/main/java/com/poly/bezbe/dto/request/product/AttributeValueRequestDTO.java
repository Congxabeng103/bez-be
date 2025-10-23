package com.poly.bezbe.dto.request.product;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AttributeValueRequestDTO {
    @NotEmpty(message = "Giá trị không được để trống")
    private String value;
}