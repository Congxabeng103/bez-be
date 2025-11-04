package com.poly.bezbe.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// TẠO FILE application.properties và ĐIỀN CÁC GIÁ TRỊ NÀY VÀO
// vnpay.tmnCode=YOUR_TMN_CODE
// vnpay.hashSecret=YOUR_HASH_SECRET
// vnpay.apiUrl=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
// vnpay.returnUrl=http://localhost:3000/payment-return (Link FE nhận kết quả)

@Configuration
@Getter
public class VnpayConfig {
    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;

    @Value("${vnpay.apiUrl}")
    private String vnp_ApiUrl;

    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;
}