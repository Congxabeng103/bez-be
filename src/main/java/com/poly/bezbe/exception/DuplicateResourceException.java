package com.poly.bezbe.exception;


// Exception này sẽ được sử dụng khi tài nguyên (ví dụ: email) đã tồn tại
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}