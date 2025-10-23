package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    // JpaRepository đã có findAll()
}