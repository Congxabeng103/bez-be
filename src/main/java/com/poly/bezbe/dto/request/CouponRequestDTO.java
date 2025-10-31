package com.poly.bezbe.dto.request; // (Hoặc package DTO của bạn)

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CouponRequestDTO {

    @NotEmpty(message = "Mã coupon không được để trống")
    @Size(min = 3, max = 50, message = "Mã coupon phải từ 3 đến 50 ký tự")
    private String code;

    private String description;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue; // Giá trị giảm (tiền mặt, vd: 50000)

    @DecimalMin(value = "0.0", message = "Giảm tối đa phải lớn hơn 0")
    private BigDecimal maxDiscountAmount; // (Có thể null)

    @DecimalMin(value = "0.0", message = "Đơn tối thiểu phải lớn hơn hoặc bằng 0")
    private BigDecimal minOrderAmount; // (Có thể null)

    @Min(value = 1, message = "Giới hạn lượt dùng phải lớn hơn 0")
    private Integer usageLimit; // (Có thể null - 0 nghĩa là không giới hạn)

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    // (Bỏ @Future nếu bạn muốn tạo coupon có hiệu lực ngay)
    private LocalDate endDate;

    private boolean active = true; // Trạng thái
}