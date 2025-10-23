package com.poly.bezbe.dto.request.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ResetPasswordRequestDTO {
    @NotEmpty(message = "Token không được để trống")
    private String token;

    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    @NotEmpty(message = "Mật khẩu mới không được để trống")
    private String newPassword;
}