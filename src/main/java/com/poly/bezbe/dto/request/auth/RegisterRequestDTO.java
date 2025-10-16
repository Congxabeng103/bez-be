package com.poly.bezbe.dto.request.auth;

import lombok.*;
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class RegisterRequestDTO { private String firstName; private String lastName; private String email; private String password; }

// File: AuthenticationRequest.java