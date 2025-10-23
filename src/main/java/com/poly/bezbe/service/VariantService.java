package com.poly.bezbe.service;


import com.poly.bezbe.dto.request.product.BatchCreateVariantsRequestDTO;
import com.poly.bezbe.dto.request.product.CreateVariantRequestDTO;
import com.poly.bezbe.dto.request.product.UpdateVariantRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.VariantResponseDTO;
import com.poly.bezbe.entity.AttributeValue;
import com.poly.bezbe.entity.Product;
import com.poly.bezbe.entity.ProductVariant;
import com.poly.bezbe.entity.VariantValue;
import com.poly.bezbe.exception.DuplicateResourceException; // Import exception
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.AttributeValueRepository;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.repository.ProductVariantRepository;
import com.poly.bezbe.repository.VariantValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*; // Import Set, Map
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final VariantValueRepository variantValueRepository;

    // Hàm chuyển đổi ProductVariant Entity -> VariantResponseDTO
    private VariantResponseDTO mapToVariantDTO(ProductVariant variant) {
        // Lấy tất cả các cặp VariantValue (liên kết) của biến thể này
        Set<VariantValue> variantValues = variantValueRepository.findByVariant_Id(variant.getId());

        // Chuyển đổi thành Map<Tên thuộc tính, Giá trị>
        Map<String, String> attributesMap = variantValues.stream()
                .collect(Collectors.toMap(
                        // Key: Lấy tên từ Attribute cha (vv -> AttributeValue -> Attribute -> Name)
                        vv -> vv.getAttributeValue().getAttribute().getName(),
                        // Value: Lấy giá trị trực tiếp từ AttributeValue
                        vv -> vv.getAttributeValue().getValue()
                ));

        return VariantResponseDTO.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .imageUrl(variant.getImageUrl())
                .attributes(attributesMap) // Gán Map đã tạo
                .createdAt(variant.getCreatedAt())
                .build();
    }

    /**
     * Lấy danh sách biến thể của một sản phẩm (có phân trang và tìm kiếm).
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<VariantResponseDTO> getVariantsByProduct(Long productId, Pageable pageable, String searchTerm) {
        Page<ProductVariant> variantPage;

        if (searchTerm != null && !searchTerm.isBlank()) {
            // Tìm kiếm trong SKU hoặc Tên/Giá trị Thuộc tính
            variantPage = variantRepository.searchBySkuOrAttribute(productId, searchTerm.trim(), pageable);
        } else {
            // Lấy tất cả biến thể của sản phẩm
            variantPage = variantRepository.findByProduct_Id(productId, pageable);
        }

        List<VariantResponseDTO> variantDTOs = variantPage.getContent().stream()
                .map(this::mapToVariantDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(
                variantDTOs,
                variantPage.getNumber(),
                variantPage.getSize(),
                variantPage.getTotalElements(),
                variantPage.getTotalPages()
        );
    }

    /**
     * Tạo nhiều biến thể cùng lúc (Batch Create).
     */
    @Transactional
    public List<VariantResponseDTO> createVariantsInBatch(BatchCreateVariantsRequestDTO request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + request.getProductId()));

        List<ProductVariant> createdVariants = new ArrayList<>();

        for (CreateVariantRequestDTO variantDTO : request.getVariants()) {
            // Kiểm tra SKU trùng lặp trước khi tạo
            if (variantRepository.existsBySku(variantDTO.getSku())) {
                throw new DuplicateResourceException("SKU '" + variantDTO.getSku() + "' đã tồn tại.");
            }
            ProductVariant newVariant = createSingleVariant(product, variantDTO);
            createdVariants.add(newVariant);
        }

        // Chuyển đổi danh sách kết quả sang DTO
        return createdVariants.stream()
                .map(this::mapToVariantDTO)
                .collect(Collectors.toList());
    }

    // Hàm private để tạo một biến thể và liên kết giá trị thuộc tính
    private ProductVariant createSingleVariant(Product product, CreateVariantRequestDTO dto) {
        // 1. Tạo và lưu ProductVariant trước
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(dto.getSku())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .imageUrl(dto.getImageUrl())
                // .name(product.getName() + " " + ...) // Có thể tự tạo tên biến thể nếu cần
                .build();
        ProductVariant savedVariant = variantRepository.save(variant);

        // 2. Lấy các đối tượng AttributeValue từ danh sách ID
        List<AttributeValue> attributeValues = attributeValueRepository.findAllById(dto.getAttributeValueIds());
        // Kiểm tra xem có lấy đủ số lượng không (đề phòng ID sai)
        if (attributeValues.size() != dto.getAttributeValueIds().size()) {
            throw new ResourceNotFoundException("Một hoặc nhiều ID giá trị thuộc tính không hợp lệ.");
        }

        // 3. Tạo các bản ghi liên kết trong bảng `variant_values`
        Set<VariantValue> variantValuesToSave = attributeValues.stream()
                .map(attrVal -> VariantValue.builder()
                        .variant(savedVariant) // Liên kết với biến thể vừa lưu
                        .attributeValue(attrVal) // Liên kết với giá trị thuộc tính
                        .build())
                .collect(Collectors.toSet()); // Dùng Set để tránh trùng lặp nếu có

        variantValueRepository.saveAll(variantValuesToSave);

        // Gán lại Set vào variant để mapToVariantDTO có thể đọc được ngay (tùy chọn)
        savedVariant.setAttributeValues(variantValuesToSave);

        return savedVariant;
    }

    /**
     * Cập nhật thông tin một biến thể (không cập nhật thuộc tính).
     */
    @Transactional
    public VariantResponseDTO updateVariant(Long variantId, UpdateVariantRequestDTO request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể với ID: " + variantId));

        // Kiểm tra SKU trùng lặp (trừ chính nó ra)
        if (variantRepository.existsBySkuAndIdNot(request.getSku(), variantId)) {
            throw new DuplicateResourceException("SKU '" + request.getSku() + "' đã được sử dụng bởi biến thể khác.");
        }

        variant.setSku(request.getSku());
        variant.setPrice(request.getPrice());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setImageUrl(request.getImageUrl());
        // (Không cập nhật attributeValues)

        ProductVariant updatedVariant = variantRepository.save(variant);
        return mapToVariantDTO(updatedVariant); // Trả về DTO đã cập nhật
    }

    /**
     * Xóa một biến thể.
     */
    @Transactional
    public void deleteVariant(Long variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Không tìm thấy biến thể với ID: " + variantId);
        }
        // Việc xóa ProductVariant sẽ tự động xóa các VariantValue liên quan (do Cascade)
        variantRepository.deleteById(variantId);
    }
}