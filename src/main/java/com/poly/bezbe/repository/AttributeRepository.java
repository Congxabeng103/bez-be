package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long> {
    // JpaRepository đã đủ
}