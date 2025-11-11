package com.poly.bezbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelRequestDTO {
    @NotBlank(message = "Lý do hủy không được để trống")
    private String reason;
}