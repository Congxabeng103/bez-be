package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.auth.UpdatePasswordRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service; 
    @PostMapping("/update-password")
    public ResponseEntity<ApiResponseDTO<Object>> updatePassword(@Valid @RequestBody UpdatePasswordRequestDTO request) {
        String message = service.updatePassword(request);
        return ResponseEntity.ok(ApiResponseDTO.success(null, message));
    }
}