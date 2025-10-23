package com.poly.bezbe.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Lớp này định nghĩa cấu trúc CHUẨN cho TẤT CẢ các response trả về từ API.
 * Giúp Frontend dễ dàng xử lý và hiển thị thông báo.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Không hiển thị các trường null trong JSON
public class ApiResponseDTO<T> {
    private Status status;
    private String message;
    private T data;

    public enum Status { SUCCESS, ERROR }

    public static <T> ApiResponseDTO<T> success(T data, String message) {
        return ApiResponseDTO.<T>builder().status(Status.SUCCESS).message(message).data(data).build();
    }

    public static <T> ApiResponseDTO<T> error(String message) {
        return ApiResponseDTO.<T>builder().status(Status.ERROR).message(message).build();
    }
}