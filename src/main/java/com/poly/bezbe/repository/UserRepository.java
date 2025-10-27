package com.poly.bezbe.repository;

import com.poly.bezbe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
