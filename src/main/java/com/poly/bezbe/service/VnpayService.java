package com.poly.bezbe.service;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.Map;

public interface VnpayService {

    /**
     * Tạo link thanh toán VNPAY
     */
    String createPaymentUrl(HttpServletRequest req, long orderId, BigDecimal amount);

    /**
     * Lấy dữ liệu VNPAY trả về (từ query params)
     * Dùng cho cả Return URL và IPN URL
     */
    Map<String, String> getVnpayData(HttpServletRequest request);

    /**
     * Xác thực chữ ký IPN
     * @param vnp_Params Map chứa dữ liệu VNPAY trả về
     * @return true nếu chữ ký hợp lệ, false nếu không
     */
    boolean validateIpnSignature(Map<String, String> vnp_Params);

    /**
     * Lấy IP của người dùng
     */
    String getIpAddress(HttpServletRequest request);


}