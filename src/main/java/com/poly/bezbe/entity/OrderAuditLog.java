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
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User staff;

    // --- SỬA Ở ĐÂY ---
    // (Giống như cách bạn làm với User.java)
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String staffName;

    // --- SỬA Ở ĐÂY ---
    // Phải là "NTEXT" (Unicode) thay vì "TEXT" (Non-Unicode)
    // Hoặc dùng NVARCHAR với độ dài lớn
    @Column(nullable = false, columnDefinition = "NVARCHAR(1000)")
    private String description;

    // (Mấy cái này thường là ENUM hoặc code, không cần NVARCHAR)
    private String fieldChanged;
    private String oldValue;
    private String newValue;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}