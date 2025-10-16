package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.auth.UpdatePasswordRequestDTO;
import com.poly.bezbe.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users") // Các API riêng tư, yêu cầu phải đăng nhập
@RequiredArgsConstructor
public class UserController {
    private final AuthenticationService service;

    @PostMapping("/update-password")
    public ResponseEntity<String> updatePassword(@RequestBody UpdatePasswordRequestDTO request) {
        return ResponseEntity.ok(service.updatePassword(request));
    }
}