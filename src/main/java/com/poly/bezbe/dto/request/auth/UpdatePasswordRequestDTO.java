package com.poly.bezbe.dto.request.auth;

import lombok.*;
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class UpdatePasswordRequestDTO { private String currentPassword; private String newPassword; }