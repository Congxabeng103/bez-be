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
    private Long id;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> roles;
    private String avatar;
    // --- THÊM 3 TRƯỜNG NÀY ---
    private String phone;
    private String gender; // (MALE, FEMALE, OTHER)
    private String dob;    // (YYYY-MM-DD)
    private String streetAddress;
    private Integer provinceCode;
    private String provinceName;
    private Integer districtCode;
    private String districtName;
    private Integer wardCode;
    private String wardName;
}