package com.poly.bezbe.exception;

import com.poly.bezbe.dto.response.ApiResponseDTO;
import org.springframework.dao.DataIntegrityViolationException; // <-- Import
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

    // 1. Lỗi Validate (@Valid) - 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(ApiResponseDTO.error(message), HttpStatus.BAD_REQUEST);
    }

    // 2. Lỗi nghiệp vụ (Trùng lặp, Logic) - 409 (CONFLICT)
    // Đây là handler "linh hoạt" mà bạn muốn.
    // Nó bắt các lỗi CỤ THỂ mà Service ném ra.
    @ExceptionHandler({BusinessRuleException.class, DuplicateResourceException.class})
    public ResponseEntity<ApiResponseDTO<Object>> handleConflictException(RuntimeException ex) {
        // Trả về CHÍNH XÁC message mà bạn đã ném từ Service
        // vd: "Mã coupon 'SALE50' đã tồn tại."
        return new ResponseEntity<>(ApiResponseDTO.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    // 3. Lỗi Database (Dự phòng cho Trùng lặp) - 409
    // Đây là handler "dự phòng" cho trường hợp bạn QUÊN kiểm tra ở Service.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // Trả về message chung chung vì chúng ta không biết chính xác
        return new ResponseEntity<>(ApiResponseDTO.error("Dữ liệu này đã tồn tại hoặc vi phạm ràng buộc."), HttpStatus.CONFLICT);
    }

    // 4. Lỗi Không tìm thấy (404)
    @ExceptionHandler({ResourceNotFoundException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponseDTO<Object>> handleResourceNotFoundException(RuntimeException ex) {
        return new ResponseEntity<>(ApiResponseDTO.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    // 5. Lỗi Sai mật khẩu (401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleBadCredentialsException(BadCredentialsException ex) {
        return new ResponseEntity<>(ApiResponseDTO.error("Email hoặc mật khẩu không chính xác."), HttpStatus.UNAUTHORIZED);
    }

    // 6. Lỗi logic (vd: Xóa KM khi còn SP) - 400
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleIllegalStateException(IllegalStateException ex) {
        return new ResponseEntity<>(ApiResponseDTO.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // 7. Lỗi 500 (Tất cả lỗi còn lại)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleUncaughtException(Exception ex) {
        ex.printStackTrace(); // In log server
        return new ResponseEntity<>(ApiResponseDTO.error("Có lỗi không mong muốn xảy ra từ hệ thống."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}