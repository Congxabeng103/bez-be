
package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Không cần thêm gì, JpaRepository đã có findAll()
}
