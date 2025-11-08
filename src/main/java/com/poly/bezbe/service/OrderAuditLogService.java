package com.poly.bezbe.service;

import com.poly.bezbe.entity.Order;
import com.poly.bezbe.entity.User;

public interface OrderAuditLogService {

    void logActivity(Order order, User staff, String description,
                     String field, String oldVal, String newVal);

    // --- THÊM HÀM MỚI NÀY (CHO HỆ THỐNG / KHÁCH HÀNG) ---
    /**
     * Ghi log khi HỆ THỐNG hoặc KHÁCH HÀNG thực hiện
     * @param actorName Tên của tác nhân (VD: "Hệ thống" hoặc "Khách hàng")
     */
    void logActivity(Order order, String actorName, String description,
                     String field, String oldVal, String newVal);
    // --- KẾT THÚC THÊM ---
}