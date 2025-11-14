package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.product.BrandRequestDTO;
import com.poly.bezbe.dto.response.product.BrandResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Brand;
import com.poly.bezbe.entity.Product;
import com.poly.bezbe.exception.DuplicateResourceException; // <-- THÊM IMPORT
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.BrandRepository;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException; // (Giữ lại vì bạn dùng ở permanentDelete)
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    // (Hàm này đã chuẩn, có productCount)
    private BrandResponseDTO mapToBrandDTO(Brand brand) {
        long productCount = productRepository.countByBrandId(brand.getId());

        return BrandResponseDTO.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .active(brand.isActive())
                .imageUrl(brand.getImageUrl())
                .productCount(productCount)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<BrandResponseDTO> getAllBrands(Pageable pageable, String searchTerm, String status) {
        // (Logic này đã ổn)
        Page<Brand> brandPage;
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        boolean activeFilter = !"INACTIVE".equalsIgnoreCase(status);

        if ("ALL".equalsIgnoreCase(status)) {
            if (searching) brandPage = brandRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable);
            else brandPage = brandRepository.findAll(pageable);
        } else {
            if (searching) brandPage = brandRepository.findByNameContainingIgnoreCaseAndActive(searchTerm.trim(), activeFilter, pageable);
            else brandPage = brandRepository.findAllByActive(activeFilter, pageable);
        }

        List<BrandResponseDTO> dtos = brandPage.getContent().stream()
                .map(this::mapToBrandDTO).collect(Collectors.toList());
        return new PageResponseDTO<>(dtos, brandPage.getNumber(), brandPage.getSize(),
                brandPage.getTotalElements(), brandPage.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<BrandResponseDTO> getAllBrandsBrief() {
        return brandRepository.findAllByActiveTrue(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::mapToBrandDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BrandResponseDTO createBrand(BrandRequestDTO request) {
        String name = request.getName().trim();

        // === THÊM KIỂM TRA "CHECK FIRST" ===
        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Tên thương hiệu '" + name + "' đã tồn tại.");
        }
        // === KẾT THÚC THÊM ===

        Brand brand = Brand.builder()
                .name(name)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.isActive())
                .build();
        Brand saved = brandRepository.save(brand);
        return mapToBrandDTO(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BrandResponseDTO updateBrand(Long id, BrandRequestDTO request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Thương hiệu: " + id));

        String name = request.getName().trim();

        // === THÊM KIỂM TRA "CHECK FIRST" ===
        // "Nếu tên đang thay đổi VÀ tên mới trùng với của người khác"
        if (!brand.getName().equalsIgnoreCase(name) &&
                brandRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {

            throw new DuplicateResourceException("Tên thươngG hiệu '" + name + "' đã được sử dụng.");
        }
        // === KẾT THÚC THÊM ===

        brand.setName(name);
        brand.setDescription(request.getDescription());
        brand.setActive(request.isActive());
        brand.setImageUrl(request.getImageUrl());

        Brand updated = brandRepository.save(brand);
        return mapToBrandDTO(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Thương hiệu: " + id));

        // (Logic này đã chuẩn)
        brand.setActive(false);
        brandRepository.save(brand);

        List<Product> productsToHide = productRepository.findAllByBrandIdAndActive(id, true);
        if (!productsToHide.isEmpty()) {
            for (Product product : productsToHide) {
                product.setActive(false);
            }
            productRepository.saveAll(productsToHide);
        }
    }

    @Override
    @Transactional
    public void permanentDeleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Thương hiệu: " + id));

        long productCount = productRepository.countByBrandId(id);

        if (productCount > 0) {
            // === SỬA EXCEPTION (Để nhất quán với các Service khác) ===
            // Ném lỗi logic nghiệp vụ (sẽ bị bắt bởi Handler 6 trong GlobalExceptionHandler)
            throw new IllegalStateException("Không thể xóa vĩnh viễn thương hiệu đang có " + productCount + " sản phẩm.");
            // =======================================================
        }

        brandRepository.delete(brand);
    }
}