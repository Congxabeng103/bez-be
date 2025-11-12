package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.EmployeeRequestDTO;
import com.poly.bezbe.dto.request.UserRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdatePasswordRequestDTO;
import com.poly.bezbe.dto.request.auth.UpdateProfileRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.UserResponseDTO;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.enums.Gender;
// import com.poly.bezbe.enums.Position; // <-- XÓA
import com.poly.bezbe.enums.Role;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.DuplicateResourceException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.UserRepository;
import com.poly.bezbe.service.UserService;
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

    // private final OrderRepository orderRepository;

    private UserResponseDTO mapToUserDTO(User user) {
        Integer totalOrders = 0; // (Logic nghiệp vụ)
        BigDecimal totalSpent = BigDecimal.ZERO; // (Logic nghiệp vụ)

        String fullName = (user.getLastName() != null ? user.getLastName() : "")
                + " " +
                (user.getFirstName() != null ? user.getFirstName() : "");

        return UserResponseDTO.builder()
                .id(user.getId())
                .name(fullName.trim())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                // .position(user.getPosition() != null ? user.getPosition().name() : null) // <-- XÓA
                .joinDate(user.getCreatedAt())
                .active(user.isActive())
                .totalOrders(totalOrders)
                .totalSpent(totalSpent)
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .dob(user.getDob() != null ? user.getDob().toString() : null)
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> getEmployees(Pageable pageable, String searchTerm, String status) {
        // --- SỬA LOGIC: Thêm MANAGER vào danh sách ---
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

        if (!user.getEmail().equalsIgnoreCase(request.getEmail().trim()) &&
                userRepository.existsByEmail(request.getEmail().trim())) {
            throw new DuplicateResourceException("Email '" + request.getEmail().trim() + "' đã được sử dụng.");
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPhone(request.getPhone());
        user.setActive(request.isActive());

        // --- XÓA LOGIC POSITION ---
        // if (request.getPosition() != null && !request.getPosition().isEmpty()) {
        //     try {
        //         user.setPosition(Position.valueOf(request.getPosition()));
        //     } catch (IllegalArgumentException e) {
        //         throw new ResourceNotFoundException("Chức vụ không hợp lệ: " + request.getPosition());
        //     }
        // } else {
        //     user.setPosition(null);
        // }
        // --- KẾT THÚC XÓA ---

        User updated = userRepository.save(user);
        return mapToUserDTO(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponseDTO updateProfile(String userEmail, UpdateProfileRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(request.getPhone());

        if (request.getGender() != null && !request.getGender().isEmpty()) {
            try {
                user.setGender(Gender.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) { /* Bỏ qua */ }
        } else {
            user.setGender(null);
        }
        if (request.getDob() != null && !request.getDob().isEmpty()) {
            try {
                user.setDob(LocalDate.parse(request.getDob()));
            } catch (DateTimeParseException e) { /* Bỏ qua */ }
        } else {
            user.setDob(null);
        }

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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

        // --- XÓA LOGIC POSITION ---
        // Position position;
        // try {
        //     position = Position.valueOf(request.getPosition().toUpperCase());
        // } catch (IllegalArgumentException e) {
        //     throw new ResourceNotFoundException("Chức vụ (Position) không hợp lệ: " + request.getPosition());
        // }
        // --- KẾT THÚC XÓA ---

        User employee = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                // .position(position) // <-- XÓA
                .provider(com.poly.bezbe.enums.AuthProvider.LOCAL)
                .isActive(true)
                .build();

        User saved = userRepository.save(employee);
        return mapToUserDTO(saved);
    }
}