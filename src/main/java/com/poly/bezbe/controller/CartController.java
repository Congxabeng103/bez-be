package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.AddToCartRequestDTO;
import com.poly.bezbe.dto.request.UpdateCartRequestDTO;
import com.poly.bezbe.dto.response.ApiResponseDTO;
import com.poly.bezbe.dto.response.CartResponseDTO;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- IMPORT
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // <-- KHÓA TOÀN BỘ CONTROLLER
public class CartController {

    private final CartService cartService;

    // API: Lấy toàn bộ giỏ hàng
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CartResponseDTO>>> getMyCart(
            @AuthenticationPrincipal User user) {

        List<CartResponseDTO> cart = cartService.getCart(user);
        return ResponseEntity.ok(ApiResponseDTO.success(cart, "Lấy giỏ hàng thành công."));
    }

    // API: Thêm vào giỏ
    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<CartResponseDTO>> addToCart(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddToCartRequestDTO request) {
        CartResponseDTO cartItem = cartService.addToCart(user, request);
        return ResponseEntity.ok(ApiResponseDTO.success(cartItem, "Đã thêm sản phẩm vào giỏ."));
    }

    // API: Cập nhật số lượng
    @PutMapping("/update")
    public ResponseEntity<ApiResponseDTO<CartResponseDTO>> updateQuantity(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateCartRequestDTO request) {
        CartResponseDTO cartItem = cartService.updateCartQuantity(user, request);
        return ResponseEntity.ok(ApiResponseDTO.success(cartItem, "Cập nhật số lượng thành công."));
    }

    // API: Xóa khỏi giỏ
    @DeleteMapping("/remove/{variantId}")
    public ResponseEntity<ApiResponseDTO<String>> removeFromCart(
            @AuthenticationPrincipal User user,
            @PathVariable Long variantId) {
        cartService.removeFromCart(user, variantId);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Đã xóa sản phẩm khỏi giỏ hàng."));
    }
}