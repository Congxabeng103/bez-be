package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    // Lọc theo trạng thái
    Page<Brand> findByNameContainingIgnoreCaseAndActive(String name, boolean active, Pageable pageable);
    Page<Brand> findAllByActive(boolean active, Pageable pageable);
    List<Brand> findAllByActiveTrue(Sort sort);

    // Lọc TẤT CẢ
    Page<Brand> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Brand> findAll(Pageable pageable);
    List<Brand> findAll(Sort sort);
}