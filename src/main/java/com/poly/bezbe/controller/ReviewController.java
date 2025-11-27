package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.review.ReviewRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.review.ReviewResponseDTO;
import com.poly.bezbe.dto.response.review.ReviewSummaryDTO;
import com.poly.bezbe.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 1. API Public: Xem danh sách đánh giá (Có phân trang, lọc, sắp xếp)
    @GetMapping("/product/{productId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<ReviewResponseDTO>>> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) Integer rating, // Lọc theo số sao (1-5)
            @RequestParam(defaultValue = "DATE_DESC") String sortBy // Sắp xếp
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponseDTO<ReviewResponseDTO> reviews = reviewService.getReviewsByProduct(productId, rating, sortBy, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(reviews, "Lấy danh sách thành công"));
    }

    // 2. API Public: Lấy bảng thống kê (Số sao TB, % từng sao)
    @GetMapping("/product/{productId}/stats")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponseDTO<ReviewSummaryDTO>> getReviewStats(@PathVariable Long productId) {
        ReviewSummaryDTO stats = reviewService.getReviewSummary(productId);
        return ResponseEntity.ok(ApiResponseDTO.success(stats, "Lấy thống kê thành công"));
    }

    // 3. API Private: Viết đánh giá (Yêu cầu login)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<ReviewResponseDTO>> createReview(
            @Valid @RequestBody ReviewRequestDTO request
    ) {
        ReviewResponseDTO newReview = reviewService.createReview(request);
        return new ResponseEntity<>(
                ApiResponseDTO.success(newReview, "Đánh giá thành công"),
                HttpStatus.CREATED
        );
    }

    // 4. API Admin/Manager: Ẩn/Hiện đánh giá (Soft Delete)
    // Dùng method DELETE để frontend dễ gọi (tư duy là xóa khỏi trang sản phẩm)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponseDTO<Object>> toggleReview(@PathVariable Long id) {
        reviewService.toggleReviewVisibility(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Đã thay đổi trạng thái hiển thị đánh giá"));
    }
}