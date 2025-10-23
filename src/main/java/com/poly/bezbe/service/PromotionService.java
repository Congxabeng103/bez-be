package com.poly.bezbe.service;

// --- Imports ---
import com.poly.bezbe.dto.request.PromotionRequestDTO; // Không cần productIds
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.PromotionBriefDTO; // Import Brief DTO
import com.poly.bezbe.dto.response.PromotionResponseDTO; // Không cần productIds
import com.poly.bezbe.entity.Promotion;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // Import Sort
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
// --- End Imports ---

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    // Map Entity Promotion -> DTO (Không có productIds)
    private PromotionResponseDTO mapToPromotionDTO(Promotion promotion) {
        return PromotionResponseDTO.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .discountValue(promotion.getDiscountValue()) // %
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .active(promotion.isActive())
                .createdAt(promotion.getCreatedAt())
                // Bỏ productIds
                .build();
    }

    // Lấy danh sách Promotion có phân trang/tìm kiếm (cho trang quản lý KM)
    @Transactional(readOnly = true)
    public PageResponseDTO<PromotionResponseDTO> getAllPromotions(Pageable pageable, String searchTerm) {
        Page<Promotion> promotionPage;
        if (searchTerm != null && !searchTerm.isBlank()) {
            promotionPage = promotionRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable);
        } else {
            promotionPage = promotionRepository.findAll(pageable);
        }
        List<PromotionResponseDTO> dtos = promotionPage.getContent().stream()
                .map(this::mapToPromotionDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(dtos, promotionPage.getNumber(), promotionPage.getSize(),
                promotionPage.getTotalElements(), promotionPage.getTotalPages());
    }

    // Tạo mới Promotion (Không xử lý productIds)
    @Transactional
    public PromotionResponseDTO createPromotion(PromotionRequestDTO request) {
        Promotion promotion = Promotion.builder()
                .name(request.getName().trim()) // Trim tên
                .description(request.getDescription())
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.isActive())
                // Bỏ .products()
                .build();
        Promotion saved = promotionRepository.save(promotion);
        return mapToPromotionDTO(saved);
    }

    // Cập nhật Promotion (Không xử lý productIds)
    @Transactional
    public PromotionResponseDTO updatePromotion(Long id, PromotionRequestDTO request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion: " + id));
        promotion.setName(request.getName().trim());
        promotion.setDescription(request.getDescription());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setActive(request.isActive());
        // Bỏ .setProducts()
        Promotion updated = promotionRepository.save(promotion);
        return mapToPromotionDTO(updated);
    }

    // Xóa Promotion
    @Transactional
    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Promotion: " + id);
        }
        // Quan trọng: Khi xóa Promotion, các Product đang liên kết đến nó sẽ
        // tự động được cập nhật promotion_id thành NULL (do JPA/Hibernate quản lý).
        // Bạn không cần xử lý thêm ở đây.
        promotionRepository.deleteById(id);
    }

    // Lấy danh sách Promotion rút gọn (cho dropdown trong Product form)
    @Transactional(readOnly = true)
    public List<PromotionBriefDTO> getPromotionBriefList() {
        // Lấy tất cả, sắp xếp theo tên A-Z
        return promotionRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(promo -> PromotionBriefDTO.builder()
                        .id(promo.getId())
                        .name(promo.getName())
                        .build())
                .collect(Collectors.toList());
    }
}