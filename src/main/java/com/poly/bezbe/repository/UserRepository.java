package com.poly.bezbe.repository;

import com.poly.bezbe.entity.User;
import com.poly.bezbe.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByActivationToken(String token);
    Optional<User> findByResetPasswordToken(String token);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);


    // 1. Cho Khách hàng (Role.USER)
    @Query("SELECT u FROM User u WHERE u.role = :role " +
            "AND (:searchTerm IS NULL OR (LOWER(u.firstName) LIKE LOWER(concat('%', :searchTerm, '%')) OR LOWER(u.lastName) LIKE LOWER(concat('%', :searchTerm, '%')) OR LOWER(u.email) LIKE LOWER(concat('%', :searchTerm, '%')))) " +
            "AND (:status = 'ALL' OR u.isActive = :active)") // <-- ĐÃ XÓA 'order by'
    Page<User> findByRoleAndSearch(
            @Param("role") Role role,
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("active") boolean active,
            Pageable pageable // Pageable sẽ tự động thêm "order by createdAt desc"
    );

    // 2. Cho Nhân viên (Role.ADMIN, Role.STAFF)
    @Query("SELECT u FROM User u WHERE u.role IN :roles " +
            "AND (:searchTerm IS NULL OR (LOWER(u.firstName) LIKE LOWER(concat('%', :searchTerm, '%')) OR LOWER(u.lastName) LIKE LOWER(concat('%', :searchTerm, '%')) OR LOWER(u.email) LIKE LOWER(concat('%', :searchTerm, '%')))) " +
            "AND (:status = 'ALL' OR u.isActive = :active)") // <-- ĐÃ XÓA 'order by'
    Page<User> findByRoleInAndSearch(
            @Param("roles") List<Role> roles,
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("active") boolean active,
            Pageable pageable // Pageable sẽ tự động thêm "order by createdAt desc"
    );


    long countByRole(Role role);
}