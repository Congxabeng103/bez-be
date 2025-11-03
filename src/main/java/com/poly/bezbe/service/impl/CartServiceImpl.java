package com.poly.bezbe.service.impl;

import com.poly.bezbe.dto.request.AddToCartRequestDTO;
import com.poly.bezbe.dto.request.UpdateCartRequestDTO;
import com.poly.bezbe.dto.response.CartResponseDTO;
import com.poly.bezbe.entity.*; // <-- Thêm Import (Product, Promotion)
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.CartRepository;
import com.poly.bezbe.repository.VariantRepository;
import com.poly.bezbe.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode; // <-- Thêm Import
import java.time.LocalDate; // <-- Thêm Import
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final VariantRepository variantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CartResponseDTO> getCart(User user) {
        List<Cart> carts = cartRepository.findByUser(user);
        return carts.stream()
                .map(this::mapCartToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartResponseDTO addToCart(User user, AddToCartRequestDTO request) {
        Variant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể (variant)."));

        // 1. Kiểm tra tồn kho
        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Sản phẩm không đủ số lượng tồn kho.");
        }

        // 2. Kiểm tra xem sản phẩm đã có trong giỏ chưa
        Optional<Cart> existingCartItem = cartRepository.findByUserAndVariant(user, variant);

        Cart savedCart;
        if (existingCartItem.isPresent()) {
            // --- Nếu đã có: Cộng dồn số lượng ---
            Cart cart = existingCartItem.get();
            int newQuantity = cart.getQuantity() + request.getQuantity();

            if (variant.getStockQuantity() < newQuantity) {
                throw new IllegalStateException("Tổng số lượng trong giỏ vượt quá tồn kho.");
            }
            cart.setQuantity(newQuantity);

            // --- SỬA CHỖ NÀY ---
            // Cập nhật giá (lấy giá mới nhất từ variant, đã bao gồm logic sale)
            cart.setPrice(getVariantPrice(variant));
            // --- KẾT THÚC SỬA ---

            savedCart = cartRepository.save(cart);
        } else {
            // --- Nếu chưa có: Tạo mới ---
            Cart newCart = Cart.builder()
                    .user(user)
                    .variant(variant)
                    .quantity(request.getQuantity())

                    // --- SỬA CHỖ NÀY ---
                    .price(getVariantPrice(variant)) // Lấy giá (sale nếu có)
                    // --- KẾT THÚC SỬA ---

                    .build();
            savedCart = cartRepository.save(newCart);
        }

        return mapCartToDTO(savedCart);
    }

    @Override
    @Transactional
    public CartResponseDTO updateCartQuantity(User user, UpdateCartRequestDTO request) {
        Cart cartItem = cartRepository.findByUserAndVariantId(user, request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng."));

        Variant variant = cartItem.getVariant();
        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Sản phẩm không đủ số lượng tồn kho.");
        }

        cartItem.setQuantity(request.getQuantity());
        Cart savedCart = cartRepository.save(cartItem);

        return mapCartToDTO(savedCart);
    }

    @Override
    @Transactional
    public void removeFromCart(User user, Long variantId) {
        Cart cartItem = cartRepository.findByUserAndVariantId(user, variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng."));

        cartRepository.delete(cartItem);
    }

    // =================================================================
    // === THÊM HÀM HELPER MỚI NÀY (ĐÃ SỬA LỖI) ===
    // (Logic này được sao chép từ VariantServiceImpl của bạn)
    // =================================================================
    private BigDecimal getVariantPrice(Variant variant) {
        Product product = variant.getProduct();
        Promotion promotion = (product != null) ? product.getPromotion() : null;

        BigDecimal originalPrice = variant.getPrice();
        BigDecimal salePrice = null;

        if (promotion != null) {
            if (promotion.isActive()) {
                LocalDate today = LocalDate.now();
                // Check nếu hôm nay nằm trong ngày khuyến mãi
                if (!today.isBefore(promotion.getStartDate()) && !today.isAfter(promotion.getEndDate())) {

                    BigDecimal discountPercent = promotion.getDiscountValue();

                    if (discountPercent != null && originalPrice != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal discountAmount = originalPrice.multiply(discountPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        salePrice = originalPrice.subtract(discountAmount);
                        if (salePrice.compareTo(BigDecimal.ZERO) < 0) {
                            salePrice = BigDecimal.ZERO;
                        }
                    }
                }
            }
        }

        // Trả về giá sale nếu có, nếu không thì trả về giá gốc
        if (salePrice != null) {
            return salePrice;
        }
        return originalPrice;
    }

    // === Helper map Entity -> DTO (ĐÃ SỬA LỖI) ===
    private CartResponseDTO mapCartToDTO(Cart cart) {
        Variant variant = cart.getVariant();

        String attributesDesc = variant.getAttributeValues().stream()
                .map(v -> v.getAttributeValue().getAttribute().getName() + ": " + v.getAttributeValue().getValue())
                .sorted()
                .collect(Collectors.joining(", "));

        // --- SỬA LOGIC LẤY GIÁ ---
        BigDecimal originalSavedPrice = cart.getPrice(); // 1. Lấy giá ĐÃ LƯU trong DB
        BigDecimal currentLivePrice = getVariantPrice(variant); // 2. Tính giá "live"

        // 3. So sánh giá. Dùng compareTo() là an toàn nhất cho BigDecimal
        // (Nếu 2 giá trị KHÔNG bằng nhau -> priceChanged = true)
        boolean hasPriceChanged = originalSavedPrice.compareTo(currentLivePrice) != 0;
        // --- KẾT THÚC SỬA ---

        return CartResponseDTO.builder()
                .cartId(cart.getId())
                .variantId(variant.getId())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .imageUrl(variant.getImageUrl() != null ? variant.getImageUrl() : variant.getProduct().getImageUrl())
                .attributesDescription(attributesDesc)

                // --- SỬA CÁCH TRẢ VỀ ---
                .currentPrice(currentLivePrice)   // <-- Giá mới (để tính tiền)
                .originalPrice(originalSavedPrice) // <-- Giá cũ (để gạch đi)
                .priceChanged(hasPriceChanged)   // <-- Cờ (để FE biết)
                // --- KẾT THÚC SỬA ---

                .quantity(cart.getQuantity())
                .stockQuantity(variant.getStockQuantity())
                .build();
    }
}