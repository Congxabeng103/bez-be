package com.poly.bezbe.dto.request.review;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequestDTO {
    @NotNull(message = "Sản phẩm không hợp lệ")
    private Long productId;

    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    private Integer rating;

    @NotBlank(message = "Vui lòng nhập nội dung đánh giá")
    private String comment;
}