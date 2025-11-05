// (Tạo file tại: dto/response/UserOrderDTO.java)
package com.poly.bezbe.dto.response;

import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.enums.PaymentMethod;
import com.poly.bezbe.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class UserOrderDTO {
    private Long id;
    private String orderNumber;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private int totalItems;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
}