package com.poly.bezbe.entity;

// ... (tất cả import: Enums, List, BigDecimal...)
import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.enums.PaymentMethod;
import com.poly.bezbe.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // --- THÊM TRƯỜNG NÀY ---
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;
    // --- KẾT THÚC THÊM ---
    // (Các liên kết này đã đúng)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    // (Các trường tính toán này đã đúng)
    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee;

    @Column(name = "coupon_discount")
    private BigDecimal couponDiscount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    // (Các trường Enum này đã đúng)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    // --- BẮT BUỘC: Thêm các trường SNAPSHOT địa chỉ ---
    // (Sao chép từ form checkout của FE)

    @Column(name = "customer_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String customerName; // (Lấy từ formData.fullName)

    @Column(name = "email", columnDefinition = "NVARCHAR(255)")
    private String email; // (Lấy từ formData.email)

    @Column(name = "phone", nullable = false, columnDefinition = "VARCHAR(20)")
    private String phone; // (Lấy từ formData.phone)

    @Column(name = "address", nullable = false, columnDefinition = "NVARCHAR(500)")
    private String address; // (Lấy từ formData.address)

    @Column(name = "note", columnDefinition = "NVARCHAR(1000)")
    private String note; // (Lấy từ formData.note)

    // --- KẾT THÚC THÊM ---

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments;
}