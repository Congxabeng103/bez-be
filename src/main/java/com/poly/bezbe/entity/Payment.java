// File: Payment.java (Đã sửa)
package com.poly.bezbe.entity;

import com.poly.bezbe.enums.PaymentMethod; // <-- Import
import com.poly.bezbe.enums.PaymentStatus; // <-- Import
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
// ... (các import khác)

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // --- SỬA CHỖ NÀY ---
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod; // (COD, VNPAY)

    @Column(name = "transaction_id", unique = true)
    private String transactionId; // (Của VNPay)

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // --- SỬA CHỖ NÀY ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status; // (PENDING, PAID, FAILED)
    // --- KẾT THÚC SỬA ---

    @Column(name = "paid_at")
    private LocalDateTime paidAt;



}