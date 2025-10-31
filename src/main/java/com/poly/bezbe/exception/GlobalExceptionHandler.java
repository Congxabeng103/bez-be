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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(ApiResponseDTO.error(message), HttpStatus.BAD_REQUEST);
    }
    // Sửa dòng này:
    @ExceptionHandler(BusinessRuleException.class) // <-- Chỉ cần 1 lỗi
    public ResponseEntity<ApiResponseDTO<Object>> handleConflictException(BusinessRuleException ex) { // <-- Sửa kiểu
        return new ResponseEntity<>(ApiResponseDTO.error(ex.getMessage()), HttpStatus.CONFLICT); // 409
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleBadCredentialsException(BadCredentialsException ex) {
        return new ResponseEntity<>(ApiResponseDTO.error("Email hoặc mật khẩu không chính xác."), HttpStatus.UNAUTHORIZED);
    }



    @ExceptionHandler({ResourceNotFoundException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponseDTO<Object>> handleResourceNotFoundException(RuntimeException ex) {
        return new ResponseEntity<>(ApiResponseDTO.error(ex.getMessage()), HttpStatus.NOT_FOUND); // 404
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleIllegalStateException(IllegalStateException ex) {
        return new ResponseEntity<>(ApiResponseDTO.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleUncaughtException(Exception ex) {
        ex.printStackTrace(); // In ra log để debug
        return new ResponseEntity<>(ApiResponseDTO.error("Có lỗi không mong muốn xảy ra từ hệ thống."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}