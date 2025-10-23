package com.poly.bezbe.dto.request.product;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AttributeRequestDTO {
    @NotEmpty(message = "Tên thuộc tính không được để trống")
    private String name;
}