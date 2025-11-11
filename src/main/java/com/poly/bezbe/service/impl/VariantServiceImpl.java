package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.product.VariantBatchRequestDTO;
import com.poly.bezbe.dto.request.product.VariantRequestDTO;
import com.poly.bezbe.dto.request.product.VariantUpdateRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.VariantResponseDTO;
import com.poly.bezbe.entity.*; // Giữ import tổng
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.*; // Giữ import tổng
import com.poly.bezbe.service.VariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VariantServiceImpl implements VariantService {

    private final VariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    // --- THÊM 2 REPO MỚI THAY THẾ REPO CŨ ---
    private final ProductOptionValueRepository productOptionValueRepository;
    private final VariantOptionValueRepository variantOptionValueRepository;

    // (Xóa AttributeValueRepository và VariantValueRepository)

    /**
     * Hàm helper (private) để chuyển Entity sang DTO
     */
    private VariantResponseDTO mapToVariantDTO(Variant variant) {
        // 1. LẤY THÔNG TIN THUỘC TÍNH (ĐÃ SỬA)
        Map<String, String> attributesMap = variant.getOptionValues().stream() // <-- SỬA (từ getAttributeValues)
                .map(VariantOptionValue::getOptionValue) // <-- SỬA (từ VariantValue::getAttributeValue)
                .collect(Collectors.toMap(
                        ov -> ov.getOption().getName(), // Lấy tên Option (Màu sắc)
                        ProductOptionValue::getValue  // Lấy Value (Đỏ)
                ));

        long orderCount = orderItemRepository.countByVariantId(variant.getId());

        // 2. Lấy Product cha và Promotion (Giữ nguyên)
        Product product = variant.getProduct();
        Promotion promotion = (product != null) ? product.getPromotion() : null;

        // 3. Sao chép Logic tính SalePrice (Giữ nguyên)
        BigDecimal originalPrice = variant.getPrice();
        BigDecimal salePrice = null;
        Boolean isPromotionStillValid = null;

        if (promotion != null) {
            isPromotionStillValid = false;
            if (promotion.isActive()) {
                LocalDate today = LocalDate.now();
                if (!today.isBefore(promotion.getStartDate()) && !today.isAfter(promotion.getEndDate())) {
                    isPromotionStillValid = true;
                    BigDecimal discountPercent = promotion.getDiscountValue();

                    if (discountPercent != null && originalPrice != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal discountAmount = originalPrice.multiply(discountPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        salePrice = originalPrice.subtract(discountAmount);
                        if (salePrice.compareTo(BigDecimal.ZERO) < 0) salePrice = BigDecimal.ZERO;
                    }
                }
            }
        }
        // --- KẾT THÚC LOGIC KHUYẾN MÃI ---

        return VariantResponseDTO.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .price(originalPrice)
                .stockQuantity(variant.getStockQuantity())
                .imageUrl(variant.getImageUrl())
                .attributes(attributesMap) // <-- Dữ liệu đã được map đúng
                .active(variant.isActive())
                .orderCount(orderCount)
                .createdAt(variant.getCreatedAt())
                .salePrice(salePrice)
                .isPromotionStillValid(isPromotionStillValid)
                .build();
    }

    /**
     * {@inheritDoc}
     * (Hàm này không thay đổi)
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<VariantResponseDTO> getVariantsByProduct(Long productId, Pageable pageable, String searchTerm, String status) {
        Page<Variant> variantPage;
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        boolean activeFilter = !"INACTIVE".equalsIgnoreCase(status);
        String search = searching ? searchTerm.trim() : null;
        String statusFilter = status.toUpperCase();

        variantPage = variantRepository.findByProductAndSearchAndStatus(
                productId, search, statusFilter, activeFilter, pageable
        );

        List<VariantResponseDTO> dtos = variantPage.getContent().stream()
                .map(this::mapToVariantDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(dtos, variantPage.getNumber(), variantPage.getSize(),
                variantPage.getTotalElements(), variantPage.getTotalPages());
    }

    /**
     * {@inheritDoc}
     * (Hàm này thay đổi logic)
     */
    @Override
    @Transactional
    public List<VariantResponseDTO> createVariantsBatch(VariantBatchRequestDTO batchRequest) {
        Product product = productRepository.findById(batchRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Product ID: " + batchRequest.getProductId()));

        List<Variant> existingVariants = variantRepository.findByProductId(product.getId());

        // --- SỬA LOGIC NÀY ---
        Set<Set<Long>> existingOptionCombinations = existingVariants.stream()
                .map(variant -> variantOptionValueRepository.findOptionValueIdsByVariantId(variant.getId())) // <-- SỬA REPO
                .collect(Collectors.toSet());
        // --- KẾT THÚC SỬA ---

        List<Variant> savedVariants = new ArrayList<>();

        for (VariantRequestDTO request : batchRequest.getVariants()) {
            // --- SỬA TÊN BIẾN (DTO đã đổi) ---
            Set<Long> newOptionValueIds = new HashSet<>(request.getOptionValueIds()); // <-- SỬA TÊN

            if (existingOptionCombinations.contains(newOptionValueIds)) { // <-- SỬA TÊN
                throw new BusinessRuleException("Lỗi: Đã tồn tại một biến thể với tổ hợp thuộc tính y hệt.");
            }

            if (variantRepository.findBySku(request.getSku()).isPresent()) {
                throw new BusinessRuleException("SKU '" + request.getSku() + "' đã tồn tại.");
            }

            Variant variant = Variant.builder()
                    .product(product)
                    .sku(request.getSku())
                    .price(request.getPrice())
                    .stockQuantity(request.getStockQuantity())
                    .imageUrl(request.getImageUrl())
                    .active(true)
                    .build();

            Variant savedVariant = variantRepository.save(variant);

            // --- SỬA TOÀN BỘ KHỐI NÀY ---
            // Tìm các entity ProductOptionValue
            List<ProductOptionValue> optionValues = productOptionValueRepository.findAllById(request.getOptionValueIds()); // <-- SỬA REPO, TÊN BIẾN
            if(optionValues.size() != request.getOptionValueIds().size()){
                throw new ResourceNotFoundException("Một hoặc nhiều Option Value ID không hợp lệ.");
            }

            Set<VariantOptionValue> variantOptionValuesJoin = new HashSet<>(); // <-- SỬA KIỂU
            for (ProductOptionValue ov : optionValues) { // <-- SỬA KIỂU

                // (Thêm 1 bước kiểm tra an toàn)
                if (!ov.getOption().getProduct().getId().equals(product.getId())) {
                    throw new BusinessRuleException("Lỗi: Giá trị thuộc tính không thuộc về sản phẩm này.");
                }

                VariantOptionValue vov = VariantOptionValue.builder() // <-- SỬA KIỂU
                        .variant(savedVariant)
                        .optionValue(ov) // <-- SỬA TRƯỜNG
                        .option(ov.getOption()) // Thêm link tới Option cha
                        .build();
                variantOptionValuesJoin.add(vov);
            }
            variantOptionValueRepository.saveAll(variantOptionValuesJoin); // <-- SỬA REPO

            savedVariant.setOptionValues(variantOptionValuesJoin); // <-- SỬA HÀM SET
            // --- KẾT THÚC SỬA KHỐI ---

            savedVariants.add(savedVariant);
            existingOptionCombinations.add(newOptionValueIds); // <-- SỬA TÊN
        }

        return savedVariants.stream()
                .map(this::mapToVariantDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * (Hàm này không thay đổi)
     */
    @Override
    @Transactional
    public VariantResponseDTO updateVariant(Long variantId, VariantUpdateRequestDTO request) {
        Variant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Biến thể: " + variantId));

        if (!variant.getSku().equalsIgnoreCase(request.getSku()) &&
                variantRepository.findBySkuAndIdNot(request.getSku(), variantId).isPresent()) {
            throw new BusinessRuleException("SKU '" + request.getSku() + "' đã tồn tại.");
        }

        variant.setSku(request.getSku());
        variant.setPrice(request.getPrice());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setImageUrl(request.getImageUrl());
        variant.setActive(request.isActive());

        Variant updated = variantRepository.save(variant);
        return mapToVariantDTO(updated);
    }

    /**
     * {@inheritDoc}
     * (Hàm này không thay đổi)
     */
    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        Variant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Biến thể: " + variantId));

        variant.setActive(false); // <-- SOFT DELETE
        variantRepository.save(variant);
    }

    /**
     * {@inheritDoc}
     * (Hàm này thay đổi logic)
     */
    @Override
    @Transactional
    public void permanentDeleteVariant(Long variantId) {
        Variant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Biến thể: " + variantId));

        long orderCount = orderItemRepository.countByVariantId(variantId);

        if (orderCount > 0) {
            throw new BusinessRuleException("Không thể xóa vĩnh viễn biến thể đã có trong đơn hàng.");
        }

        // --- SỬA LOGIC NÀY ---
        // Xóa các bản ghi liên quan trong bảng join `variant_option_values`
        variantOptionValueRepository.deleteAll(variant.getOptionValues()); // <-- SỬA
        // --- KẾT THÚC SỬA ---

        // Xóa vĩnh viễn chính biến thể đó
        variantRepository.delete(variant);
    }

    /**
     * {@inheritDoc}
     * (Hàm này thay đổi logic)
     */
    @Override
    @Transactional(readOnly = true)
    public VariantResponseDTO findVariantByAttributes(Long productId, List<Long> valueIds) {
        if (valueIds == null || valueIds.isEmpty()) {
            throw new BusinessRuleException("Cần có ít nhất một giá trị thuộc tính.");
        }

        // --- SỬA LOGIC NÀY ---
        // 1. Gọi query mới từ Repository
        List<Long> variantIds = variantOptionValueRepository.findVariantIdsByExactOptionValues( // <-- SỬA REPO
                productId, valueIds, (long) valueIds.size()
        );
        // --- KẾT THÚC SỬA ---

        // 2. Xử lý kết quả (Giữ nguyên)
        if (variantIds.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy biến thể phù hợp.");
        }
        if (variantIds.size() > 1) {
            System.err.println("Cảnh báo: Tìm thấy " + variantIds.size() + " biến thể trùng lặp.");
        }

        // 3. Lấy (variantId đầu tiên) và map sang DTO (Giữ nguyên)
        Variant variant = variantRepository.findById(variantIds.get(0))
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi: Không tìm thấy ID biến thể đã map."));

        return mapToVariantDTO(variant);
    }
}