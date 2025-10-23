package com.poly.bezbe.dto.response;


import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
// Bỏ import java.util.List;

@Data
@Builder
public class PromotionResponseDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal discountValue; // %
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private LocalDateTime createdAt;
    // Bỏ List<Long> productIds;
    // Bỏ List<String> productNames;
}