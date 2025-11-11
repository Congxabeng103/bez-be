// File: com/poly/bezbe/dto/request/product/OptionValueRequestDTO.java
package com.poly.bezbe.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OptionValueRequestDTO {
    @NotBlank
    private String value; // "Đỏ", "Xanh", "S", "M"
}