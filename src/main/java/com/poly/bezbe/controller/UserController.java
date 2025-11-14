package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.EmployeeRequestDTO;
import com.poly.bezbe.dto.request.UserRequestDTO;
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
import org.springframework.security.access.prepost.PreAuthorize; // <-- 1. IMPORT
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // --- CÁC API QUẢN LÝ (ADMIN) ---

    // Chỉ Admin
    @PostMapping("/employees")
    @PreAuthorize("hasAuthority('ADMIN')") // <-- 2. THÊM
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createEmployee(
            @Valid @RequestBody EmployeeRequestDTO request) {
        UserResponseDTO newEmployee = userService.createEmployee(request);
        return new ResponseEntity<>(ApiResponseDTO.success(newEmployee, "Tạo nhân viên thành công"), HttpStatus.CREATED);
    }

    // Cả 3 vai trò (Manager/Staff xem khách hàng)
    @GetMapping("/customers")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')") // <-- 3. THÊM
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

    // Chỉ Admin (Quản lý nhân viên)
    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('ADMIN')") // <-- 4. THÊM
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

    // Chỉ Admin
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')") // <-- 5. THÊM
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request) {
        UserResponseDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedUser, "Cập nhật người dùng thành công"));
    }

    // Chỉ Admin
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')") // <-- 6. THÊM
    public ResponseEntity<ApiResponseDTO<Object>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id); // Soft delete
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Ngừng hoạt động người dùng thành công"));
    }

    // --- CÁC API CỦA PROFILE (USER TỰ CẬP NHẬT) ---

    // Chỉ User/Staff/Manager/Admin tự làm (Đã đăng nhập)
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()") // <-- 7. THÊM
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        UserResponseDTO updatedUser = userService.updateProfile(userEmail, request);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedUser, "Cập nhật hồ sơ thành công"));
    }

    // Chỉ User/Staff/Manager/Admin tự làm (Đã đăng nhập)
    @PostMapping("/update-password")
    @PreAuthorize("isAuthenticated()") // <-- 8. THÊM
    public ResponseEntity<ApiResponseDTO<Object>> updatePassword(@Valid @RequestBody UpdatePasswordRequestDTO request) {
        String message = userService.updatePassword(request);
        return ResponseEntity.ok(ApiResponseDTO.success(null, message));
    }
    @DeleteMapping("/permanent-delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Object>> permanentDeleteUser(@PathVariable Long id) {
        // Hàm này đã tồn tại trong UserServiceImpl và đã có logic "thông minh"
        userService.permanentDeleteUser(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa vĩnh viễn người dùng thành công"));
    }
}