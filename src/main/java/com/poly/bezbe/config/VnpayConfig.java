package com.poly.bezbe.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VnpayConfig {
    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;

    @Value("${vnpay.apiUrl}")
    private String vnp_ApiUrl;

    // Sửa: Đây là API backend /api/v1/payment/vnpay-return
    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;



    // THÊM 2 DÒNG NÀY
    @Value("${vnpay.ipnUrl}")
    private String vnp_IpnUrl; // API IPN (server-to-server)

    @Value("${vnpay.frontendReturnUrl}")
    private String vnp_FrontendReturnUrl; // Link FE
    @Value("${vnpay.refundApiUrl}") // Nó sẽ khớp với tên mới bạn vừa đổi
    private String vnp_RefundApiUrl;

}