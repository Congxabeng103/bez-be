package com.poly.bezbe.dto.response.product;

import com.poly.bezbe.dto.response.product.AttributeResponseDTO;
import com.poly.bezbe.dto.response.product.ProductResponseDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductDetailResponseDTO {
    private ProductResponseDTO product; // Thông tin sản phẩm chính
    private List<ProductResponseDTO> relatedProducts; // 4 sản phẩm liên quan

    // Danh sách các thuộc tính và giá trị (vd: Size: [S, M, L], Color: [Đỏ, Xanh])
    private List<AttributeResponseDTO> attributes;
}