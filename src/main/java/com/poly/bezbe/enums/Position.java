package com.poly.bezbe.enums;

/**
 * Định nghĩa các chức vụ (công việc) của nhân viên.
 * Dùng để hiển thị thông tin, không dùng để phân quyền.
 */
public enum Position {

    /**
     * Quản trị viên - Tương ứng với Role.ADMIN.
     */
    QUAN_TRI_VIEN, // "Quản trị viên"

    /**
     * Nhân viên Bán hàng - Tương ứng với Role.STAFF.
     */
    NHAN_VIEN_BAN_HANG, // "Nhân viên Bán hàng"

    /**
     * Nhân viên Kho - Tương ứng với Role.STAFF.
     */
    NHAN_VIEN_KHO // "Nhân viên Kho"

    // (Bạn có thể thêm các chức vụ khác nếu cần)
}