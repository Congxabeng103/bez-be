package com.poly.bezbe.dto.request.auth;

import lombok.*;
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class AuthenticationRequestDTO { private String email; private String password; }