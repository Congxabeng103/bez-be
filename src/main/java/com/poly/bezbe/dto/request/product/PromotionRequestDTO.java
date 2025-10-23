package com.poly.bezbe.dto.request.product;


import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PromotionRequestDTO { // Đổi tên từ CampaignRequestDTO cho khớp Entity
    @NotEmpty(message = "Tên khuyến mãi không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Phần trăm giảm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Phần trăm giảm phải > 0")
    @DecimalMax(value = "100.0", message = "Phần trăm giảm phải <= 100")
    private BigDecimal discountValue; // % Giảm giá

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Future(message = "Ngày kết thúc phải là tương lai")
    private LocalDate endDate;

    private boolean active = true;

    // Không có productIds
    // Thêm @AssertTrue để validate endDate > startDate nếu cần
}