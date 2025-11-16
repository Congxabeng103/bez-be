package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.EmployeeRequestDTO;
import com.poly.bezbe.dto.request.UserRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdateAddressRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdatePasswordRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdateProfileRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.UserResponseDTO;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.enums.Gender;
import com.poly.bezbe.enums.Role;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.DuplicateResourceException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.OrderAuditLogRepository;
import com.poly.bezbe.repository.OrderRepository; // <-- THÊM IMPORT
import com.poly.bezbe.repository.UserRepository;
import com.poly.bezbe.service.UserService;
import jakarta.persistence.EntityNotFoundException; // <-- THÊM IMPORT
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository; // <-- THÊM DEPENDENCY
    private final OrderAuditLogRepository orderAuditLogRepository; // <-- THÊM DEPENDENCY
    private UserResponseDTO mapToUserDTO(User user) {

        Integer totalOrders = null;
        BigDecimal totalSpent = BigDecimal.ZERO; // (Tạm thời)
        Integer activityCount = null;

        String fullName = (user.getLastName() != null ? user.getLastName() : "")
                + " " +
                (user.getFirstName() != null ? user.getFirstName() : "");

        // Phân tách logic đếm
        if (user.getRole() == Role.USER) {
            // 1. Nếu là KHÁCH HÀNG -> Đếm Đơn hàng
            totalOrders = orderRepository.countByUserId(user.getId());
            // TODO: Cập nhật logic tính totalSpent khi có
        } else {
            // 2. Nếu là NHÂN VIÊN -> Đếm Hoạt động (Audit Log)
            activityCount = orderAuditLogRepository.countByStaffId(user.getId());
        }

        return UserResponseDTO.builder()
                .id(user.getId())
                .name(fullName.trim())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .joinDate(user.getCreatedAt())
                .active(user.isActive())
                .totalOrders(totalOrders) // (Sẽ là null nếu là Nhân viên)
                .totalSpent(totalSpent)
                .activityCount(activityCount) // (Sẽ là null nếu là Khách hàng)
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .dob(user.getDob() != null ? user.getDob().toString() : null)
                .streetAddress(user.getStreetAddress())
                .provinceCode(user.getProvinceCode())
                .provinceName(user.getProvinceName())
                .districtCode(user.getDistrictCode())
                .districtName(user.getDistrictName())
                .wardCode(user.getWardCode())
                .wardName(user.getWardName())
                // --- KẾT THÚC THÊM MỚI (MAP) ---
                .build();
    }
    // === KẾT THÚC SỬA HÀM MAP ===

    // ... (Các hàm getCustomers, getEmployees, updateUser, updateProfile, deleteUser, updatePassword, createEmployee giữ nguyên) ...
    // (Vì chúng ta đã sửa hàm mapToUserDTO, nên các hàm get... sẽ tự động trả về dữ liệu đúng)

    // (Hàm findUsers đã chuẩn, giữ nguyên)
    private Page<User> findUsers(Role role, List<Role> roles, Pageable pageable, String searchTerm, String status) {
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        boolean activeFilter = !"INACTIVE".equalsIgnoreCase(status);
        String search = searching ? searchTerm.trim() : null;
        String statusFilter = status.toUpperCase();

        if (role != null) {
            return userRepository.findByRoleAndSearch(role, search, statusFilter, activeFilter, pageable);
        } else {
            return userRepository.findByRoleInAndSearch(roles, search, statusFilter, activeFilter, pageable);
        }
    }

    // (Hàm getCustomers đã chuẩn, giữ nguyên)
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> getCustomers(Pageable pageable, String searchTerm, String status) {
        Page<User> userPage = findUsers(Role.USER, null, pageable, searchTerm, status);
        List<UserResponseDTO> dtos = userPage.getContent().stream()
                .map(this::mapToUserDTO)
                .collect(Collectors.toList());
        return new PageResponseDTO<>(dtos, userPage.getNumber(), userPage.getSize(),
                userPage.getTotalElements(), userPage.getTotalPages());
    }

    // (Hàm getEmployees đã chuẩn, giữ nguyên)
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> getEmployees(Pageable pageable, String searchTerm, String status) {
        List<Role> employeeRoles = List.of(Role.ADMIN, Role.STAFF, Role.MANAGER);
        Page<User> userPage = findUsers(null, employeeRoles, pageable, searchTerm, status);
        List<UserResponseDTO> dtos = userPage.getContent().stream()
                .map(this::mapToUserDTO)
                .collect(Collectors.toList());
        return new PageResponseDTO<>(dtos, userPage.getNumber(), userPage.getSize(),
                userPage.getTotalElements(), userPage.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User: " + id));

        String email = request.getEmail().trim();

        // === SỬA LOGIC (Cho nhất quán với các Service khác) ===
        // "Kiểm tra xem email mới có trùng với CỦA NGƯỜI KHÁC không"
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("Email '" + email + "' đã được sử dụng.");
        }
        // === KẾT THÚC SỬA ===

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone());
        user.setActive(request.isActive());

        User updated = userRepository.save(user);
        return mapToUserDTO(updated);
    }

    // (Hàm updateProfile đã chuẩn, giữ nguyên)
    @Override
    @Transactional
    public UserResponseDTO updateProfile(String userEmail, UpdateProfileRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar()); // Lưu avatar mới
        if (request.getGender() != null && !request.getGender().isEmpty()) {
            try { user.setGender(Gender.valueOf(request.getGender().toUpperCase())); }
            catch (IllegalArgumentException e) { /* Bỏ qua */ }
        } else { user.setGender(null); }
        if (request.getDob() != null && !request.getDob().isEmpty()) {
            try { user.setDob(LocalDate.parse(request.getDob())); }
            catch (DateTimeParseException e) { /* Bỏ qua */ }
        } else { user.setDob(null); }

        User updatedUser = userRepository.save(user);
        return mapToUserDTO(updatedUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User: " + id));
        user.setActive(false); // Soft delete
        userRepository.save(user);
    }

    // (Hàm updatePassword đã chuẩn, giữ nguyên)
    @Override
    @Transactional
    public String updatePassword(UpdatePasswordRequestDTO request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng. Lỗi hệ thống."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadCredentialsException("Mật khẩu cũ không chính xác.");
        }
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        return "Đổi mật khẩu thành công!";
    }

    // (Hàm createEmployee đã chuẩn, giữ nguyên)
    @Override
    @Transactional
    public UserResponseDTO createEmployee(EmployeeRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new DuplicateResourceException("Email '" + request.getEmail().trim() + "' đã được sử dụng.");
        }

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Vai trò (Role) không hợp lệ: " + request.getRole());
        }
        if (role == Role.USER) {
            throw new BusinessRuleException("Không thể tạo Khách hàng (USER) từ API này.");
        }

        User employee = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .provider(com.poly.bezbe.enums.AuthProvider.LOCAL)
                .isActive(true)
                .build();

        User saved = userRepository.save(employee);
        return mapToUserDTO(saved);
    }

    // === THÊM HÀM MỚI: XÓA VĨNH VIỄN ===
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void permanentDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + id));

        // KIỂM TRA LOGIC NGHIỆP VỤ (PHÂN TÁCH)
        if (user.getRole() == Role.USER) {
            // 1. Nếu là KHÁCH HÀNG -> Kiểm tra Đơn hàng
            Integer orderCount = orderRepository.countByUserId(id);
            if (orderCount > 0) {
                throw new IllegalStateException("Không thể xóa vĩnh viễn khách hàng đã có " + orderCount + " đơn hàng.");
            }
        } else {
            // 2. Nếu là NHÂN VIÊN -> Kiểm tra Lịch sử hoạt động
            Integer activityCount = orderAuditLogRepository.countByStaffId(id);
            if (activityCount > 0) {
                throw new IllegalStateException("Không thể xóa vĩnh viễn nhân viên đã có " + activityCount + " lịch sử hoạt động.");
            }
        }

        // 3. Nếu không vướng gì (count == 0), tiến hành xóa vĩnh viễn
        userRepository.delete(user);
    }
    @Override
    @Transactional
    public UserResponseDTO updateAddress(String userEmail, UpdateAddressRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setStreetAddress(request.getStreetAddress());
        user.setProvinceCode(request.getProvinceCode());
        user.setProvinceName(request.getProvinceName());
        user.setDistrictCode(request.getDistrictCode());
        user.setDistrictName(request.getDistrictName());
        user.setWardCode(request.getWardCode());
        user.setWardName(request.getWardName());

        User updatedUser = userRepository.save(user);
        // Trả về DTO đã được map đầy đủ thông tin
        return mapToUserDTO(updatedUser);
    }

}