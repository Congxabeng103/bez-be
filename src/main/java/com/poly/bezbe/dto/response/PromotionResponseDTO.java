package com.poly.bezbe.dto.response; // (Hoặc package DTO của bạn)

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PromotionResponseDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal discountValue; // %
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active; // Trạng thái
    private LocalDateTime createdAt;
    private long productCount; // <-- THÊM TRƯỜNG NÀY (hoặc int)
}