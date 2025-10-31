// File: com/poly/bezbe/service/impl/VariantServiceImpl.java
package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.product.VariantBatchRequestDTO;
import com.poly.bezbe.dto.request.product.VariantRequestDTO;
import com.poly.bezbe.dto.request.product.VariantUpdateRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.VariantResponseDTO;
import com.poly.bezbe.entity.AttributeValue;
import com.poly.bezbe.entity.Product;
import com.poly.bezbe.entity.ProductVariant;
import com.poly.bezbe.entity.VariantValue;
// SỬA IMPORT NÀY:
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.*;
import com.poly.bezbe.service.VariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VariantServiceImpl implements VariantService {

    private final VariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final VariantValueRepository variantValueRepository;
    private final OrderItemRepository orderItemRepository;
    /**
     * Hàm helper (private) để chuyển Entity sang DTO
     */
    private VariantResponseDTO mapToVariantDTO(ProductVariant variant) {
        Map<String, String> attributesMap = variant.getAttributeValues().stream()
                .map(VariantValue::getAttributeValue)
                .collect(Collectors.toMap(
                        av -> av.getAttribute().getName(),
                        AttributeValue::getValue
                ));

        // --- 2. SỬA DÒNG NÀY (LÀM THẬT) ---
        long orderCount = orderItemRepository.countByVariantId(variant.getId());

        return VariantResponseDTO.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .imageUrl(variant.getImageUrl())
                .attributes(attributesMap)
                .active(variant.isActive())
                .orderCount(orderCount) // <-- Gán giá trị thật
                .createdAt(variant.getCreatedAt())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<VariantResponseDTO> getVariantsByProduct(Long productId, Pageable pageable, String searchTerm, String status) {
        Page<ProductVariant> variantPage;
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
     */
    @Override
    @Transactional
    public List<VariantResponseDTO> createVariantsBatch(VariantBatchRequestDTO batchRequest) {
        Product product = productRepository.findById(batchRequest.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Product ID: " + batchRequest.getProductId()));

        List<ProductVariant> existingVariants = variantRepository.findByProductId(product.getId());

        Set<Set<Long>> existingAttributeCombinations = existingVariants.stream()
                .map(variant -> variantValueRepository.findAttributeValueIdsByVariantId(variant.getId()))
                .collect(Collectors.toSet());

        List<ProductVariant> savedVariants = new ArrayList<>();

        for (VariantRequestDTO request : batchRequest.getVariants()) {
            Set<Long> newAttributeValueIds = new HashSet<>(request.getAttributeValueIds());

            if (existingAttributeCombinations.contains(newAttributeValueIds)) {
                // --- SỬA LỖI EXCEPTION ---
                throw new BusinessRuleException("Lỗi: Đã tồn tại một biến thể với tổ hợp thuộc tính y hệt.");
            }

            if (variantRepository.findBySku(request.getSku()).isPresent()) {
                // --- SỬA LỖI EXCEPTION ---
                throw new BusinessRuleException("SKU '" + request.getSku() + "' đã tồn tại.");
            }

            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku(request.getSku())
                    .price(request.getPrice())
                    .stockQuantity(request.getStockQuantity())
                    .imageUrl(request.getImageUrl())
                    .active(true)
                    .build();

            ProductVariant savedVariant = variantRepository.save(variant);

            List<AttributeValue> attributeValues = attributeValueRepository.findAllById(request.getAttributeValueIds());
            if(attributeValues.size() != request.getAttributeValueIds().size()){
                throw new ResourceNotFoundException("Một hoặc nhiều Attribute Value ID không hợp lệ.");
            }

            Set<VariantValue> variantValuesJoin = new HashSet<>();
            for (AttributeValue av : attributeValues) {
                VariantValue vv = VariantValue.builder()
                        .variant(savedVariant)
                        .attributeValue(av)
                        .build();
                variantValuesJoin.add(vv);
            }
            variantValueRepository.saveAll(variantValuesJoin);

            savedVariant.setAttributeValues(variantValuesJoin);
            savedVariants.add(savedVariant);

            existingAttributeCombinations.add(newAttributeValueIds);
        }

        return savedVariants.stream()
                .map(this::mapToVariantDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public VariantResponseDTO updateVariant(Long variantId, VariantUpdateRequestDTO request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Biến thể: " + variantId));

        if (!variant.getSku().equalsIgnoreCase(request.getSku()) &&
                variantRepository.findBySkuAndIdNot(request.getSku(), variantId).isPresent()) {
            // --- SỬA LỖI EXCEPTION ---
            throw new BusinessRuleException("SKU '" + request.getSku() + "' đã tồn tại.");
        }

        variant.setSku(request.getSku());
        variant.setPrice(request.getPrice());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setImageUrl(request.getImageUrl());
        variant.setActive(request.isActive());

        ProductVariant updated = variantRepository.save(variant);
        return mapToVariantDTO(updated);
    }

    /**
     * {@inheritDoc} (Hàm này là Soft Delete - Giữ nguyên)
     */
    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Biến thể: " + variantId));

        variant.setActive(false); // <-- SOFT DELETE
        variantRepository.save(variant);
    }

    // --- 3. SỬA HÀM NÀY (LÀM THẬT) ---
    @Override
    @Transactional
    public void permanentDeleteVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Biến thể: " + variantId));

        // --- SỬA DÒNG NÀY (LÀM THẬT) ---
        long orderCount = orderItemRepository.countByVariantId(variantId);

        // Logic kiểm tra
        if (orderCount > 0) {
            throw new BusinessRuleException("Không thể xóa vĩnh viễn biến thể đã có trong đơn hàng.");
        }

        // Xóa các bản ghi liên quan trong bảng join `variant_values`
        variantValueRepository.deleteAll(variant.getAttributeValues());

        // Xóa vĩnh viễn chính biến thể đó
        variantRepository.delete(variant);
    }
}