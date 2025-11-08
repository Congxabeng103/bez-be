package com.poly.bezbe.service.impl;

import com.poly.bezbe.config.VnpayConfig;
import com.poly.bezbe.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service này xử lý tất cả logic nghiệp vụ liên quan đến VNPAY
 * bao gồm tạo link thanh toán, xác thực chữ ký, và gọi API hoàn tiền.
 */
@Service
@RequiredArgsConstructor
public class VnpayServiceImpl implements VnpayService {

    // RestTemplate để gọi API server-to-server (ví dụ: API Hoàn tiền)
    private final RestTemplate restTemplate;

    // Config chứa các key và URL của VNPAY
    private final VnpayConfig vnpayConfig;

    /**
     * Tạo URL thanh toán VNPAY (Cổng VNPAY)
     */
    @Override
    public String createPaymentUrl(HttpServletRequest req, long orderId, BigDecimal amount) {
        // VNPAY yêu cầu số tiền là số nguyên (nhân 100)
        String vnp_Amount = amount.multiply(new BigDecimal("100")).toBigInteger().toString();

        // Tạo mã tham chiếu giao dịch (vnp_TxnRef) duy nhất cho mỗi lần thanh toán
        // Định dạng: {orderId}_{random}
        String vnp_TxnRef = orderId + "_" + UUID.randomUUID().toString().substring(0, 4);
        String vnp_IpAddr = getIpAddress(req);

        // Map chứa các tham số gửi lên VNPAY
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnpayConfig.getVnp_TmnCode());
        vnp_Params.put("vnp_Amount", vnp_Amount);
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");

        // URL VNPAY sẽ gọi về server BE khi user hoàn tất (Luồng 1)
        vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getVnp_ReturnUrl());

        // (Bạn đang comment dòng này và cấu hình IPN cứng trên Merchant, điều này OK)
        // vnp_Params.put("vnp_IpnUrl", vnpayConfig.getVnp_IpnUrl());

        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Cài đặt thời gian
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Cài đặt thời gian hết hạn (ví dụ: 15 phút)
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // --- Tạo Chữ ký (vnp_SecureHash) ---
        // 1. Sắp xếp các tham số theo thứ tự alphabet
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        // 2. Tạo chuỗi hash data (key=value&key=value...)
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                // Build query string
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        // 3. Tạo URL và chữ ký
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnpayConfig.getVnp_ApiUrl() + "?" + queryUrl;
    }

    /**
     * Helper: Lấy tất cả tham số VNPAY trả về (từ returnUrl hoặc IPN)
     */
    @Override
    public Map<String, String> getVnpayData(HttpServletRequest request) {
        Map<String, String> vnp_Params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            vnp_Params.put(paramName, paramValue);
        }
        return vnp_Params;
    }

    /**
     * Xác thực chữ ký VNPAY trả về (Thường dùng cho IPN)
     */
    @Override
    public boolean validateIpnSignature(Map<String, String> vnp_Params) {
        // Lấy chữ ký gốc VNPAY gửi về
        String vnp_SecureHash = vnp_Params.get("vnp_SecureHash");

        // VNPAY yêu cầu: Xóa vnp_SecureHash và vnp_SecureHashType khỏi map trước khi hash
        vnp_Params.remove("vnp_SecureHash");
        vnp_Params.remove("vnp_SecureHashType");

        // Sắp xếp lại, băm, và so sánh với chữ ký gốc
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                hashData.append('&');
            }
        }

        // Xóa dấu '&' cuối cùng
        if (hashData.length() > 0) {
            hashData.deleteCharAt(hashData.length() - 1);
        }

        // Tạo chữ ký mới từ dữ liệu
        String calculatedHash = hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData.toString());

        // So sánh
        return calculatedHash.equals(vnp_SecureHash);
    }

    /**
     * Helper: Lấy IP của client (cần cho VNPAY)
     */
    @Override
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    /**
     * Hàm mã hóa HmacSHA512 (dùng cho cả Thanh toán và Hoàn tiền)
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSha512.init(secretKey);
            byte[] hash = hmacSha512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi mã hóa HmacSHA512", e);
        }
    }

    /**
     * Gửi yêu cầu HOÀN TIỀN (Refund) đến VNPAY
     */
    @Override
    public String requestRefund(
            String vnp_TxnRef,          // Mã giao dịch của shop (đã lưu lúc thanh toán)
            String vnp_TransactionNo,   // Mã giao dịch VNPAY (đã lưu lúc thanh toán)
            String vnp_TransactionDate, // Ngày thanh toán (đã lưu lúc thanh toán)
            BigDecimal vnp_Amount,
            String vnp_CreateBy,
            String vnp_IpAddr
    ) {
        // 1. Chuẩn bị các tham số theo tài liệu VNPAY
        String vnp_RequestId = UUID.randomUUID().toString().substring(0, 15); // Mã request duy nhất
        String vnp_Version = "2.1.0";
        String vnp_Command = "refund";
        String vnp_TmnCode = vnpayConfig.getVnp_TmnCode();
        String vnp_TransactionType = "02"; // 02: Hoàn toàn phần. (Nếu hoàn 1 phần: 03)
        String vnp_Amount_Str = vnp_Amount.multiply(new BigDecimal("100")).toBigInteger().toString();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(new Date()); // Ngày tạo yêu cầu hoàn tiền
        String vnp_OrderInfo = "Hoan tien don hang " + vnp_TxnRef;

        // 2. Tạo chuỗi hash (data string) theo đúng thứ tự VNPAY yêu cầu (dùng dấu |)
        String hashData = String.join("|",
                vnp_RequestId,
                vnp_Version,
                vnp_Command,
                vnp_TmnCode,
                vnp_TransactionType,
                vnp_TxnRef,
                vnp_Amount_Str,
                vnp_TransactionNo,
                vnp_TransactionDate,
                vnp_CreateBy,
                vnp_CreateDate,
                vnp_IpAddr,
                vnp_OrderInfo
        );

        String vnp_SecureHash = hmacSHA512(vnpayConfig.getVnp_HashSecret(), hashData);

        // 3. Tạo JSON request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("vnp_RequestId", vnp_RequestId);
        requestBody.put("vnp_Version", vnp_Version);
        requestBody.put("vnp_Command", vnp_Command);
        requestBody.put("vnp_TmnCode", vnp_TmnCode);
        requestBody.put("vnp_TransactionType", vnp_TransactionType);
        requestBody.put("vnp_TxnRef", vnp_TxnRef);
        requestBody.put("vnp_Amount", vnp_Amount_Str);
        requestBody.put("vnp_TransactionNo", vnp_TransactionNo);
        requestBody.put("vnp_TransactionDate", vnp_TransactionDate);
        requestBody.put("vnp_CreateBy", vnp_CreateBy);
        requestBody.put("vnp_CreateDate", vnp_CreateDate);
        requestBody.put("vnp_IpAddr", vnp_IpAddr);
        requestBody.put("vnp_OrderInfo", vnp_OrderInfo);
        requestBody.put("vnp_SecureHash", vnp_SecureHash);

        // 4. Gửi request POST (JSON) đến VNPAY Refund API
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // Gọi API bằng RestTemplate
            String vnpayResponse = restTemplate.postForObject(
                    vnpayConfig.getVnp_RefundApiUrl(), // URL API Hoàn tiền
                    entity,
                    String.class
            );

            // Log lại kết quả (quan trọng để debug)
            System.out.println("--- VNPAY REFUND RESPONSE ---");
            System.out.println(vnpayResponse);
            System.out.println("-----------------------------");

            return vnpayResponse; // Trả về chuỗi JSON cho OrderService xử lý

        } catch (Exception e) {
            System.err.println("Lỗi khi gọi API Hoàn tiền VNPAY: " + e.getMessage());
            // Trả về một JSON lỗi giả lập để OrderService biết và báo lỗi cho Admin
            return "{\"vnp_ResponseCode\":\"99\",\"vnp_Message\":\"Lỗi hệ thống khi gọi VNPAY\"}";
        }
    }
}