package com.poly.bezbe.service.impl; // <-- Chuyển sang package 'impl'

import com.poly.bezbe.config.JwtService;
import com.poly.bezbe.dto.request.auth.*;
import com.poly.bezbe.dto.response.auth.AuthenticationResponseDTO;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.enums.AuthProvider;
import com.poly.bezbe.enums.Role;
import com.poly.bezbe.exception.DuplicateResourceException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.UserRepository;
import com.poly.bezbe.service.AuthenticationService; // <-- Import interface
import com.poly.bezbe.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service // <-- @Service nằm ở lớp Impl
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService { // <-- Implement interface

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // Đặt URL frontend của bạn ở đây (React app)
    private final String clientUrl = "http://localhost:3000";

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public String register(RegisterRequestDTO request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new DuplicateResourceException("Email đã được sử dụng.");
        });

        String activationToken = UUID.randomUUID().toString();

        var user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : "")
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .isActive(false)
                .activationToken(activationToken)
                .build();

        userRepository.save(user);

        try {
            String activationLink = clientUrl + "/activate?token=" + activationToken;
            String htmlBody = String.format("""
                <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2>Chào mừng bạn đến với BezBe!</h2>
                    <p>Cảm ơn bạn đã đăng ký. Vui lòng nhấn vào nút bên dưới để kích hoạt tài khoản của bạn:</p>
                    <a href="%s" style="display: inline-block; padding: 10px 18px; font-size: 16px; color: white; background-color: #007bff; text-decoration: none; border-radius: 5px;">
                        Kích hoạt tài khoản
                    </a>
                </div>
            """, activationLink);

            emailService.sendHtmlEmail(user.getEmail(), "Kích hoạt tài khoản BezBe", htmlBody);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi email kích hoạt: " + e.getMessage());
            // Cân nhắc: Có thể ném ra một ngoại lệ tùy chỉnh ở đây nếu việc gửi email thất bại là nghiêm trọng
        }

        return "Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt tài khoản.";
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional // Thêm Transactional cho thao tác cập nhật
    public String activateAccount(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token kích hoạt không hợp lệ hoặc đã hết hạn."));
        user.setActive(true);
        user.setActivationToken(null);
        userRepository.save(user);
        return "Tài khoản của bạn đã được kích hoạt thành công! Vui lòng đăng nhập.";
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user")); // Về mặt lý thuyết, không bao giờ xảy ra nếu authenticate() thành công

        if (!user.isEnabled()) {
            if (user.getActivationToken() != null) {
                throw new IllegalStateException("Tài khoản của bạn chưa được kích hoạt. Vui lòng kiểm tra email.");
            }
            throw new BadCredentialsException("Tài khoản của bạn đã bị khóa.");
        }

        var jwtToken = jwtService.generateToken(user);

        String lastName = (user.getLastName() != null ? user.getLastName() : "");
        String firstName = (user.getFirstName() != null ? user.getFirstName() : "");
        String fullName = (lastName + " " + firstName).trim();

        return AuthenticationResponseDTO.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .id(user.getId())
                .name(fullName)
                .firstName(firstName)
                .lastName(lastName)
                .email(user.getEmail())
                .roles(List.of(user.getRole().name()))
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .dob(user.getDob() != null ? user.getDob().toString() : null)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional // Thêm Transactional cho thao tác cập nhật
    public String forgotPassword(ForgotPasswordRequestDTO request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setResetPasswordToken(resetToken);
            userRepository.save(user);

            try {
                String resetLink = clientUrl + "/reset-password?token=" + resetToken;
                String htmlBody = String.format("""
                    <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                        <h2>Yêu cầu đặt lại mật khẩu</h2>
                        <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Nhấn vào nút bên dưới:</p>
                        <a href="%s" style="display: inline-block; padding: 10px 18px; font-size: 16px; color: white; background-color: #007bff; text-decoration: none; border-radius: 5px;">
                            Đặt lại mật khẩu
                        </a>
                        <p style="margin-top: 20px;">Link này sẽ hết hạn sau 15 phút (nếu bạn có cài đặt).</p>
                        <p>Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>
                    </div>
                """, resetLink);

                emailService.sendHtmlEmail(user.getEmail(), "Yêu cầu đặt lại mật khẩu", htmlBody);
            } catch (MessagingException e) {
                System.err.println("Lỗi khi gửi email reset mật khẩu: " + e.getMessage());
            }
        });

        return "Nếu email của bạn tồn tại trong hệ thống, một link đặt lại mật khẩu đã được gửi đến.";
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional // Thêm Transactional cho thao tác cập nhật
    public String resetPassword(ResetPasswordRequestDTO request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Token không hợp lệ hoặc đã hết hạn."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        userRepository.save(user);
        return "Đặt lại mật khẩu thành công!";
    }
}