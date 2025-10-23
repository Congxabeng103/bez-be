package com.poly.bezbe.service;

import com.poly.bezbe.config.JwtService;
import com.poly.bezbe.dto.request.auth.*;
import com.poly.bezbe.dto.response.auth.AuthenticationResponseDTO;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.enums.AuthProvider;
import com.poly.bezbe.enums.Role;
import com.poly.bezbe.exception.DuplicateResourceException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public String register(RegisterRequestDTO request) {
        // Ném ra exception cụ thể khi email đã tồn tại
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new DuplicateResourceException("Email đã được sử dụng.");
        });

        String activationToken = UUID.randomUUID().toString();
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .isActive(false)
                .activationToken(activationToken)
                .build();
        userRepository.save(user);

        // TODO: Gọi EmailService ở đây để gửi email thật
        System.out.println("Gửi email kích hoạt tới " + user.getEmail() + " với token: " + activationToken);

        return "Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt tài khoản.";
    }

    public String activateAccount(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token kích hoạt không hợp lệ hoặc đã hết hạn."));
        user.setActive(true);
        user.setActivationToken(null);
        userRepository.save(user);
        return "Tài khoản của bạn đã được kích hoạt thành công! Vui lòng đăng nhập.";
    }

    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Tài khoản của bạn chưa được kích hoạt.");
        }

        var jwtToken = jwtService.generateToken(user);

        // --- BẮT ĐẦU SỬA ---
        // Xây dựng Response với tất cả các trường
        return AuthenticationResponseDTO.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .id(user.getId())
                .name(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .roles(List.of(user.getRole().name())) // Chuyển Enum Role sang List<String>
                .avatar(user.getAvatar())
                .build();
        // --- KẾT THÚC SỬA ---
    }

    public String forgotPassword(ForgotPasswordRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + request.getEmail()));
        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        userRepository.save(user);

        // TODO: Gọi EmailService
        System.out.println("Gửi token đến email " + user.getEmail() + ". Token: " + resetToken);
        return "Một token để reset mật khẩu đã được gửi đến email của bạn.";
    }

    public String resetPassword(ResetPasswordRequestDTO request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Token không hợp lệ hoặc đã hết hạn."));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        userRepository.save(user);
        return "Đặt lại mật khẩu thành công!";
    }


}