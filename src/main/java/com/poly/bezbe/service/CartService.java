package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.CartRequest;
import com.poly.bezbe.dto.response.CartResponse;
import java.util.List;

public interface CartService {
    CartResponse addToCart(CartRequest request);
    List<CartResponse> getCartByUser(Long userId);
    void removeFromCart(Long cartId);
}
