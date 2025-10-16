package com.poly.bezbe.dto.request.auth;


import lombok.*;
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ResetPasswordRequestDTO { private String token; private String newPassword; }