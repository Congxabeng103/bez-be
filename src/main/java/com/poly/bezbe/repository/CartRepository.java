package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Cart;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.entity.Variant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Lấy tất cả giỏ hàng của user
    List<Cart> findByUser(User user);

    // Tìm một item cụ thể (để cộng dồn số lượng)
    Optional<Cart> findByUserAndVariant(User user, Variant variant);

    // Tìm bằng variantId (để update/delete)
    Optional<Cart> findByUserAndVariantId(User user, Long variantId);
}