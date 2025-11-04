package com.poly.bezbe.repository;

import com.poly.bezbe.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // <-- Import dòng này

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Tìm bản ghi thanh toán dựa trên ID của đơn hàng.
     * (Hàm này được OrderServiceImpl dùng khi xử lý VNPAY return)
     */
    Optional<Payment> findByOrderId(Long orderId);

}