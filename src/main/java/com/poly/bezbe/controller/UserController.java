package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.EmployeeRequestDTO;
import com.poly.bezbe.dto.request.UserRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdatePasswordRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdateProfileRequestDTO;  // <-- 1. Import DTO Profile
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.UserResponseDTO;
import com.poly.bezbe.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder; // <-- 2. Import SecurityContext
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // --- CÁC API QUẢN LÝ (ADMIN) ---

    @PostMapping("/employees")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createEmployee(
            @Valid @RequestBody EmployeeRequestDTO request) {
        UserResponseDTO newEmployee = userService.createEmployee(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newEmployee, "Tạo nhân viên thành công"), HttpStatus.CREATED);
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<UserResponseDTO>>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<UserResponseDTO> userPage = userService.getCustomers(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(userPage, "Lấy danh sách khách hàng thành công"));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<UserResponseDTO>>> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<UserResponseDTO> userPage = userService.getEmployees(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(userPage, "Lấy danh sách nhân viên thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request) {
        UserResponseDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedUser, "Cập nhật người dùng thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id); // Soft delete
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động người dùng thành công"));
    }

    // --- CÁC API CỦA PROFILE (USER TỰ CẬP NHẬT) ---

    /**
     * API Cập nhật Thông tin Profile (Tên, SĐT, Giới tính, Ngày sinh).
     * Endpoint này bị thiếu trong code của bạn.
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO request) {

        // Lấy email của user đang đăng nhập từ Spring Security (từ JWT)
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        UserResponseDTO updatedUser = userService.updateProfile(userEmail, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedUser, "Cập nhật hồ sơ thành công"));
    }

    /**
     * API Cập nhật Mật khẩu.
     * Endpoint này đã có trong code của bạn.
     */
    @PostMapping("/update-password")
    public ResponseEntity<ApiResponseDTO<Object>> updatePassword(@Valid @RequestBody UpdatePasswordRequestDTO request) {
        // Hàm 'updatePassword' trong Service sẽ tự lấy user đang đăng nhập
        String message = userService.updatePassword(request);
        return ResponseEntity.ok(ApiResponseDTO.success(null, message));
    }
}