package com.poly.bezbe.dto.request.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class UpdatePasswordRequestDTO {
    @NotEmpty(message = "Mật khẩu hiện tại không được để trống")
    private String currentPassword;

    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    @NotEmpty(message = "Mật khẩu mới không được để trống")
    private String newPassword;
}