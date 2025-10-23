package com.poly.bezbe.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ForgotPasswordRequestDTO {
    @Email(message = "Email không đúng định dạng")
    @NotEmpty(message = "Email không được để trống")
    private String email;
}