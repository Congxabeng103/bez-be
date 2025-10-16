package com.poly.bezbe.controller;


import com.poly.bezbe.dto.request.auth.AuthenticationRequestDTO;
import com.poly.bezbe.dto.request.auth.ForgotPasswordRequestDTO;
import com.poly.bezbe.dto.request.auth.RegisterRequestDTO;
import com.poly.bezbe.dto.request.auth.ResetPasswordRequestDTO;
import com.poly.bezbe.dto.response.auth.AuthenticationResponseDTO;
import com.poly.bezbe.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth") // Các API công khai, không cần đăng nhập
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(service.register(request));
    }

    @GetMapping("/activate/{token}")
    public ResponseEntity<String> activateAccount(@PathVariable String token) {
        return ResponseEntity.ok(service.activateAccount(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDTO> login(@RequestBody AuthenticationRequestDTO request) {
        return ResponseEntity.ok(service.authenticate(request));
    }



    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        return ResponseEntity.ok(service.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        return ResponseEntity.ok(service.resetPassword(request));
    }
}