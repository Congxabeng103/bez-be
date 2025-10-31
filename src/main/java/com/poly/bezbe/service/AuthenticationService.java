package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.auth.AuthenticationRequestDTO;
import com.poly.bezbe.dto.request.auth.ForgotPasswordRequestDTO;
import com.poly.bezbe.dto.request.auth.RegisterRequestDTO;
import com.poly.bezbe.dto.request.auth.ResetPasswordRequestDTO;
import com.poly.bezbe.dto.response.auth.AuthenticationResponseDTO;

/**
 * Interface cho các dịch vụ liên quan đến xác thực người dùng.
 * (Đăng ký, Đăng nhập, Quên mật khẩu, v.v.)
 */
public interface AuthenticationService {

    /**
     * Xử lý đăng ký người dùng mới.
     * Gửi email kích hoạt sau khi tạo tài khoản.
     *
     * @param request Thông tin đăng ký.
     * @return Thông báo kết quả.
     */
    String register(RegisterRequestDTO request);

    /**
     * Kích hoạt tài khoản người dùng dựa trên token.
     *
     * @param token Token kích hoạt từ email.
     * @return Thông báo kết quả.
     */
    String activateAccount(String token);

    /**
     * Xác thực (đăng nhập) người dùng.
     *
     * @param request Thông tin email và mật khẩu.
     * @return DTO chứa thông tin user và JWT token.
     */
    AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request);

    /**
     * Xử lý yêu cầu quên mật khẩu.
     * Gửi email chứa link đặt lại mật khẩu.
     *
     * @param request Chứa email của người dùng.
     * @return Thông báo kết quả (luôn thành công để bảo mật).
     */
    String forgotPassword(ForgotPasswordRequestDTO request);

    /**
     * Xử lý việc đặt lại mật khẩu mới.
     *
     * @param request Chứa token và mật khẩu mới.
     * @return Thông báo kết quả.
     */
    String resetPassword(ResetPasswordRequestDTO request);
}