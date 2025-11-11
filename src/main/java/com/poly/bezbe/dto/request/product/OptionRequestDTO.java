// File: com/poly/bezbe/dto/request/product/OptionRequestDTO.java
package com.poly.bezbe.dto.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class OptionRequestDTO {
    @NotBlank
    private String name; // "Màu sắc", "Kích cỡ"

    @NotEmpty
    @Valid
    private List<OptionValueRequestDTO> values; // ["Đỏ", "Xanh"] hoặc ["S", "M"]
}