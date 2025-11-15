package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.product.ProductImageRequestDTO;
import com.poly.bezbe.dto.response.product.ProductImageResponseDTO;
import com.poly.bezbe.entity.Product;
import com.poly.bezbe.entity.ProductImage;
import com.poly.bezbe.exception.DuplicateResourceException; // <-- Import
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.ProductImageRepository;
import com.poly.bezbe.repository.ProductRepository;
import com.poly.bezbe.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    private ProductImageResponseDTO mapToDTO(ProductImage image) {
        return ProductImageResponseDTO.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                // (Không có publicId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponseDTO> getImagesByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId);
        }
        return productImageRepository.findByProductId(productId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductImageResponseDTO addImageToProduct(Long productId, ProductImageRequestDTO request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));

        String newImageUrl = request.getImageUrl();

        // === SỬA 1: KIỂM TRA TRÙNG VỚI ẢNH GỐC ===
        if (product.getImageUrl() != null && product.getImageUrl().equals(newImageUrl)) {
            throw new DuplicateResourceException("Ảnh này đã là ảnh đại diện (ảnh gốc) của sản phẩm.");
        }

        // === SỬA 2: KIỂM TRA TRÙNG TRONG ALBUM ===
        // (Hàm này yêu cầu bạn phải thêm 'existsByProductAndImageUrl' vào ProductImageRepository)
        if (productImageRepository.existsByProductAndImageUrl(product, newImageUrl)) {
            throw new DuplicateResourceException("Ảnh này đã có trong album của sản phẩm.");
        }
        // === KẾT THÚC SỬA ===

        ProductImage productImage = ProductImage.builder()
                .product(product)
                .imageUrl(newImageUrl)
                .build();

        ProductImage savedImage = productImageRepository.save(productImage);
        return mapToDTO(savedImage);
    }

    @Override
    @Transactional
    public void deleteProductImage(Long imageId) {
        if (!productImageRepository.existsById(imageId)) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh: " + imageId);
        }

        // Chỉ xóa khỏi CSDL (Cho phép ảnh mồ côi)
        productImageRepository.deleteById(imageId);
    }
}