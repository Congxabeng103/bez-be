package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.CartRequest;
import com.poly.bezbe.dto.response.CartResponse;
import com.poly.bezbe.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@CrossOrigin("*")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping
    public CartResponse addToCart(@RequestBody CartRequest request) {
        return cartService.addToCart(request);
    }

    @GetMapping("/{userId}")
    public List<CartResponse> getCartByUser(@PathVariable Long userId) {
        return cartService.getCartByUser(userId);
    }

    @DeleteMapping("/{cartId}")
    public void removeFromCart(@PathVariable Long cartId) {
        cartService.removeFromCart(cartId);
    }
}
