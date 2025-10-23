package com.poly.bezbe.dto.request;

// File: dto/request/CouponRequestDTO.java

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CouponRequestDTO {
    @NotEmpty(message = "Mã coupon không được để trống")
    private String code;

    private String description;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue; // Ví dụ: 10 (cho 10%) hoặc 50000 (cho 50k)

    // Các trường tùy chọn, thêm validation nếu bắt buộc
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer usageLimit; // Dùng Integer để chấp nhận null nếu không giới hạn

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Future(message = "Ngày kết thúc phải là tương lai") // Đảm bảo ngày kết thúc sau ngày hiện tại
    private LocalDate endDate;

    private boolean active = true;

    // Thêm @AssertTrue để validate endDate > startDate nếu cần
}
