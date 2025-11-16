package com.poly.bezbe.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponseDTO {
    private Long id;
    private String name; // (firstName + " " + lastName)
    private String email;
    private String phone;
    private String avatar;
    private String role; // (ADMIN, USER, STAFF)
    private String position; // Chức vụ (QUAN_TRI_VIEN, ...)
    // private String department; // Bạn yêu cầu bỏ
    private LocalDateTime joinDate; // (là createdAt)
    private boolean active; // Trạng thái (active/inactive)


    // --- THÊM 2 TRƯỜNG NÀY ---
    private String gender; // (vd: "MALE", "FEMALE", "OTHER")
    private String dob;    // (vd: "1990-10-25")
    // Các trường tính toán (chủ yếu cho Khách hàng)
    // Sẽ là 0 trừ khi bạn implement logic tính toán
    private Integer totalOrders;

    /** Dùng cho Khách hàng (USER): Tổng tiền đã chi */
    private BigDecimal totalSpent;

    /** Dùng cho Nhân viên (STAFF/ADMIN/MANAGER): Đếm số hoạt động đã ghi log */
    private Integer activityCount; // <-- THÊM TRƯỜNG NÀY
    // --- KẾT THÚC SỬA ---
    // --- BẮT ĐẦU THÊM MỚI ---
    private String streetAddress;
    private Integer provinceCode;
    private String provinceName;
    private Integer districtCode;
    private String districtName;
    private Integer wardCode;
    private String wardName;
}