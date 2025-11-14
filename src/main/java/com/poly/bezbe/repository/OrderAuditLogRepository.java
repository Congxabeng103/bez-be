package com.poly.bezbe.repository;

import com.poly.bezbe.entity.OrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, Long> {

    // Spring tự hiểu tên hàm: "Tìm theo OrderId, Sắp xếp theo CreatedAt Giảm dần"
    List<OrderAuditLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    Integer countByStaffId(Long userId);
}