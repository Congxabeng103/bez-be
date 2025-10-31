package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.EmployeeRequestDTO;
import com.poly.bezbe.dto.request.UserRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdatePasswordRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdateProfileRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.UserResponseDTO;
import org.springframework.data.domain.Pageable;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến quản lý người dùng
 * (Khách hàng, Nhân viên, và Profile cá nhân).
 */
public interface UserService {

    /**
     * Lấy danh sách Khách hàng (ROLE_USER) có phân trang và lọc.
     */
    PageResponseDTO<UserResponseDTO> getCustomers(Pageable pageable, String searchTerm, String status);

    /**
     * Lấy danh sách Nhân viên (ROLE_ADMIN, ROLE_STAFF) có phân trang và lọc.
     */
    PageResponseDTO<UserResponseDTO> getEmployees(Pageable pageable, String searchTerm, String status);

    /**
     * (Admin) Cập nhật thông tin của một User (thường là nhân viên).
     */
    UserResponseDTO updateUser(Long id, UserRequestDTO request);

    /**
     * (User) Tự cập nhật thông tin profile cá nhân của mình.
     */
    UserResponseDTO updateProfile(String userEmail, UpdateProfileRequestDTO request);

    /**
     * (Admin) Xóa mềm (soft delete) một User (Khách hàng hoặc Nhân viên).
     */
    void deleteUser(Long id);

    /**
     * (User) Tự đổi mật khẩu của mình.
     */
    String updatePassword(UpdatePasswordRequestDTO request);

    /**
     * (Admin) Tạo một tài khoản Nhân viên mới.
     */
    UserResponseDTO createEmployee(EmployeeRequestDTO request);
}