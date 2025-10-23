package com.poly.bezbe.repository;


import com.poly.bezbe.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Hỗ trợ tìm kiếm theo tên (không phân biệt hoa/thường) VÀ phân trang
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Hỗ trợ lấy tất cả VÀ phân trang (Spring đã có sẵn, nhưng khai báo rõ)
    Page<Product> findAll(Pageable pageable);
}