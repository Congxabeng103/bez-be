package com.poly.bezbe.service.impl;

// (Thêm 2 import này ở đầu file)
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.poly.bezbe.dto.request.OrderRequestDTO;
import com.poly.bezbe.dto.request.UpdateStatusRequestDTO;
import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.entity.*;
import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.enums.PaymentMethod;
import com.poly.bezbe.enums.PaymentStatus;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.*;
import com.poly.bezbe.service.CouponService;
import com.poly.bezbe.service.OrderService;
import com.poly.bezbe.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final VariantRepository variantRepository;
    private final PaymentRepository paymentRepository;
    private final CouponService couponService;
    private final VnpayService vnpayService;
    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public Object createOrder(User user, OrderRequestDTO request, HttpServletRequest httpServletRequest) {

        // 1. Lấy giỏ hàng
        List<Cart> cartItems = cartRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new BusinessRuleException("Giỏ hàng của bạn đang rỗng");
        }

        // 2. Tính toán
        BigDecimal subtotal = cartItems.stream()
                .map(cart -> cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Coupon coupon = couponService.validateCoupon(request.getCouponCode(), subtotal);
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (coupon != null) {
            couponDiscount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && couponDiscount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                couponDiscount = coupon.getMaxDiscountAmount();
            }
        }
        BigDecimal shippingFee = new BigDecimal("30000");
        BigDecimal totalAmount = subtotal.subtract(couponDiscount).add(shippingFee);

        // 3. Kiểm tra tồn kho
        for (Cart cartItem : cartItems) {
            Variant variant = cartItem.getVariant();
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new BusinessRuleException("Sản phẩm '" + variant.getProduct().getName() + "' không đủ tồn kho.");
            }
        }

        // --- 4. SỬA: TẠO MÃ ĐƠN HÀNG TRƯỚC KHI LƯU ---
        // (Giải quyết lỗi 'not-null property')

        // Lấy ngày tháng (vd: 251104 cho 04/11/2025)
        String datePart = DateTimeFormatter.ofPattern("yyMMdd").format(LocalDateTime.now());
        // Lấy 6 ký tự ngẫu nhiên (vd: A5B2E1)
        String randomPart = UUID.randomUUID().toString().toUpperCase().substring(0, 6);
        // Nối lại (vd: DH-251104-A5B2E1)
        String orderNumber = "DH-" + datePart + "-" + randomPart;

        // 5. Tạo đơn hàng (orderNumber đã có giá trị)
        Order order = Order.builder()
                .orderNumber(orderNumber) // <-- Gán mã đã tạo
                .user(user)
                .coupon(coupon)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .couponDiscount(couponDiscount)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .customerName(request.getCustomerName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .note(request.getNote())
                .build();

        // Lệnh save() này sẽ thành công ngay lập tức
        Order savedOrder = orderRepository.save(order);
        // --- KẾT THÚC SỬA ---

        // 6. Chuyển CartItems -> OrderItems (Không trừ kho)
        List<OrderItem> orderItems = new ArrayList<>();
        for (Cart cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .variant(cartItem.getVariant())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .totalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);

        // 7. Cập nhật lượt dùng coupon
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        // 8. Xóa giỏ hàng
        cartRepository.deleteAll(cartItems);

        // 9. Xử lý thanh toán
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            createPaymentRecord(savedOrder, PaymentStatus.PENDING, null);
            return mapOrderToDTO(savedOrder);
        } else if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            String paymentUrl = vnpayService.createPaymentUrl(httpServletRequest, savedOrder.getId(), savedOrder.getTotalAmount());
            createPaymentRecord(savedOrder, PaymentStatus.PENDING, null);
            return VnpayResponseDTO.builder()
                    .status("OK")
                    .message("Tạo link VNPAY thành công")
                    .paymentUrl(paymentUrl)
                    .build();
        }

        throw new BusinessRuleException("Phương thức thanh toán không hợp lệ");
    }

    // (Hàm VNPAY Return - Giữ nguyên)
    @Override
    @Transactional
    public OrderResponseDTO handleVnpayReturn(HttpServletRequest request) {
        String status = request.getParameter("vnp_ResponseCode");
        String orderIdStr = request.getParameter("vnp_TxnRef");
        String transId = request.getParameter("vnp_TransactionNo");

        Order order = orderRepository.findById(Long.parseLong(orderIdStr))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderIdStr));

        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            if (status.equals("00")) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setOrderStatus(OrderStatus.CONFIRMED);
                try {
                    subtractStockForOrder(order);
                } catch (BusinessRuleException e) {
                    order.setOrderStatus(OrderStatus.PENDING);
                    System.err.println("LỖI OVERSALE (VNPAY): " + e.getMessage() + " VỚI ĐƠN HÀNG " + order.getId());
                }
                Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                if (payment != null) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setTransactionId(transId);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                } else {
                    createPaymentRecord(order, PaymentStatus.PAID, transId);
                }
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
                Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                if (payment != null) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }
            }
            orderRepository.save(order);
        }
        return mapOrderToDTO(order);
    }

    // (Hàm Admin Confirm - Giữ nguyên)
    @Override
    @Transactional
    public OrderResponseDTO adminConfirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Đơn hàng không ở trạng thái Chờ xác nhận");
        }
        subtractStockForOrder(order);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);
        return mapOrderToDTO(savedOrder);
    }

    // (Hàm Admin Cancel - Giữ nguyên)
    @Override
    @Transactional
    public OrderResponseDTO adminCancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));
        OrderStatus currentStatus = order.getOrderStatus();

        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.COMPLETED) {
            throw new BusinessRuleException("Không thể hủy đơn hàng đã hoàn tất hoặc đã hủy.");
        }
        if (currentStatus == OrderStatus.CONFIRMED ||
                currentStatus == OrderStatus.SHIPPING ||
                currentStatus == OrderStatus.DELIVERED) {
            returnStockForOrder(order);
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
        }
        Order savedOrder = orderRepository.save(order);
        return mapOrderToDTO(savedOrder);
    }

    // --- CÁC HÀM MỚI CHO ADMIN PAGE (Giữ nguyên) ---

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AdminOrderDTO> getAdminOrders(Pageable pageable, String status, String searchTerm) {
        OrderStatus statusEnum = null;
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            statusEnum = OrderStatus.valueOf(status.toUpperCase());
        }

        Page<Order> orderPage = orderRepository.findByAdminFilters(statusEnum, searchTerm, pageable);

        List<AdminOrderDTO> dtos = orderPage.getContent().stream()
                .map(this::mapToAdminOrderDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(
                dtos,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailDTO getAdminOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));
        return mapToAdminOrderDetailDTO(order);
    }

    @Override
    @Transactional
    public AdminOrderDTO updateOrderStatus(Long orderId, UpdateStatusRequestDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        OrderStatus newStatus = request.getNewStatus();
        OrderStatus currentStatus = order.getOrderStatus();

        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể cập nhật trạng thái cho đơn hàng đã hoàn tất hoặc đã hủy.");
        }

        switch (newStatus) {
            case CONFIRMED:
                if (currentStatus == OrderStatus.PENDING) {
                    if (order.getPaymentMethod() == PaymentMethod.COD || order.getPaymentStatus() == PaymentStatus.PAID) {
                        subtractStockForOrder(order);
                    }
                    order.setOrderStatus(OrderStatus.CONFIRMED);
                }
                break;

            case SHIPPING:
                if (currentStatus == OrderStatus.CONFIRMED) {
                    order.setOrderStatus(OrderStatus.SHIPPING);
                } else {
                    throw new BusinessRuleException("Phải xác nhận đơn hàng trước khi giao.");
                }
                break;

            case DELIVERED:
                if (currentStatus == OrderStatus.SHIPPING) {
                    order.setOrderStatus(OrderStatus.DELIVERED);
                    if (order.getPaymentMethod() == PaymentMethod.COD) {
                        order.setPaymentStatus(PaymentStatus.PAID);
                        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                        if(payment != null){
                            payment.setStatus(PaymentStatus.PAID);
                            payment.setPaidAt(LocalDateTime.now());
                            paymentRepository.save(payment);
                        }
                    }
                } else {
                    throw new BusinessRuleException("Đơn hàng phải được giao trước khi hoàn tất.");
                }
                break;

            case CANCELLED:
                // 1. (Xóa check: Không cho hủy khi đang giao)
                // Admin có quyền hủy (ví dụ: giao thất bại)

                // 2. TRẢ KHO: Chỉ trả kho nếu đã 'CONFIRMED' trở đi (vì PENDING chưa trừ)
                if (currentStatus == OrderStatus.CONFIRMED ||
                        currentStatus == OrderStatus.SHIPPING ||
                        currentStatus == OrderStatus.DELIVERED ||
                        currentStatus == OrderStatus.DISPUTE) {

                    // Chỉ trả kho nếu kho đã bị trừ
                    // (Đơn VNPAY (PAID) và đơn COD (CONFIRMED) mới bị trừ kho)
                    if(order.getPaymentStatus() == PaymentStatus.PAID ||
                            (order.getPaymentMethod() == PaymentMethod.COD && currentStatus != OrderStatus.PENDING) )
                    {
                        returnStockForOrder(order); // Trả kho
                    }
                }

                order.setOrderStatus(OrderStatus.CANCELLED);

                // 3. HOÀN TIỀN (nếu VNPAY đã thanh toán)
                if (order.getPaymentStatus() == PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
                }
                break;
            // --- KẾT THÚC SỬA ---

            case PENDING:
                if (currentStatus == OrderStatus.CONFIRMED) {
                    returnStockForOrder(order);
                    order.setOrderStatus(OrderStatus.PENDING);
                }
                break;

            case COMPLETED:
                if (currentStatus == OrderStatus.DELIVERED) {
                    order.setOrderStatus(OrderStatus.COMPLETED);
                }
                break;
        }

        Order savedOrder = orderRepository.save(order);
        return mapToAdminOrderDTO(savedOrder);
    }


    // --- CÁC HÀM HELPER (Private và Public) ---

    private void createPaymentRecord(Order order, PaymentStatus status, String transactionId) {
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(order.getPaymentMethod())
                .amount(order.getTotalAmount())
                .status(status)
                .transactionId(transactionId)
                .build();
        if (status == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);
    }

    private OrderResponseDTO mapOrderToDTO(Order order) {
        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .customerName(order.getCustomerName())
                .phone(order.getPhone())
                .address(order.getAddress())
                .build();
    }

    private AdminOrderDTO mapToAdminOrderDTO(Order order) {
        return AdminOrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomerName())
                .createdAt(order.getCreatedAt())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

    private AdminOrderDetailDTO mapToAdminOrderDetailDTO(Order order) {
        List<AdminOrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .map(item -> {
                    Variant variant = item.getVariant();
                    String variantInfo = variant.getAttributeValues().stream()
                            .map(v -> v.getAttributeValue().getAttribute().getName() + ": " + v.getAttributeValue().getValue())
                            .collect(Collectors.joining(", "));

                    return AdminOrderItemDTO.builder()
                            .variantId(variant.getId())
                            .productName(variant.getProduct().getName())
                            .variantInfo(variantInfo)
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .imageUrl(variant.getImageUrl() != null ? variant.getImageUrl() : variant.getProduct().getImageUrl())
                            .build();
                }).collect(Collectors.toList());

        return AdminOrderDetailDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .createdAt(order.getCreatedAt())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .customerName(order.getCustomerName())
                .phone(order.getPhone())
                .email(order.getEmail())
                .address(order.getAddress())
                .note(order.getNote())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .couponDiscount(order.getCouponDiscount())
                .totalAmount(order.getTotalAmount())
                .items(itemDTOs)
                .build();
    }

    public void subtractStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Variant variant = item.getVariant();
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new BusinessRuleException("Sản phẩm '" + variant.getProduct().getName() + "' (ID: " + variant.getId() + ") không đủ tồn kho.");
            }
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            variantRepository.save(variant);
        }
    }

    public void returnStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Variant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            variantRepository.save(variant);
        }
    }


    // --- THÊM 2 HÀM MỚI NÀY CHO USER ---

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserOrderDTO> getMyOrders(User user, Pageable pageable) {
        // Sửa: Gọi hàm 'findByUser' (thay vì 'findByUserOrderByCreatedAtDesc')
        Page<Order> orderPage = orderRepository.findByUser(user, pageable);

        List<UserOrderDTO> dtos = orderPage.getContent().stream()
                .map(order -> UserOrderDTO.builder()
                        .id(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .createdAt(order.getCreatedAt())
                        .totalAmount(order.getTotalAmount())
                        .orderStatus(order.getOrderStatus())
                        .totalItems(order.getOrderItems().size()) // Đếm số lượng item
                        .build())
                .collect(Collectors.toList());

        return new PageResponseDTO<>(
                dtos,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailDTO getMyOrderDetail(User user, Long orderId) {
        // Hàm findByIdAndUser đảm bảo user chỉ xem được đơn hàng của chính mình
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập."));

        // Chúng ta có thể dùng lại mapper của Admin vì nó đã đầy đủ
        return mapToAdminOrderDetailDTO(order);
    }
    // (Thêm 3 hàm mới cho User)
    @Override
    @Transactional
    public OrderResponseDTO reportDeliveryIssue(Long orderId, User user) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Chỉ có thể khiếu nại đơn hàng ở trạng thái 'Đã giao'.");
        }

        order.setOrderStatus(OrderStatus.DISPUTE);
        Order savedOrder = orderRepository.save(order);

        return mapOrderToDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO userCancelOrder(Long orderId, User user) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));

        OrderStatus currentStatus = order.getOrderStatus();

        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            throw new BusinessRuleException("Bạn không thể hủy đơn hàng ở trạng thái này.");
        }

        if (currentStatus == OrderStatus.CONFIRMED) {
            if(order.getPaymentStatus() == PaymentStatus.PAID ||
                    (order.getPaymentMethod() == PaymentMethod.COD) )
            {
                returnStockForOrder(order);
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
        }

        Order savedOrder = orderRepository.save(order);
        return mapOrderToDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO userConfirmDelivery(Long orderId, User user) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Đơn hàng chưa được giao.");
        }

        order.setOrderStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepository.save(order);

        return mapOrderToDTO(savedOrder);
    }
}