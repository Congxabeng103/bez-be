package com.poly.bezbe.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UserRequestDTO {
    @NotEmpty(message = "Tên không được để trống")
    private String firstName; // Sửa

    @NotEmpty(message = "Họ không được để trống")
    private String lastName; // Sửa

    @NotEmpty(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;

    // Dùng String để nhận giá trị Enum (vd: "QUAN_TRI_VIEN")
    // Hoặc đổi thành Enum Position nếu bạn muốn validate chặt hơn
    private String position;

    private boolean active; // Cho phép cập nhật trạng thái
}