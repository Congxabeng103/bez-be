// File: com/poly/bezbe/repository/OrderDetailRepository.java
package com.poly.bezbe.repository;

import com.poly.bezbe.entity.OrderItem; // <-- (Hoặc tên Entity chi tiết đơn hàng của bạn)
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> { // <-- (Giả sử ID là Long)

    /**
     * Đếm số lần một biến thể sản phẩm xuất hiện trong BẤT KỲ chi tiết đơn hàng nào.
     */
    long countByVariantId(Long productVariantId);

    // (Lưu ý: Tên hàm 'countByProductVariantId' phải khớp
    // chính xác với tên trường 'productVariant' trong Entity 'OrderDetail' của bạn)
}