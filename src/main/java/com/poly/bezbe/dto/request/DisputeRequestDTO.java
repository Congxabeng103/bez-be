package com.poly.bezbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisputeRequestDTO {
    @NotBlank(message = "Lý do khiếu nại không được để trống")
    private String reason;
}