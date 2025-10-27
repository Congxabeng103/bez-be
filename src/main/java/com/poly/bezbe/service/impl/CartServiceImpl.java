package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.CartRequest;
import com.poly.bezbe.dto.response.CartResponse;
import com.poly.bezbe.entity.Cart;
import com.poly.bezbe.entity.ProductVariant;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.repository.CartRepository;
import com.poly.bezbe.repository.ProductVariantRepository;
import com.poly.bezbe.repository.UserRepository;
import com.poly.bezbe.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Override
    public CartResponse addToCart(CartRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        Cart cart = Cart.builder()
                .user(user)
                .variant(variant)
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .build();

        Cart saved = cartRepository.save(cart);
        return convertToResponse(saved);
    }

    @Override
    public List<CartResponse> getCartByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cartRepository.findByUser(user)
                .stream().map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void removeFromCart(Long cartId) {
        cartRepository.deleteById(cartId);
    }

    private CartResponse convertToResponse(Cart cart) {
        CartResponse res = new CartResponse();
        res.setId(cart.getId());
        res.setUserId(cart.getUser().getId());
        res.setFullName(cart.getUser().getFirstName() + " " + cart.getUser().getLastName());
        res.setVariantId(cart.getVariant().getId());
        res.setVariantName(cart.getVariant().getName());
        res.setQuantity(cart.getQuantity());
        res.setPrice(cart.getPrice());
        return res;
    }
}
