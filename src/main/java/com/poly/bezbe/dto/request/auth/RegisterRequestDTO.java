package com.poly.bezbe.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class RegisterRequestDTO {
    @NotEmpty(message = "Tên không được để trống")
    private String firstName; // vd: "Công"

    private String lastName; // vd: "Đỗ Thành"

    @Email(message = "Email không đúng định dạng")
    @NotEmpty(message = "Email không được để trống")
    private String email;

    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @NotEmpty(message = "Mật khẩu không được để trống")
    private String password;

    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại phải có 10 số và đúng định dạng VN (09, 03, 07...)")
    private String phone;


}