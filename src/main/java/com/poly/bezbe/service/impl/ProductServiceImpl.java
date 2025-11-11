package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.product.OptionRequestDTO;
import com.poly.bezbe.dto.request.product.OptionValueRequestDTO;
import com.poly.bezbe.dto.request.product.ProductRequestDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.dto.response.product.*; // (Import DTO mới)
import com.poly.bezbe.entity.*;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.*;
import com.poly.bezbe.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final PromotionRepository promotionRepository;
    private final VariantRepository variantRepository;

    // Repos mới cho logic Option
    private final ProductOptionRepository productOptionRepository;
    private final ProductOptionValueRepository productOptionValueRepository;

    // (Hàm này đã đúng, không cần sửa)
    private ProductResponseDTO mapToProductDTO(Product product) {
        Category category = product.getCategory();
        Brand brand = product.getBrand();
        Promotion promotion = product.getPromotion();

        String categoryName = (category != null) ? category.getName() : null;
        Long categoryId = (category != null) ? category.getId() : null;
        Boolean isCategoryActive = (category != null) ? category.isActive() : null;

        String brandName = (brand != null) ? brand.getName() : null;
        Long brandId = (brand != null) ? brand.getId() : null;
        Boolean isBrandActive = (brand != null) ? brand.isActive() : null;

        Optional<BigDecimal> lowestVariantPriceOpt = variantRepository
                .findFirstByProductIdAndActiveTrueOrderByPriceAsc(product.getId())
                .map(Variant::getPrice);

        BigDecimal basePrice = product.getPrice();
        BigDecimal displayPrice = lowestVariantPriceOpt.orElse(basePrice);

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
                    BigDecimal originalPrice = displayPrice;

                    if (discountPercent != null && originalPrice != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal discountAmount = originalPrice.multiply(discountPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        salePrice = originalPrice.subtract(discountAmount);
                        if (salePrice.compareTo(BigDecimal.ZERO) < 0) salePrice = BigDecimal.ZERO;
                    }
                }
            }
        }

        long variantCount = variantRepository.countByProductId(product.getId());

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(displayPrice)
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
                .variantCount(variantCount)
                .build();
    }

    // (Hàm này đúng)
    private Promotion findPromotionById(Long promotionId) {
        if (promotionId == null) return null;
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Promotion ID: " + promotionId));
    }

    // (Hàm này đúng)
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ProductResponseDTO> getAllProducts(
            Pageable pageable,
            String searchTerm,
            String status,
            String categoryName,
            Double minPrice,
            Double maxPrice
    ) {
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        String search = searching ? searchTerm.trim() : null;
        String statusFilter = status.toUpperCase();
        boolean activeStatus = "ACTIVE".equalsIgnoreCase(statusFilter);

        Page<Product> productPage = productRepository.searchAndFilterProducts(
                search,
                statusFilter,
                activeStatus,
                categoryName,
                minPrice,
                maxPrice,
                pageable
        );

        List<ProductResponseDTO> productDTOs = productPage.getContent().stream()
                .map(this::mapToProductDTO).collect(Collectors.toList());

        return new PageResponseDTO<>(
                productDTOs, productPage.getNumber(), productPage.getSize(),
                productPage.getTotalElements(), productPage.getTotalPages()
        );
    }

    // (Hàm này đúng)
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
                .price(BigDecimal.ZERO)
                .imageUrl(request.getImageUrl())
                .category(category)
                .brand(brand)
                .promotion(promotion)
                .active(request.isActive())
                .build();
        Product savedProduct = productRepository.save(product);

        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            int optionPosition = 1;
            List<ProductOption> newOptions = new ArrayList<>();
            for (OptionRequestDTO optionDTO : request.getOptions()) {
                ProductOption option = ProductOption.builder()
                        .product(savedProduct)
                        .name(optionDTO.getName().trim())
                        .position(optionPosition++)
                        .build();
                ProductOption savedOption = productOptionRepository.save(option);

                int valuePosition = 1;
                List<ProductOptionValue> newValues = new ArrayList<>();
                for (OptionValueRequestDTO valueDTO : optionDTO.getValues()) {
                    ProductOptionValue value = ProductOptionValue.builder()
                            .option(savedOption)
                            .value(valueDTO.getValue().trim())
                            .position(valuePosition++)
                            .build();
                    newValues.add(value);
                }
                productOptionValueRepository.saveAll(newValues);
                option.setValues(newValues);
                newOptions.add(savedOption);
            }
            savedProduct.setOptions(newOptions);
        }

        return mapToProductDTO(savedProduct);
    }

    // --- SỬA HÀM UPDATE (LOGIC MỚI) ---
    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Long productId, ProductRequestDTO request) {
        // 1. Tìm Product, Category, Brand, Promotion (Giữ nguyên)
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

        // 2. Cập nhật thông tin cơ bản (Giữ nguyên)
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setBrand(brand);
        product.setPromotion(promotion);
        product.setActive(request.isActive()); // <-- CẬP NHẬT TRẠNG THÁI ACTIVE TỪ REQUEST

        // 3. --- LOGIC CẬP NHẬT OPTIONS (ĐÃ SỬA) ---

        List<OptionRequestDTO> requestOptions = request.getOptions();

        // **BƯỚC 1: CHỈ xử lý options NẾU requestOptions không phải là null.**
        // (Nếu requestOptions là null (ví dụ: từ nút Kích hoạt),
        //  chúng ta sẽ bỏ qua toàn bộ khối 'if' này và chỉ lưu các thay đổi cơ bản ở trên)
        if (requestOptions != null) {

            List<ProductOption> persistentOptions = product.getOptions();
            if (persistentOptions == null) persistentOptions = new ArrayList<>();

            // Đếm số biến thể
            long variantCount = variantRepository.countByProductId(productId);

            if (variantCount > 0) {
                // TRƯỜNG HỢP 1: ĐÃ CÓ BIẾN THỂ

                boolean isChanged = false;
                if (persistentOptions.size() != requestOptions.size()) {
                    isChanged = true;
                } else {
                    // (Giả sử FE gửi về đúng thứ tự)
                    for (int i = 0; i < persistentOptions.size(); i++) {
                        ProductOption pOpt = persistentOptions.get(i);
                        OptionRequestDTO rOpt = requestOptions.get(i);

                        List<ProductOptionValue> pVals = pOpt.getValues();
                        List<OptionValueRequestDTO> rVals = rOpt.getValues();

                        if (pVals == null) pVals = new ArrayList<>(); // (Thêm check null)
                        if (rVals == null) rVals = new ArrayList<>(); // (Thêm check null)

                        if (pVals.size() != rVals.size()) {
                            isChanged = true; break;
                        }

                        for (int j = 0; j < pVals.size(); j++) {
                            if (!pVals.get(j).getValue().equals(rVals.get(j).getValue().trim())) {
                                isChanged = true; break;
                            }
                        }
                        if (isChanged) break;

                        // Nếu cấu trúc không đổi, chỉ cho phép cập nhật tên Option
                        pOpt.setName(rOpt.getName().trim());
                    }
                }

                if (isChanged) {
                    throw new BusinessRuleException("Không thể thay đổi thuộc tính hoặc giá trị khi sản phẩm đã có biến thể. Vui lòng xóa các biến thể trước.");
                }

            } else {
                // TRƯỜNG HỢP 2: CHƯA CÓ BIẾN THỂ
                // --> An toàn để thực hiện logic "Xóa-Tạo lại"

                persistentOptions.clear(); // Xóa cũ

                // Thêm mới từ request
                int optionPosition = 1;
                for (OptionRequestDTO optionDTO : requestOptions) {
                    ProductOption option = ProductOption.builder()
                            .product(product)
                            .name(optionDTO.getName().trim())
                            .position(optionPosition++)
                            .build();

                    int valuePosition = 1;
                    List<ProductOptionValue> newValues = new ArrayList<>();
                    for (OptionValueRequestDTO valueDTO : optionDTO.getValues()) {
                        ProductOptionValue value = ProductOptionValue.builder()
                                .option(option)
                                .value(valueDTO.getValue().trim())
                                .position(valuePosition++)
                                .build();
                        newValues.add(value);
                    }
                    option.setValues(newValues);

                    persistentOptions.add(option); // Thêm vào list đang được quản lý
                }
            }
        } // <-- Kết thúc khối 'if (requestOptions != null)'

        // BƯỚC 4: Lưu lại product (luôn luôn thực hiện)
        Product updatedProduct = productRepository.save(product);

        // BƯỚC 5: Trả về DTO.
        return mapToProductDTO(updatedProduct);
    }

    // (Hàm deleteProduct đúng)
    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));

        product.setActive(false);
        productRepository.save(product);

        List<Variant> variantsToHide = variantRepository.findAllByProductIdAndActive(productId, true);
        if (!variantsToHide.isEmpty()) {
            for (Variant variant : variantsToHide) {
                variant.setActive(false);
            }
            variantRepository.saveAll(variantsToHide);
        }
    }

    // (Hàm permanentDeleteProduct đúng)
    @Override
    @Transactional
    public void permanentDeleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));

        long variantCount = variantRepository.countByProductId(productId);

        if (variantCount > 0) {
            throw new BusinessRuleException("Không thể xóa vĩnh viễn sản phẩm đang có biến thể.");
        }

        productRepository.delete(product);
    }


    // (Hàm getProductBriefList đúng)
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
                        .variantCount(variantRepository.countByProductId(p.getId()))
                        .build())
                .collect(Collectors.toList());

        return new PageResponseDTO<>(
                briefDTOs, productPage.getNumber(), productPage.getSize(),
                productPage.getTotalElements(), productPage.getTotalPages()
        );
    }

    // (Hàm getProductDetailById đúng, đã sửa ở lần trước)
    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponseDTO getProductDetailById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        // (Đã xóa check active ở đây, logic này đúng)

        ProductResponseDTO productDTO = mapToProductDTO(product);

        List<ProductResponseDTO> related = productRepository.findByCategoryIdAndIdNotAndActiveTrue(
                product.getCategory().getId(), productId, PageRequest.of(0, 4)
        ).stream().map(this::mapToProductDTO).collect(Collectors.toList());

        List<ProductOptionResponseDTO> attributeDTOs = product.getOptions()
                .stream()
                .map(option -> {
                    List<ProductOptionValueResponseDTO> valueDTOs = option.getValues()
                            .stream()
                            .map(val -> ProductOptionValueResponseDTO.builder()
                                    .id(val.getId())
                                    .value(val.getValue())
                                    .build())
                            .collect(Collectors.toList());

                    return ProductOptionResponseDTO.builder()
                            .id(option.getId())
                            .name(option.getName())
                            .values(valueDTOs)
                            .build();
                }).collect(Collectors.toList());


        return ProductDetailResponseDTO.builder()
                .product(productDTO)
                .relatedProducts(related)
                .attributes(attributeDTOs)
                .build();
    }
}