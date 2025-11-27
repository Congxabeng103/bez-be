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
import com.poly.bezbe.repository.OrderRepository;
import com.poly.bezbe.repository.UserRepository;
import com.poly.bezbe.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
    private final OrderRepository orderRepository;
    private final OrderAuditLogRepository orderAuditLogRepository;

    private User getCurrentLoginUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng hiện tại"));
    }

    private UserResponseDTO mapToUserDTO(User user) {
        Integer totalOrders = null;
        BigDecimal totalSpent = BigDecimal.ZERO;
        Integer activityCount = null;

        String fullName = (user.getLastName() != null ? user.getLastName() : "") + " " + (user.getFirstName() != null ? user.getFirstName() : "");

        if (user.getRole() == Role.USER) {
            totalOrders = orderRepository.countByUserId(user.getId());
        } else {
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
                .totalOrders(totalOrders)
                .totalSpent(totalSpent)
                .activityCount(activityCount)
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .dob(user.getDob() != null ? user.getDob().toString() : null)
                .streetAddress(user.getStreetAddress())
                .provinceCode(user.getProvinceCode())
                .provinceName(user.getProvinceName())
                .districtCode(user.getDistrictCode())
                .districtName(user.getDistrictName())
                .wardCode(user.getWardCode())
                .wardName(user.getWardName())
                .build();
    }

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

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> getCustomers(Pageable pageable, String searchTerm, String status) {
        Page<User> userPage = findUsers(Role.USER, null, pageable, searchTerm, status);
        List<UserResponseDTO> dtos = userPage.getContent().stream().map(this::mapToUserDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(dtos, userPage.getNumber(), userPage.getSize(), userPage.getTotalElements(), userPage.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> getEmployees(Pageable pageable, String searchTerm, String status) {
        List<Role> employeeRoles = List.of(Role.ADMIN, Role.STAFF, Role.MANAGER);
        Page<User> userPage = findUsers(null, employeeRoles, pageable, searchTerm, status);
        List<UserResponseDTO> dtos = userPage.getContent().stream().map(this::mapToUserDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(dtos, userPage.getNumber(), userPage.getSize(), userPage.getTotalElements(), userPage.getTotalPages());
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO request) {
        User currentUser = getCurrentLoginUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User: " + id));

        // --- FIX QUYỀN (Cho phép Manager tự sửa mình) ---
        boolean isSelfUpdate = currentUser.getId().equals(targetUser.getId());

        if (currentUser.getRole() == Role.STAFF && !isSelfUpdate) {
            if (targetUser.getRole() != Role.USER) {
                throw new AccessDeniedException("Nhân viên chỉ được phép chỉnh sửa Khách hàng.");
            }
        }

        if (currentUser.getRole() == Role.MANAGER && !isSelfUpdate) {
            // Manager không được sửa Admin hoặc Manager khác
            if (targetUser.getRole() == Role.ADMIN || targetUser.getRole() == Role.MANAGER) {
                throw new AccessDeniedException("Không đủ quyền hạn để chỉnh sửa tài khoản cấp cao hơn hoặc ngang hàng.");
            }
        }
        // ------------------------------------------------

        String email = request.getEmail().trim();
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("Email '" + email + "' đã được sử dụng.");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (userRepository.existsByPhoneAndIdNot(request.getPhone().trim(), id)) {
                throw new DuplicateResourceException("Số điện thoại đã được sử dụng.");
            }
        }

        targetUser.setFirstName(request.getFirstName().trim());
        targetUser.setLastName(request.getLastName().trim());
        targetUser.setEmail(email);
        targetUser.setPhone(request.getPhone());

        // --- FIX QUYỀN KÍCH HOẠT ---
        // Admin: Full quyền
        // Manager: Được sửa active của STAFF và USER. Không được sửa active của chính mình (tự sát).
        if (currentUser.getRole() == Role.ADMIN) {
            targetUser.setActive(request.isActive());
        } else if (currentUser.getRole() == Role.MANAGER) {
            if (targetUser.getRole() == Role.STAFF || targetUser.getRole() == Role.USER) {
                targetUser.setActive(request.isActive());
            }
            // Nếu Manager tự sửa mình -> Giữ nguyên active cũ (không cho tự khóa)
        }
        // Staff: Không làm gì với active

        User updated = userRepository.save(targetUser);
        return mapToUserDTO(updated);
    }

    @Override
    @Transactional
    public UserResponseDTO updateProfile(String userEmail, UpdateProfileRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String newPhone = request.getPhone().trim();
            if (userRepository.existsByPhoneAndIdNot(newPhone, user.getId())) {
                throw new DuplicateResourceException("Số điện thoại đã được sử dụng.");
            }
            user.setPhone(newPhone);
        }

        user.setAvatar(request.getAvatar());
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

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User currentUser = getCurrentLoginUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User: " + id));

        // --- FIX QUYỀN XÓA ---
        // Staff: Cấm tiệt
        if (currentUser.getRole() == Role.STAFF) {
            throw new AccessDeniedException("Nhân viên không có quyền xóa/ngừng hoạt động.");
        }

        // Manager:
        if (currentUser.getRole() == Role.MANAGER) {
            // Cấm xóa Admin hoặc Manager khác
            if (targetUser.getRole() == Role.ADMIN || targetUser.getRole() == Role.MANAGER) {
                throw new AccessDeniedException("Bạn không có quyền ngừng hoạt động tài khoản này.");
            }
            // Được phép xóa STAFF và USER -> OK
        }
        // ---------------------

        targetUser.setActive(false);
        userRepository.save(targetUser);
    }

    @Override
    @Transactional
    public String updatePassword(UpdatePasswordRequestDTO request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadCredentialsException("Mật khẩu cũ không chính xác.");
        }
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        return "Đổi mật khẩu thành công!";
    }

    @Override
    @Transactional
    public UserResponseDTO createEmployee(EmployeeRequestDTO request) {
        User currentUser = getCurrentLoginUser();
        Role newRole;
        try {
            newRole = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Vai trò không hợp lệ: " + request.getRole());
        }

        // --- FIX QUYỀN TẠO ---
        if (currentUser.getRole() == Role.MANAGER) {
            if (newRole != Role.STAFF) {
                throw new AccessDeniedException("Manager chỉ được phép tạo tài khoản STAFF.");
            }
        }
        // ---------------------

        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new DuplicateResourceException("Email '" + request.getEmail().trim() + "' đã được sử dụng.");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (userRepository.existsByPhone(request.getPhone().trim())) {
                throw new DuplicateResourceException("Số điện thoại đã được sử dụng.");
            }
        }

        if (newRole == Role.USER) {
            throw new BusinessRuleException("Không thể tạo Khách hàng (USER) từ API này.");
        }

        User employee = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(newRole)
                .provider(com.poly.bezbe.enums.AuthProvider.LOCAL)
                .isActive(true)
                .build();

        User saved = userRepository.save(employee);
        return mapToUserDTO(saved);
    }

    @Override
    @Transactional
    public void permanentDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + id));

        if (user.getRole() == Role.USER) {
            Integer orderCount = orderRepository.countByUserId(id);
            if (orderCount > 0) {
                throw new IllegalStateException("Không thể xóa vĩnh viễn khách hàng đã có " + orderCount + " đơn hàng.");
            }
        } else {
            Integer activityCount = orderAuditLogRepository.countByStaffId(id);
            if (activityCount > 0) {
                throw new IllegalStateException("Không thể xóa vĩnh viễn nhân viên đã có " + activityCount + " lịch sử hoạt động.");
            }
        }
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
        return mapToUserDTO(updatedUser);
    }
}