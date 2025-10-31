// File: com/poly/bezbe/service/impl/ProductServiceImpl.java
package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.product.ProductRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.ProductBriefDTO;
import com.poly.bezbe.dto.response.product.ProductResponseDTO;
import com.poly.bezbe.entity.*; // Sửa lại import
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.*; // Sửa lại import
import com.poly.bezbe.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final PromotionRepository promotionRepository;

    // --- 1. THÊM REPOSITORY NÀY ---
    private final VariantRepository variantRepository;

    // --- 2. SỬA HÀM NÀY ---
    private ProductResponseDTO mapToProductDTO(Product product) {
        Category category = product.getCategory();
        Brand brand = product.getBrand();
        Promotion promotion = product.getPromotion();

        // (Logic lấy categoryName, brandName... của bạn giữ nguyên)
        String categoryName = (category != null) ? category.getName() : null;
        Long categoryId = (category != null) ? category.getId() : null;
        Boolean isCategoryActive = (category != null) ? category.isActive() : null;

        String brandName = (brand != null) ? brand.getName() : null;
        Long brandId = (brand != null) ? brand.getId() : null;
        Boolean isBrandActive = (brand != null) ? brand.isActive() : null;

        // (Logic tính promotion... của bạn giữ nguyên)
        Long promotionId = null;
        String promotionName = null;
        BigDecimal salePrice = null;
        Boolean isPromotionStillValid = null;
        if (promotion != null) {
            promotionId = promotion.getId();
            promotionName = promotion.getName();
            isPromotionStillValid = false;
            if (promotion.isActive()) {
                LocalDate today = LocalDate.now();
                if (!today.isBefore(promotion.getStartDate()) && !today.isAfter(promotion.getEndDate())) {
                    isPromotionStillValid = true;
                    BigDecimal discountPercent = promotion.getDiscountValue();
                    BigDecimal originalPrice = product.getPrice();
                    if (discountPercent != null && originalPrice != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal discountAmount = originalPrice.multiply(discountPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        salePrice = originalPrice.subtract(discountAmount);
                        if (salePrice.compareTo(BigDecimal.ZERO) < 0) salePrice = BigDecimal.ZERO;
                    }
                }
            }
        }

        // --- TÍNH TOÁN variantCount ---
        long variantCount = variantRepository.countByProductId(product.getId());

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .brandId(brandId)
                .brandName(brandName)
                .promotionId(promotionId)
                .promotionName(promotionName)
                .salePrice(salePrice)
                .createdAt(product.getCreatedAt())
                .active(product.isActive())
                .isCategoryActive(isCategoryActive)
                .isBrandActive(isBrandActive)
                .isPromotionStillValid(isPromotionStillValid)
                .variantCount(variantCount) // <-- Gán giá trị
                .build();
    }

    // (Hàm findPromotionById giữ nguyên)
    private Promotion findPromotionById(Long promotionId) {
        if (promotionId == null) return null;
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion ID: " + promotionId));
    }

    // (Hàm getAllProducts giữ nguyên)
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ProductResponseDTO> getAllProducts(Pageable pageable, String searchTerm, String status) {
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        boolean activeFilter = !"INACTIVE".equalsIgnoreCase(status);
        String search = searching ? searchTerm.trim() : null;
        String statusFilter = status.toUpperCase();
        Page<Product> productPage = productRepository.findBySearchAndStatus(search, statusFilter, activeFilter, pageable);
        List<ProductResponseDTO> productDTOs = productPage.getContent().stream()
                .map(this::mapToProductDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(
                productDTOs, productPage.getNumber(), productPage.getSize(),
                productPage.getTotalElements(), productPage.getTotalPages()
        );
    }

    // (Hàm createProduct giữ nguyên)
    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        Category category = (request.getCategoryId() != null) ?
                categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Category ID: " + request.getCategoryId()))
                : null;
        Brand brand = (request.getBrandId() != null) ?
                brandRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Brand ID: " + request.getBrandId()))
                : null;
        Promotion promotion = findPromotionById(request.getPromotionId());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(category)
                .brand(brand)
                .promotion(promotion)
                .active(request.isActive())
                .build();
        Product saved = productRepository.save(product);
        return mapToProductDTO(saved);
    }

    // (Hàm updateProduct giữ nguyên)
    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Long productId, ProductRequestDTO request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));
        Category category = (request.getCategoryId() != null) ?
                categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Category ID: " + request.getCategoryId()))
                : null;
        Brand brand = (request.getBrandId() != null) ?
                brandRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Brand ID: " + request.getBrandId()))
                : null;
        Promotion promotion = findPromotionById(request.getPromotionId());

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setBrand(brand);
        product.setPromotion(promotion);
        product.setActive(request.isActive());

        Product updatedProduct = productRepository.save(product);
        return mapToProductDTO(updatedProduct);
    }

    // --- 3. SỬA HÀM NÀY (Ẩn hàng loạt) ---
    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));

        // 1. Ẩn sản phẩm
        product.setActive(false);
        productRepository.save(product);

        // 2. Ẩn hàng loạt các BIẾN THỂ đang active
        List<ProductVariant> variantsToHide = variantRepository.findAllByProductIdAndActive(productId, true);
        if (!variantsToHide.isEmpty()) {
            for (ProductVariant variant : variantsToHide) {
                variant.setActive(false);
            }
            variantRepository.saveAll(variantsToHide);
        }
    }

    // --- 4. THÊM HÀM MỚI (Xóa vĩnh viễn) ---
    @Override
    @Transactional
    public void permanentDeleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));

        // 1. Kiểm tra xem có biến thể nào không
        long variantCount = variantRepository.countByProductId(productId);

        if (variantCount > 0) {
            // 2. Nếu có, ném lỗi
            throw new BusinessRuleException("Không thể xóa vĩnh viễn sản phẩm đang có biến thể.");        }

        // (Kiểm tra xem có nằm trong Chi tiết đơn hàng nào không)
        // long orderDetailsCount = orderDetailRepository.countByProductId(productId);
        // if (orderDetailsCount > 0) {
        //    throw new DataConflictException("Không thể xóa sản phẩm đã từng được bán.");
        // }

        // 3. Nếu không có, tiến hành xóa vĩnh viễn
        productRepository.delete(product);
    }


    // --- 5. SỬA HÀM NÀY (Tối ưu) ---
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ProductBriefDTO> getProductBriefList(Pageable pageable, String searchTerm) {
        Page<Product> productPage;
        if (searchTerm != null && !searchTerm.isBlank()) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(searchTerm.trim(), pageable);
        } else {
            productPage = productRepository.findAllByActiveTrue(pageable);
        }

        List<ProductBriefDTO> briefDTOs = productPage.getContent().stream()
                .map(p -> ProductBriefDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        // Sửa: Dùng query count thay vì .size()
                        .variantCount(variantRepository.countByProductId(p.getId()))
                        .build())
                .collect(Collectors.toList());

        return new PageResponseDTO<>(
                briefDTOs, productPage.getNumber(), productPage.getSize(),
                productPage.getTotalElements(), productPage.getTotalPages()
        );
    }
}