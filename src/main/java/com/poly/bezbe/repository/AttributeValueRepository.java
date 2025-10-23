package com.poly.bezbe.repository;

import com.poly.bezbe.entity.AttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Import List

@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {

    // Tìm tất cả các giá trị thuộc về một Attribute cụ thể
    List<AttributeValue> findByAttribute_Id(Long attributeId);
}