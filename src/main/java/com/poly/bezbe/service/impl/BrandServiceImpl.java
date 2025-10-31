package com.poly.bezbe.service.impl; // <-- Chú ý package con 'impl'

import com.poly.bezbe.dto.request.product.BrandRequestDTO;
import com.poly.bezbe.dto.response.product.BrandResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.Brand;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.BrandRepository;
import com.poly.bezbe.service.BrandService; // <-- Import interface
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException; // <-- THÊM IMPORT
import com.poly.bezbe.repository.ProductRepository; // <-- THÊM IMPORT
import com.poly.bezbe.entity.Product; // <-- THÊM IMPORT
import java.util.List;
import java.util.stream.Collectors;

@Service // <-- @Service được đặt ở đây
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService { // <-- Implement interface

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository; // <-- THÊM DEPENDENCY
    // --- SỬA HÀM NÀY ---
    private BrandResponseDTO mapToBrandDTO(Brand brand) {
        // Đếm số sản phẩm liên quan
        long productCount = productRepository.countByBrandId(brand.getId()); // <-- SỬA

        return BrandResponseDTO.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .active(brand.isActive())
                .imageUrl(brand.getImageUrl())
                .productCount(productCount) // <-- THÊM
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<BrandResponseDTO> getAllBrands(Pageable pageable, String searchTerm, String status) {
        Page<Brand> brandPage;
        boolean searching = searchTerm != null && !searchTerm.isBlank();
        boolean activeFilter = !"INACTIVE".equalsIgnoreCase(status);

        if ("ALL".equalsIgnoreCase(status)) {
            // Logic ALL
            if (searching) brandPage = brandRepository.findByNameContainingIgnoreCase(searchTerm.trim(), pageable);
            else brandPage = brandRepository.findAll(pageable);
        } else {
            // Logic ACTIVE/INACTIVE
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
    @Override // <-- Thêm @Override
    @Transactional(readOnly = true)
    public List<BrandResponseDTO> getAllBrandsBrief() {
        return brandRepository.findAllByActiveTrue(Sort.by(Sort.Direction.ASC, "name")) // Chỉ lấy active=true
                .stream()
                .map(this::mapToBrandDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public BrandResponseDTO createBrand(BrandRequestDTO request) {
        // (Cân nhắc thêm kiểm tra trùng tên ở đây nếu cần)
        Brand brand = Brand.builder()
                .name(request.getName().trim())
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
    @Override // <-- Thêm @Override
    @Transactional
    public BrandResponseDTO updateBrand(Long id, BrandRequestDTO request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Thương hiệu: " + id));
        brand.setName(request.getName().trim());
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

        // 1. Ẩn thương hiệu
        brand.setActive(false);
        brandRepository.save(brand);

        // 2. Ẩn hàng loạt các sản phẩm đang active thuộc thương hiệu này
        List<Product> productsToHide = productRepository.findAllByBrandIdAndActive(id, true);
        if (!productsToHide.isEmpty()) {
            for (Product product : productsToHide) {
                product.setActive(false);
            }
            productRepository.saveAll(productsToHide);
        }
    }

    // --- THÊM HÀM MỚI (XÓA VĨNH VIỄN) ---
    @Override
    @Transactional
    public void permanentDeleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Thương hiệu: " + id));

        // 1. Kiểm tra xem có sản phẩm nào không
        long productCount = productRepository.countByBrandId(id);

        if (productCount > 0) {
            // 2. Nếu có, ném lỗi, không cho xóa
            throw new DataIntegrityViolationException("Không thể xóa vĩnh viễn thương hiệu đang có sản phẩm. Chỉ có thể xóa khi không còn sản phẩm nào.");
        }

        // 3. Nếu không có, tiến hành xóa vĩnh viễn
        brandRepository.delete(brand);
    }
}