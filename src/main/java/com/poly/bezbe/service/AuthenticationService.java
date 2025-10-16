package com.poly.bezbe.service;

import com.poly.bezbe.config.JwtService;
import com.poly.bezbe.dto.request.auth.*;
import com.poly.bezbe.dto.response.auth.AuthenticationResponseDTO;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.enums.AuthProvider;
import com.poly.bezbe.enums.Role;
import com.poly.bezbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    public String register(RegisterRequestDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        // Tạo một token kích hoạt ngẫu nhiên
        String activationToken = UUID.randomUUID().toString();

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                // ✨ 1. Đặt tài khoản là CHƯA KÍCH HOẠT
                .isActive(false)
                // ✨ 2. Lưu token kích hoạt vào user
                .activationToken(activationToken)
                .build();

        userRepository.save(user);

        // ✨ 3. TRONG THỰC TẾ: Bạn sẽ gọi một EmailService ở đây để gửi email.
        // Link kích hoạt sẽ có dạng: http://your-frontend-app.com/activate?token=...
        System.out.println("Gửi email kích hoạt tới " + user.getEmail() + " với token: " + activationToken);

        // ✨ 4. Thay đổi thông báo trả về
        return "Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt tài khoản của bạn.";
    }

    /**
     * ✅ THÊM LOGIC KÍCH HOẠT TÀI KHOẢN MỚI
     */
    public String activateAccount(String token) {
        // 1. Tìm người dùng bằng token kích hoạt
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new RuntimeException("Token kích hoạt không hợp lệ hoặc đã hết hạn."));

        // 2. Nếu tìm thấy, kích hoạt tài khoản
        user.setActive(true);
        // 3. Xóa token đi để nó không thể được sử dụng lại
        user.setActivationToken(null);
        userRepository.save(user);

        return "Tài khoản của bạn đã được kích hoạt thành công! Vui lòng đăng nhập.";
    }

    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponseDTO.builder().token(jwtToken).build();
    }

    public String forgotPassword(ForgotPasswordRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + request.getEmail()));
        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        userRepository.save(user);
        System.out.println("Gửi token đến email " + user.getEmail() + ". Token: " + resetToken);
        return "Một token để reset mật khẩu đã được gửi đến email của bạn.";
    }

    public String resetPassword(ResetPasswordRequestDTO request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ hoặc đã hết hạn."));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        userRepository.save(user);
        return "Đặt lại mật khẩu thành công!";
    }

    public String updatePassword(UpdatePasswordRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác.");
        }
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        return "Đổi mật khẩu thành công!";
    }
}