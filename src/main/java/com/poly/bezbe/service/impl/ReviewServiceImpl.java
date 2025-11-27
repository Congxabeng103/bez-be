package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.review.ReviewRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.review.ReviewResponseDTO;
import com.poly.bezbe.dto.response.review.ReviewSummaryDTO;
import com.poly.bezbe.entity.Product;
import com.poly.bezbe.entity.Review;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.repository.ReviewRepository;
import com.poly.bezbe.repository.UserRepository;
import com.poly.bezbe.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public PageResponseDTO<ReviewResponseDTO> getReviewsByProduct(Long productId, Integer rating, String sortBy, Pageable pageable) {
        // 1. Xử lý Sắp xếp
        Sort sort = switch (sortBy != null ? sortBy : "DATE_DESC") {
            case "RATING_DESC" -> Sort.by("rating").descending();
            case "RATING_ASC" -> Sort.by("rating").ascending();
            case "DATE_ASC" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        };
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // 2. Check quyền Admin
        boolean isAdminOrManager = false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            isAdminOrManager = authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("MANAGER"));
        }

        Page<Review> page;

        if (isAdminOrManager) {
            // ADMIN: Lấy tất cả (Cả ẩn và hiện)
            if (rating != null && rating > 0) {
                // Hàm này trong Repo giữ nguyên
                page = reviewRepository.findByProductIdAndRating(productId, rating, sortedPageable);
            } else {
                page = reviewRepository.findByProductId(productId, sortedPageable);
            }
        } else {
            // USER: Chỉ lấy cái hiện (Visible = true)
            // LƯU Ý: Repository phải có hàm tên là ...VisibleTrue
            if (rating != null && rating > 0) {
                page = reviewRepository.findByProductIdAndRatingAndVisibleTrue(productId, rating, sortedPageable);
            } else {
                page = reviewRepository.findByProductIdAndVisibleTrue(productId, sortedPageable);
            }
        }

        Page<ReviewResponseDTO> dtoPage = page.map(this::mapToDTO);

        return PageResponseDTO.<ReviewResponseDTO>builder()
                .content(dtoPage.getContent())
                .pageNo(dtoPage.getNumber())
                .pageSize(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .build();
    }

    @Override
    public ReviewSummaryDTO getReviewSummary(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return null;

        // Chỉ đếm những review ĐANG HIỆN (visible = true)
        List<Object[]> stats = reviewRepository.countReviewsByRating(productId);

        Map<Integer, Long> starCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) starCounts.put(i, 0L);

        for (Object[] row : stats) {
            Integer r = (Integer) row[0];
            Long count = (Long) row[1];
            starCounts.put(r, count);
        }

        return ReviewSummaryDTO.builder()
                .averageRating(product.getAverageRating() != null ? product.getAverageRating() : BigDecimal.ZERO)
                .totalReviews(product.getReviewCount() != null ? product.getReviewCount() : 0L)
                .starCounts(starCounts)
                .build();
    }

    @Override
    @Transactional
    public ReviewResponseDTO createReview(ReviewRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Vui lòng đăng nhập để đánh giá");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if(reviewRepository.existsByUserIdAndProductId(user.getId(), request.getProductId())) {
            throw new IllegalStateException("Bạn đã đánh giá sản phẩm này rồi!");
        }

        if(!reviewRepository.existsByUserAndProductAndStatus(user.getId(), request.getProductId(), OrderStatus.COMPLETED)) {
            throw new IllegalStateException("Bạn phải mua và nhận hàng thành công mới được đánh giá!");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy sản phẩm"));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .visible(true) // Tên biến là 'visible', nên builder là .visible()
                .build();
        reviewRepository.save(review);

        recalculateProductStats(product);

        return mapToDTO(review);
    }

    @Override
    @Transactional
    public void toggleReviewVisibility(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy đánh giá"));

        // Với boolean nguyên thủy, lombok sinh getter là isVisible()
        // Đảo ngược trạng thái
        review.setVisible(!review.isVisible());

        reviewRepository.save(review);

        recalculateProductStats(review.getProduct());
    }

    private void recalculateProductStats(Product product) {
        // Đếm lại dựa trên các review visible = true
        List<Object[]> stats = reviewRepository.countReviewsByRating(product.getId());

        long totalCount = 0;
        long totalScore = 0;

        for (Object[] row : stats) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];

            totalCount += count;
            totalScore += (rating * count);
        }

        if (totalCount > 0) {
            BigDecimal avg = BigDecimal.valueOf(totalScore)
                    .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
            product.setAverageRating(avg);
            product.setReviewCount(totalCount);
        } else {
            product.setAverageRating(BigDecimal.ZERO);
            product.setReviewCount(0L);
        }

        productRepository.save(product);
    }

    private ReviewResponseDTO mapToDTO(Review review) {
        String fullName = review.getUser().getLastName() + " " + review.getUser().getFirstName();
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .userName(fullName)
                .userAvatar(review.getUser().getAvatar())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .isVerifiedPurchase(true)
                // Tên biến DTO là 'visible', gán bằng giá trị của review.isVisible()
                .visible(review.isVisible())
                .build();
    }
}