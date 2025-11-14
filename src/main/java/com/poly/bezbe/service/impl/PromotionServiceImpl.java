package com.poly.bezbe.service.impl; // <-- Chú ý package con 'impl'

import com.poly.bezbe.dto.request.PromotionRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.PromotionBriefDTO;
import com.poly.bezbe.dto.response.PromotionResponseDTO;
import com.poly.bezbe.entity.Promotion;
import com.poly.bezbe.exception.DuplicateResourceException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.repository.PromotionRepository;
import com.poly.bezbe.service.PromotionService; // <-- Import interface
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service // <-- @Service được đặt ở đây
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService { // <-- Implement interface

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;


    // Hàm private này là chi tiết nội bộ, chỉ nằm ở Impl
    private PromotionResponseDTO mapToPromotionDTO(Promotion promotion) {
        // 1. Lấy số lượng sản phẩm
        // (Chúng ta dùng countByPromotion vì hàm delete của bạn cũng dùng nó)
        long count = productRepository.countByPromotion(promotion);

        // 2. Trả về DTO đã bao gồm productCount
        return PromotionResponseDTO.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .discountValue(promotion.getDiscountValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .active(promotion.isActive())
                .createdAt(promotion.getCreatedAt())
                .productCount(count) // <-- THÊM DÒNG NÀY
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<PromotionResponseDTO> getAllPromotions(Pageable pageable, String searchTerm, String status) {
        Page<Promotion> promotionPage;
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        boolean activeFilter = !"INACTIVE".equalsIgnoreCase(status);
        String search = searching ? searchTerm.trim() : null;

        promotionPage = promotionRepository.findBySearchAndStatus(search, status.toUpperCase(), activeFilter, pageable);

        List<PromotionResponseDTO> dtos = promotionPage.getContent().stream()
                .map(this::mapToPromotionDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(dtos, promotionPage.getNumber(), promotionPage.getSize(),
                promotionPage.getTotalElements(), promotionPage.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public PromotionResponseDTO createPromotion(PromotionRequestDTO request) {
        String name = request.getName().trim();
        if (promotionRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Tên khuyến mãi '" + name + "' đã tồn tại.");
        }

        Promotion promotion = Promotion.builder()
                .name(name)
                .description(request.getDescription())
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.isActive())
                .build();
        Promotion saved = promotionRepository.save(promotion);
        return mapToPromotionDTO(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public PromotionResponseDTO updatePromotion(Long id, PromotionRequestDTO request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion: " + id));

        String name = request.getName().trim();
        if (!promotion.getName().equalsIgnoreCase(name) &&
                promotionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Tên khuyến mãi '" + name + "' đã được sử dụng.");
        }

        promotion.setName(name);
        promotion.setDescription(request.getDescription());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setActive(request.isActive());

        Promotion updated = promotionRepository.save(promotion);
        return mapToPromotionDTO(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion: " + id));

        promotion.setActive(false); // <-- SOFT DELETE
        promotionRepository.save(promotion);
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional(readOnly = true)
    public List<PromotionBriefDTO> getPromotionBriefList() {
        return promotionRepository.findAllByActiveTrue(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(promo -> PromotionBriefDTO.builder()
                        .id(promo.getId())
                        .name(promo.getName())
                        .build())
                .collect(Collectors.toList());
    }
    @Override
    @Transactional // Đảm bảo an toàn dữ liệu
    public void permanentDeletePromotion(Long id) {
        // 1. Tìm khuyến mãi
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi với ID: " + id));

        // 2. KIỂM TRA ĐIỀU KIỆN (RẤT QUAN TRỌNG)
        // Kiểm tra xem có sản phẩm nào đang liên kết với khuyến mãi này không.
        // (Giả sử bạn có hàm countByPromotionId trong ProductRepository)
        long productCount = productRepository.countByPromotion(promotion);

        if (productCount > 0) {
            // Nếu có, ném lỗi, không cho xóa
            throw new IllegalStateException("Không thể xóa vĩnh viễn khuyến mãi đang được áp dụng cho " + productCount + " sản phẩm.");
        }

        // 3. Nếu không có sản phẩm nào liên kết, tiến hành xóa vĩnh viễn
        promotionRepository.delete(promotion);
    }
}