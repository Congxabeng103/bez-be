package com.poly.bezbe.exception;

import com.poly.bezbe.dto.response.ApiResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Xử lý lỗi Validate (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(ApiResponseDTO.error(message), HttpStatus.BAD_REQUEST);
    }

    // 2. Xử lý lỗi XUNG ĐỘT / TRÙNG LẶP (QUAN TRỌNG: Đã thêm DuplicateResourceException vào đây)
    // Trả về 409 Conflict
    @ExceptionHandler({BusinessRuleException.class, DuplicateResourceException.class})
    public ResponseEntity<ApiResponseDTO<Object>> handleConflictException(RuntimeException ex) {
        // ex.getMessage() sẽ là: "Email đã được sử dụng."
        return new ResponseEntity<>(ApiResponseDTO.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    // 3. Xử lý lỗi sai mật khẩu/Tài khoản chưa kích hoạt
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleBadCredentialsException(BadCredentialsException ex) {
        return new ResponseEntity<>(ApiResponseDTO.error("Email hoặc mật khẩu không chính xác."), HttpStatus.UNAUTHORIZED);
    }

    // 4. Xử lý lỗi Không tìm thấy (404)
    @ExceptionHandler({ResourceNotFoundException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponseDTO<Object>> handleResourceNotFoundException(RuntimeException ex) {
        return new ResponseEntity<>(ApiResponseDTO.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    // 5. Xử lý lỗi Request sai logic
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleIllegalStateException(IllegalStateException ex) {
        return new ResponseEntity<>(ApiResponseDTO.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // 6. Xử lý các lỗi còn lại (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleUncaughtException(Exception ex) {
        ex.printStackTrace(); // In log server
        return new ResponseEntity<>(ApiResponseDTO.error("Có lỗi không mong muốn xảy ra từ hệ thống."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}