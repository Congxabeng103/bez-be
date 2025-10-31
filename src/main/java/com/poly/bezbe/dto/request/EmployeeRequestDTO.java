package com.poly.bezbe.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EmployeeRequestDTO { // DTO riêng cho tạo Employee
    @NotEmpty(message = "Tên không được để trống")
    private String firstName; // Sửa

    @NotEmpty(message = "Họ không được để trống")
    private String lastName; // Sửa
    @NotEmpty(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotEmpty(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password; // Mật khẩu ban đầu

    private String phone;

    @NotEmpty(message = "Chức vụ không được để trống")
    private String position; // vd: "NHAN_VIEN_KHO"

    @NotEmpty(message = "Vai trò không được để trống")
    private String role; // "STAFF" hoặc "ADMIN"
}