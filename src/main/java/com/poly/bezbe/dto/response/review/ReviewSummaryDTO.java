package com.poly.bezbe.dto.response.review;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class ReviewSummaryDTO {
    private BigDecimal averageRating;
    private Long totalReviews;
    // Map lưu: Key là số sao (5), Value là số lượng đánh giá (10)
    private Map<Integer, Long> starCounts;
}