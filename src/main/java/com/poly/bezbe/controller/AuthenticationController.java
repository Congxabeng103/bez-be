package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.auth.*;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.auth.AuthenticationResponseDTO;
import com.poly.bezbe.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<Object>> register(@Valid @RequestBody RegisterRequestDTO request) {
        String message = service.register(request);
        return new ResponseEntity<>(ApiResponseDTO.success(null, message), HttpStatus.CREATED);
    }

    @GetMapping("/activate/{token}")
    public ResponseEntity<ApiResponseDTO<Object>> activateAccount(@PathVariable String token) {
        String message = service.activateAccount(token);
        return ResponseEntity.ok(ApiResponseDTO.success(null, message));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<AuthenticationResponseDTO>> login(@Valid @RequestBody AuthenticationRequestDTO request) {
        AuthenticationResponseDTO data = service.authenticate(request);
        return ResponseEntity.ok(ApiResponseDTO.success(data, "Đăng nhập thành công."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        String message = service.forgotPassword(request);
        return ResponseEntity.ok(ApiResponseDTO.success(null, message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Object>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        String message = service.resetPassword(request);
        return ResponseEntity.ok(ApiResponseDTO.success(null, message));
    }
}