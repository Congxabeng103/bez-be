package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.AddToCartRequestDTO;
import com.poly.bezbe.dto.request.UpdateCartRequestDTO;
import com.poly.bezbe.dto.response.CartResponseDTO;
import com.poly.bezbe.entity.User;

import java.util.List;

public interface CartService {
    /** Lấy tất cả item trong giỏ hàng của user */
    List<CartResponseDTO> getCart(User user);

    /** Thêm sản phẩm vào giỏ (hoặc cộng dồn nếu đã tồn tại) */
    CartResponseDTO addToCart(User user, AddToCartRequestDTO request);

    /** Cập nhật số lượng của một item trong giỏ */
    CartResponseDTO updateCartQuantity(User user, UpdateCartRequestDTO request);

    /** Xóa một item khỏi giỏ hàng */
    void removeFromCart(User user, Long variantId);
}