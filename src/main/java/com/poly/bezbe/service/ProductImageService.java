package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.product.ProductImageRequestDTO;
import com.poly.bezbe.dto.response.product.ProductImageResponseDTO;
import java.util.List;

public interface ProductImageService {
    List<ProductImageResponseDTO> getImagesByProductId(Long productId);
    ProductImageResponseDTO addImageToProduct(Long productId, ProductImageRequestDTO request);
    void deleteProductImage(Long imageId);
}