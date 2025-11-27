package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.EmployeeRequestDTO;
import com.poly.bezbe.dto.request.UserRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdateAddressRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdatePasswordRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdateProfileRequestDTO;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // --- CÁC API QUẢN LÝ ---

    // 1. Tạo nhân viên: Admin hoặc Manager đều được
    @PostMapping("/employees")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createEmployee(
            @Valid @RequestBody EmployeeRequestDTO request) {
        UserResponseDTO newEmployee = userService.createEmployee(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newEmployee, "Tạo nhân viên thành công"), HttpStatus.CREATED);
    }

    // 2. Xem khách hàng: Ai cũng được xem (Trừ User thường)
    @GetMapping("/customers")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<UserResponseDTO>>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        // ... (Giữ nguyên logic paging) ...
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<UserResponseDTO> userPage = userService.getCustomers(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(userPage, "Lấy danh sách khách hàng thành công"));
    }

    // 3. Xem nhân viên: Admin và Manager (Manager xem để quản lý Staff)
    @GetMapping("/employees")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<UserResponseDTO>>> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        // ... (Giữ nguyên logic paging) ...
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<UserResponseDTO> userPage = userService.getEmployees(pageable, search, status);
        return ResponseEntity.ok(ApiResponseDTO.success(userPage, "Lấy danh sách nhân viên thành công"));
    }

    // 4. Cập nhật User (Chung cho cả NV và KH): Mở quyền cho cả 3
    // Logic chặn ai sửa ai sẽ nằm ở Service
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request) {
        UserResponseDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedUser, "Cập nhật người dùng thành công"));
    }

    // 5. Ngừng hoạt động (Soft Delete): Admin và Manager
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<Object>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động người dùng thành công"));
    }

    // 6. Xóa cứng: CHỈ ADMIN (Giữ nguyên)
    @DeleteMapping("/permanent-delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Object>> permanentDeleteUser(@PathVariable Long id) {
        userService.permanentDeleteUser(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa vĩnh viễn người dùng thành công"));
    }

    // --- CÁC API PROFILE (Giữ nguyên) ---
    // ... (Giữ nguyên code updateProfile, updatePassword, updateAddress) ...
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponseDTO updatedUser = userService.updateProfile(userEmail, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedUser, "Cập nhật hồ sơ thành công"));
    }

    @PostMapping("/update-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<Object>> updatePassword(@Valid @RequestBody UpdatePasswordRequestDTO request) {
        String message = userService.updatePassword(request);
        return ResponseEntity.ok(ApiResponseDTO.success(null, message));
    }

    @PutMapping("/profile/address")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateAddress(
            @Valid @RequestBody UpdateAddressRequestDTO request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponseDTO updatedUser = userService.updateAddress(userEmail, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedUser, "Cập nhật địa chỉ thành công"));
    }
}