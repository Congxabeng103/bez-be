package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.review.ReviewRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.review.ReviewResponseDTO;
import com.poly.bezbe.dto.response.review.ReviewSummaryDTO;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    // 1. Lấy danh sách có Lọc (rating), Sắp xếp (sortBy) và Chỉ lấy review đang hiện
    PageResponseDTO<ReviewResponseDTO> getReviewsByProduct(Long productId, Integer rating, String sortBy, Pageable pageable);

    // 2. Lấy bảng thống kê (Số sao TB, phần trăm từng sao)
    ReviewSummaryDTO getReviewSummary(Long productId);

    // 3. Tạo đánh giá
    ReviewResponseDTO createReview(ReviewRequestDTO request);

    // 4. Ẩn/Hiện đánh giá (Dành cho Admin/Manager)
    void toggleReviewVisibility(Long reviewId);
}