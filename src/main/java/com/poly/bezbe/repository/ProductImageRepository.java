package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Product;
import com.poly.bezbe.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // Lấy tất cả ảnh của 1 sản phẩm
    List<ProductImage> findByProductId(Long productId);
    boolean existsByProductAndImageUrl(Product product, String imageUrl);

}