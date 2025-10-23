package com.poly.bezbe.dto.response;

// File: dto/response/CouponResponseDTO.java

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
    private BigDecimal discountValue; // Giá trị giảm (có thể là % hoặc số tiền, tùy logic bạn định nghĩa)
    private BigDecimal maxDiscountAmount; // Giảm tối đa (nếu discountValue là %)
    private BigDecimal minOrderAmount; // Đơn tối thiểu
    private int usageLimit; // Lượt dùng tối đa
    private int usedCount; // Đã dùng
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private LocalDateTime createdAt;
}

