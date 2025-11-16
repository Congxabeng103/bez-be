package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.PromotionRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.PromotionBriefDTO;
import com.poly.bezbe.dto.response.PromotionResponseDTO;
import com.poly.bezbe.entity.Promotion;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.DuplicateResourceException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.repository.PromotionRepository;
import com.poly.bezbe.service.PromotionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;

    private PromotionResponseDTO mapToPromotionDTO(Promotion promotion) {
        long count = productRepository.countByPromotion(promotion);
        return PromotionResponseDTO.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .discountValue(promotion.getDiscountValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .active(promotion.isActive())
                .createdAt(promotion.getCreatedAt())
                .productCount(count)
                .build();
    }

    @Override
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
     * (HÀM HELPER)
     * Ném lỗi nếu admin cố kích hoạt KM không hợp lệ.
     */
    private boolean determineActiveStatus(LocalDate startDate, LocalDate endDate, boolean formIsActive) {
        // 1. Nếu admin muốn "TẮT" (bản nháp), luôn là false. Không cần validate.
        if (!formIsActive) {
            return false;
        }

        // 2. Nếu admin muốn "BẬT", chúng ta phải validate
        LocalDate today = LocalDate.now();

        // 3. Nếu admin BẬT một KM đã hết hạn -> NÉM LỖI
        if (endDate.isBefore(today)) {
            throw new BusinessRuleException("Lỗi: Không thể kích hoạt. Ngày kết thúc đã ở trong quá khứ. (Gia hạn lại ngày kết thúc nếu muốn kích hoạt lại)");
        }

        // 4. Nếu admin BẬT một KM chưa tới ngày -> NÉM LỖI
        if (startDate.isAfter(today)) {
            throw new BusinessRuleException("Lỗi: Không thể kích hoạt. Ngày bắt đầu đang ở trong tương lai. (Bỏ tick 'Kích hoạt' để lưu nháp hoặc sửa lại ngày bắt đầu)");
        }

        // 5. Nếu đang trong hạn -> true (OK)
        return true;
    }

    @Override
    @Transactional
    public PromotionResponseDTO createPromotion(PromotionRequestDTO request) {
        String name = request.getName().trim();
        if (promotionRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Tên khuyến mãi '" + name + "' đã tồn tại.");
        }

        boolean newActiveStatus = determineActiveStatus(
                request.getStartDate(),
                request.getEndDate(),
                request.isActive()
        );

        Promotion promotion = Promotion.builder()
                .name(name)
                .description(request.getDescription())
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(newActiveStatus) // Dùng trạng thái đã validate
                .build();
        Promotion saved = promotionRepository.save(promotion);
        return mapToPromotionDTO(saved);
    }

    @Override
    @Transactional
    public PromotionResponseDTO updatePromotion(Long id, PromotionRequestDTO request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion: " + id));

        String name = request.getName().trim();
        if (!promotion.getName().equalsIgnoreCase(name) &&
                promotionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Tên khuyến mãi '" + name + "' đã được sử dụng.");
        }

        boolean newActiveStatus = determineActiveStatus(
                request.getStartDate(),
                request.getEndDate(),
                request.isActive()
        );

        promotion.setName(name);
        promotion.setDescription(request.getDescription());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setActive(newActiveStatus); // Dùng trạng thái đã validate

        Promotion updated = promotionRepository.save(promotion);
        return mapToPromotionDTO(updated);
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion: " + id));
        promotion.setActive(false);
        promotionRepository.save(promotion);
    }

    @Override
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
    @Transactional
    public void permanentDeletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi với ID: " + id));
        long productCount = productRepository.countByPromotion(promotion);
        if (productCount > 0) {
            throw new IllegalStateException("Không thể xóa vĩnh viễn khuyến mãi đang được áp dụng cho " + productCount + " sản phẩm.");
        }
        promotionRepository.delete(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponseDTO getLatestActivePromotion() {
        Optional<Promotion> latestPromotionOpt = promotionRepository
                .findFirstByActiveOrderByEndDateDesc(true);
        if (latestPromotionOpt.isEmpty()) {
            return null;
        }
        return mapToPromotionDTO(latestPromotionOpt.get());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getActivePromotions() {
        List<Promotion> promotions = promotionRepository.findAllByActive(true);
        return promotions.stream()
                .map(this::mapToPromotionDTO)
                .collect(Collectors.toList());
    }
}