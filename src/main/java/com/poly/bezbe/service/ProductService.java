package com.poly.bezbe.service;

// --- Imports ---
import com.poly.bezbe.dto.request.product.ProductRequestDTO; // Sửa đường dẫn nếu DTO của bạn là com.poly.bezbe.dto.request.product
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.ProductBriefDTO;
import com.poly.bezbe.dto.response.product.ProductResponseDTO; // Sửa đường dẫn nếu DTO của bạn là com.poly.bezbe.dto.response.product
import com.poly.bezbe.entity.Brand;
import com.poly.bezbe.entity.Category;
import com.poly.bezbe.entity.Product;
import com.poly.bezbe.entity.Promotion; // <-- 1. Import Promotion
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.BrandRepository;
import com.poly.bezbe.repository.CategoryRepository;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.repository.PromotionRepository; // <-- 2. Import PromotionRepository
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
// --- End Imports ---

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final PromotionRepository promotionRepository; // <-- 3. Inject PromotionRepository

    // Hàm map Entity -> DTO (Tính salePrice và lấy thông tin promotion)
    private ProductResponseDTO mapToProductDTO(Product product) {
        String categoryName = (product.getCategory() != null) ? product.getCategory().getName() : null;
        String brandName = (product.getBrand() != null) ? product.getBrand().getName() : null;
        Long promotionId = null;
        String promotionName = null;
        BigDecimal salePrice = null;

        Promotion promotion = product.getPromotion(); // Lấy promotion từ product

        // 4. Logic tính salePrice (đã có sẵn trong code của bạn, rất tốt)
        if (promotion != null && promotion.isActive()) {
            LocalDate today = LocalDate.now();
            if (!today.isBefore(promotion.getStartDate()) && !today.isAfter(promotion.getEndDate())) {
                promotionId = promotion.getId();
                promotionName = promotion.getName();
                BigDecimal discountPercent = promotion.getDiscountValue();
                BigDecimal originalPrice = product.getPrice();
                if (discountPercent != null && originalPrice != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discountAmount = originalPrice.multiply(discountPercent)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    salePrice = originalPrice.subtract(discountAmount);
                    if (salePrice.compareTo(BigDecimal.ZERO) < 0) salePrice = BigDecimal.ZERO;
                }
            }
        }

        return ProductResponseDTO.builder()
                .id(product.getId()).name(product.getName()).description(product.getDescription())
                .price(product.getPrice()) // Giá gốc
                .imageUrl(product.getImageUrl()).categoryName(categoryName).brandName(brandName)
                .promotionId(promotionId) // ID KM
                .promotionName(promotionName) // Tên KM
                .salePrice(salePrice) // Giá sau giảm
                .createdAt(product.getCreatedAt())
                .build();
    }

    // 5. Hàm tìm Promotion hoặc trả về null
    private Promotion findPromotionById(Long promotionId) {
        if (promotionId == null) {
            return null; // Cho phép không chọn KM
        }
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion ID: " + promotionId));
    }

    /**
     * Lấy danh sách sản phẩm có phân trang và tìm kiếm.
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<ProductResponseDTO> getAllProducts(Pageable pageable, String searchTerm) {
        // ... (Logic này đã đúng, giữ nguyên) ...
        Page<Product> productPage;
        if (searchTerm != null && !searchTerm.isBlank()) {
            productPage = productRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        List<ProductResponseDTO> productDTOs = productPage.getContent().stream()
                .map(this::mapToProductDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(
                productDTOs, productPage.getNumber(), productPage.getSize(),
                productPage.getTotalElements(), productPage.getTotalPages()
        );
    }

    /**
     * Tạo sản phẩm mới. (Đã sửa)
     */
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

        // 6. Tìm Promotion từ promotionId
        Promotion promotion = findPromotionById(request.getPromotionId());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(category)
                .brand(brand)
                .promotion(promotion) // <-- 7. Gán Promotion vào Product
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToProductDTO(savedProduct);
    }

    /**
     * Cập nhật sản phẩm. (Đã sửa)
     */
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

        // 8. Tìm Promotion từ promotionId
        Promotion promotion = findPromotionById(request.getPromotionId());

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setBrand(brand);
        product.setPromotion(promotion); // <-- 9. Cập nhật Promotion cho Product

        Product updatedProduct = productRepository.save(product);
        return mapToProductDTO(updatedProduct);
    }

    /**
     * Xóa sản phẩm.
     */
    @Transactional
    public void deleteProduct(Long productId) {
        // ... (Logic này đã đúng, giữ nguyên) ...
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId);
        }
        productRepository.deleteById(productId);
    }

    /**
     * Lấy danh sách Product rút gọn (cho Variant form)
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<ProductBriefDTO> getProductBriefList(Pageable pageable, String searchTerm) {
        // ... (Logic này đã đúng, giữ nguyên) ...
        Page<Product> productPage;
        if (searchTerm != null && !searchTerm.isBlank()) {
            productPage = productRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        List<ProductBriefDTO> briefDTOs = productPage.getContent().stream()
                .map(p -> ProductBriefDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .variantCount(p.getVariants() != null ? p.getVariants().size() : 0)
                        .build())
                .collect(Collectors.toList());
        return new PageResponseDTO<>(
                briefDTOs, productPage.getNumber(), productPage.getSize(),
                productPage.getTotalElements(), productPage.getTotalPages()
        );
    }
}