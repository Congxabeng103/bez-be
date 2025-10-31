package com.poly.bezbe.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lỗi vi phạm quy tắc nghiệp vụ (ví dụ: SP đã có trong KM khác).
 * Trả về lỗi 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}