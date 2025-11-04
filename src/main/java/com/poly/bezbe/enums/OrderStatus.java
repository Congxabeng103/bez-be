package com.poly.bezbe.enums;

public enum OrderStatus
{
    PENDING,
    CONFIRMED,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    DISPUTE,        // Khách báo lỗi/khiếu nại (chưa nhận hàng)
    CANCELLED
}
