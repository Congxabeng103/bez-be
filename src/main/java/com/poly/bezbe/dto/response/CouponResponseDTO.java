package com.poly.bezbe.dto.response; // (Hoặc package DTO của bạn)

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CouponResponseDTO {
    private Long id;
    private String code;
    private String description;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer usageLimit; // (0 là không giới hạn)
    private Integer usedCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active; // Trạng thái
    private LocalDateTime createdAt;
}