package com.poly.bezbe.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VnpayResponseDTO {
    private String status;
    private String message;
    private String paymentUrl;
}