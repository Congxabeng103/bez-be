package com.poly.bezbe.dto.request.auth;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UpdateProfileRequestDTO {
    @NotEmpty(message = "Tên không được để trống")
    private String firstName;

    @NotEmpty(message = "Họ không được để trống")
    private String lastName;
    private String phone;
    private String gender; // (Service sẽ chuyển String này thành Enum)
    private String dob;    // (Service sẽ chuyển String "YYYY-MM-DD" thành LocalDate)
    private String avatar;
}