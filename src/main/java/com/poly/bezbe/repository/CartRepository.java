package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Cart;
import com.poly.bezbe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUser(User user);
}
