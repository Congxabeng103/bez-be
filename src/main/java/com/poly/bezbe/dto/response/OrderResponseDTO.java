package com.poly.bezbe.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponseDTO {
    private Long orderId;
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private String customerName;
    private String phone;
    private String address;
}