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

// --- THÊM HÀM MỚI NÀY ---
    /**
     * Gửi yêu cầu hoàn tiền đến VNPAY
     * @param vnp_TxnRef Mã giao dịch của shop (VD: 26_d325)
     * @param vnp_TransactionNo Mã giao dịch của VNPAY (lưu lúc IPN)
     * @param vnp_TransactionDate Ngày thanh toán (lưu lúc IPN)
     * @param vnp_Amount Số tiền hoàn
     * @param vnp_CreateBy Tên người thực hiện (VD: admin@gmail.com)
     * @param vnp_IpAddr IP người thực hiện
     * @return String JSON response từ VNPAY
     */
    String requestRefund(
            String vnp_TxnRef,
            String vnp_TransactionNo,
            String vnp_TransactionDate,
            BigDecimal vnp_Amount,
            String vnp_CreateBy,
            String vnp_IpAddr
    );
    // --- KẾT THÚC HÀM MỚI ---
}