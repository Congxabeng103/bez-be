package com.poly.bezbe.dto.response.review;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponseDTO {
    private Long id;
    private String userName;   // Sẽ ghép từ lastName + firstName
    private String userAvatar; // Lấy từ field avatar của User
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private boolean isVerifiedPurchase;
    private boolean visible;
}