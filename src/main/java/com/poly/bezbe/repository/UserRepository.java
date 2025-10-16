package com.poly.bezbe.repository;

import com.poly.bezbe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByResetPasswordToken(String token);

    // ✨ THÊM PHƯƠNG THỨC MỚI NÀY
    // Spring Data JPA sẽ tự động tạo câu lệnh query để tìm User theo activationToken.
    Optional<User> findByActivationToken(String token);
}