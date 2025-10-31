package com.poly.bezbe.dto.request; // (Hoặc package DTO của bạn)

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PromotionRequestDTO {

    @NotEmpty(message = "Tên khuyến mãi không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Phần trăm giảm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "% giảm phải > 0")
    @DecimalMax(value = "100.0", message = "% giảm phải <= 100")
    private BigDecimal discountValue; // % Giảm giá

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    private boolean active = true; // Trạng thái
}