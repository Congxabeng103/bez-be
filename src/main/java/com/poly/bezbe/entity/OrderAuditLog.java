package com.poly.bezbe.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_audit_logs")
@Data
public class OrderAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false) // (Đơn hàng thì luôn phải có, false là đúng)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true) // <-- SỬA LẠI THÀNH "TRUE"
    private User staff; // (Staff/User có thể null, vì hệ thống có thể tự chạy)

    // (Các sửa đổi này của bạn là tốt, giữ nguyên)
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String staffName;

    @Column(nullable = false, columnDefinition = "NVARCHAR(1000)")
    private String description;

    // (Mấy cái này ok)
    private String fieldChanged;
    private String oldValue;
    private String newValue;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}