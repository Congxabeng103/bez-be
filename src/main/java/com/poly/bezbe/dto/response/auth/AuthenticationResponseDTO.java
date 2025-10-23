package com.poly.bezbe.dto.response.auth;

import lombok.*;
import java.util.List; // <-- THÊM IMPORT NÀY

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponseDTO {

    // 1. Sửa 'token' thành 'accessToken'
    private String accessToken;

    // 2. Thêm các trường mới
    private String tokenType;
    private Long id; // Hoặc String nếu ID là UUID
    private String name;
    private String email;
    private List<String> roles;
    private String avatar;
}