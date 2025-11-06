package com.poly.bezbe.service.impl;

// (Thêm 2 import này ở đầu file)
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map; // <-- THÊM IMPORT NÀY

import com.poly.bezbe.dto.request.OrderRequestDTO;
import com.poly.bezbe.dto.request.UpdateStatusRequestDTO;
import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.dto.response.OrderAuditLogResponseDTO;
import com.poly.bezbe.entity.*;
import com.poly.bezbe.entity.OrderAuditLog;
import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.enums.PaymentMethod;
import com.poly.bezbe.enums.PaymentStatus;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.*;
import com.poly.bezbe.service.CouponService;
import com.poly.bezbe.service.OrderAuditLogService;
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


@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // (Các repo/service bạn đã tiêm)
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final VariantRepository variantRepository;
    private final PaymentRepository paymentRepository;
    private final CouponService couponService;
    private final VnpayService vnpayService; // (Đã xóa dòng bị trùng ở trên)
    private final CouponRepository couponRepository;
    private final OrderAuditLogService auditLogService;
    private final OrderAuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public Object createOrder(User user, OrderRequestDTO request, HttpServletRequest httpServletRequest) {
        // ... (Code của bạn giữ nguyên, đã rất tốt) ...
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

        // 4. TẠO MÃ ĐƠN HÀNG TRƯỚC KHI LƯU
        String datePart = DateTimeFormatter.ofPattern("yyMMdd").format(LocalDateTime.now());
        String randomPart = UUID.randomUUID().toString().toUpperCase().substring(0, 6);
        String orderNumber = "DH-" + datePart + "-" + randomPart;

        // 5. Tạo đơn hàng
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
        Order savedOrder = orderRepository.save(order);

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
            createPaymentRecord(savedOrder, PaymentStatus.PENDING, null); // Tạo payment PENDING
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
        String orderIdStr = request.getParameter("vnp_TxnRef"); // Đây là "26_d325"
        String transId = request.getParameter("vnp_TransactionNo");

        // (Code sửa lỗi của bạn đã chính xác)
        String actualOrderId;
        if (orderIdStr != null && orderIdStr.contains("_")) {
            actualOrderId = orderIdStr.split("_")[0]; // Lấy ra "26"
        } else {
            actualOrderId = orderIdStr;
        }

        Order order = orderRepository.findById(Long.parseLong(actualOrderId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + actualOrderId));

//        // Chỉ xử lý nếu đơn hàng đang PENDING hoặc FAILED
//        if (order.getPaymentStatus() == PaymentStatus.PENDING || order.getPaymentStatus() == PaymentStatus.FAILED) {
//
//            Payment payment = paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
//                    .orElse(null);
//
//            if (status.equals("00")) {
//                // VNPAY BÁO THÀNH CÔNG
//                order.setPaymentStatus(PaymentStatus.PAID);
//                order.setOrderStatus(OrderStatus.CONFIRMED);
//                try {
//                    subtractStockForOrder(order); // Trừ kho
//                } catch (BusinessRuleException e) {
//                    order.setOrderStatus(OrderStatus.PENDING); // Lỗi trừ kho (oversale)
//                    System.err.println("LỖI OVERSALE (VNPAY RETURN): " + e.getMessage() + " VỚI ĐƠN HÀNG " + order.getId());
//                }
//
//                if (payment != null) {
//                    payment.setStatus(PaymentStatus.PAID);
//                    payment.setTransactionId(transId);
//                    payment.setPaidAt(LocalDateTime.now());
//                    paymentRepository.save(payment);
//                } else {
//                    createPaymentRecord(order, PaymentStatus.PAID, transId);
//                }
//            } else {
//                // VNPAY BÁO THẤT BẠI
//                order.setPaymentStatus(PaymentStatus.FAILED);
//                if (payment != null) {
//                    payment.setStatus(PaymentStatus.FAILED);
//                    paymentRepository.save(payment);
//                }
//            }
//            orderRepository.save(order);
//        }

        return mapOrderToDTO(order);
    }

    // --- HÀM IPN MỚI ĐƯỢC THÊM VÀO ---
    @Override
    @Transactional
    public String handleVnpayIpn(HttpServletRequest request) {

        // --- 1. LOG KHI MỚI VÀO ---
        // (Thêm new java.util.Date() để biết chính xác thời gian nó được gọi)
        System.out.println(
                "--- LOG TEST: IPN ĐÃ GỌI VÀO SERVER --- (Lúc: " + new java.util.Date() + ")"
        );

        try {
            // Lấy tất cả tham số VNPAY trả về
            Map<String, String> vnp_Params = vnpayService.getVnpayData(request);

            // 1. Xác thực chữ ký
            boolean isValidSignature = vnpayService.validateIpnSignature(vnp_Params);
            if (!isValidSignature) {

                // --- 2. LOG KHI LỖI CHỮ KÝ ---
                System.err.println("--- LOG TEST IPN FAILED: Chữ ký (Signature) KHÔNG HỢP LỆ ---");

                return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
            }

            // ... (Lấy các thông số vnp_TxnRef, vnp_ResponseCode...)
            String vnp_TxnRef = vnp_Params.get("vnp_TxnRef");
            String vnp_ResponseCode = vnp_Params.get("vnp_ResponseCode");
            String vnp_Amount = vnp_Params.get("vnp_Amount");
            String vnp_TransactionNo = vnp_Params.get("vnp_TransactionNo");
            String vnp_PayDate = vnp_Params.get("vnp_PayDate");
            // ... (Lấy Order ID, tìm Order) ...
            String actualOrderId;
            if (vnp_TxnRef != null && vnp_TxnRef.contains("_")) {
                actualOrderId = vnp_TxnRef.split("_")[0];
            } else {
                actualOrderId = vnp_TxnRef;
            }
            Order order = orderRepository.findById(Long.parseLong(actualOrderId))
                    .orElse(null);

            // ... (Kiểm tra order == null) ...
            if (order == null) {
                System.err.println("--- LOG TEST IPN FAILED: Không tìm thấy Order ID: " + actualOrderId + " ---");
                return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
            }

            // 4. Kiểm tra số tiền
            BigDecimal vnpAmountDecimal = new BigDecimal(vnp_Amount).divide(new BigDecimal("100"));
            if (order.getTotalAmount().compareTo(vnpAmountDecimal) != 0) {

                // --- 3. LOG KHI LỖI SAI SỐ TIỀN ---
                System.err.println("--- LOG TEST IPN FAILED: Sai SỐ TIỀN (Amount Mismatch) ---");
                System.err.println("   -> Tiền trong Database (đã làm tròn): " + order.getTotalAmount());
                System.err.println("   -> Tiền VNPAY IPN gửi về: " + vnpAmountDecimal);

                return "{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}";
            }

            // ... (Kiểm tra Race Condition) ...
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                System.out.println("--- LOG TEST IPN INFO: Đơn hàng này đã PAID (do 'return' chạy trước), bỏ qua. ---");
                return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
            }

            // 6. Xử lý logic
            // ... (Code 'if ("00".equals(vnp_ResponseCode))' của bạn) ...
            if ("00".equals(vnp_ResponseCode)) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setOrderStatus(OrderStatus.CONFIRMED);
                try {
                    subtractStockForOrder(order);
                } catch (BusinessRuleException e) {
                    order.setOrderStatus(OrderStatus.PENDING);
                    System.err.println("--- LOG TEST IPN WARNING: Trừ kho thất bại (Oversale) ---");
                }
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
            }

            orderRepository.save(order);

            System.out.println("--- LOG TEST IPN SUCCESS: Đã cập nhật DB thành công! ---");
            return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";

        } catch (Exception e) {

            // --- 4. LOG KHI CÓ LỖI EXCEPTION BẤT NGỜ ---
            System.err.println("--- LOG TEST IPN FAILED: Lỗi EXCEPTION BẤT NGỜ ---");
            e.printStackTrace(); // In ra toàn bộ dấu vết lỗi màu đỏ

            return "{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}";
        }
    }
    // --- KẾT THÚC HÀM IPN MỚI ---


    // (Hàm Admin Confirm - Giữ nguyên)
    @Override
    @Transactional
    public OrderResponseDTO adminConfirmOrder(Long orderId) {
        // ... (Code của bạn giữ nguyên) ...
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
        // ... (Code của bạn giữ nguyên) ...
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

    // (Các hàm Admin Page - Giữ nguyên)
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AdminOrderDTO> getAdminOrders(Pageable pageable, String status, String searchTerm) {
        // ... (Code của bạn giữ nguyên) ...
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
        // ... (Code của bạn giữ nguyên) ...
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));
        return mapToAdminOrderDetailDTO(order);
    }

    // (Hàm updateOrderStatus - Giữ nguyên)
    @Override
    @Transactional
    public AdminOrderDTO updateOrderStatus(Long orderId, UpdateStatusRequestDTO request, User currentUser) {
        // ... (Code của bạn giữ nguyên, đã rất tốt) ...
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        OrderStatus newStatus = request.getNewStatus();
        OrderStatus oldStatus = order.getOrderStatus();
        String logDescription = null;

        if (oldStatus == OrderStatus.COMPLETED || oldStatus == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể cập nhật trạng thái cho đơn hàng đã hoàn tất hoặc đã hủy.");
        }

        switch (newStatus) {
            case CONFIRMED:
                if (oldStatus == OrderStatus.PENDING) {
                    if (order.getPaymentMethod() == PaymentMethod.COD || order.getPaymentStatus() == PaymentStatus.PAID) {
                        subtractStockForOrder(order);
                    }
                    order.setOrderStatus(OrderStatus.CONFIRMED);
                    logDescription = "Xác nhận đơn hàng, đã trừ kho.";
                }
                break;
            case SHIPPING:
                if (oldStatus == OrderStatus.CONFIRMED) {
                    order.setOrderStatus(OrderStatus.SHIPPING);
                    logDescription = "Bắt đầu giao hàng.";
                } else {
                    throw new BusinessRuleException("Phải xác nhận đơn hàng trước khi giao.");
                }
                break;
            case DELIVERED:
                if (oldStatus == OrderStatus.SHIPPING) {
                    order.setOrderStatus(OrderStatus.DELIVERED);
                    logDescription = "Đã giao hàng thành công.";
                    if (order.getPaymentMethod() == PaymentMethod.COD) {
                        order.setPaymentStatus(PaymentStatus.PAID);
                        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                        if(payment != null){
                            payment.setStatus(PaymentStatus.PAID);
                            payment.setPaidAt(LocalDateTime.now());
                            paymentRepository.save(payment);
                        }
                        logDescription += " Đã thu COD.";
                    }
                } else {
                    throw new BusinessRuleException("Đơn hàng phải được giao trước khi hoàn tất.");
                }
                break;
            case CANCELLED:
                if (oldStatus == OrderStatus.CONFIRMED ||
                        oldStatus == OrderStatus.SHIPPING ||
                        oldStatus == OrderStatus.DELIVERED ||
                        oldStatus == OrderStatus.DISPUTE) {

                    if(order.getPaymentStatus() == PaymentStatus.PAID ||
                            (order.getPaymentMethod() == PaymentMethod.COD && oldStatus != OrderStatus.PENDING) )
                    {
                        returnStockForOrder(order);
                    }
                }
                if(logDescription == null) logDescription = "Hủy đơn hàng (trước khi trừ kho).";

                order.setOrderStatus(OrderStatus.CANCELLED);

                if (order.getPaymentStatus() == PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
                    logDescription += " Chuyển sang chờ hoàn tiền.";
                }
                break;
            case PENDING:
                if (oldStatus == OrderStatus.CONFIRMED) {
                    returnStockForOrder(order);
                    order.setOrderStatus(OrderStatus.PENDING);
                    logDescription = "Hoàn đơn về 'Chờ xác nhận'. Đã hoàn kho.";
                }
                break;
            case COMPLETED:
                // 1. Đã giao (DELIVERED) -> Hoàn thành (Quy trình bình thường)
                // 2. Đang khiếu nại (DISPUTE) -> Hoàn thành (Admin đã giải quyết xong khiếu nại)
                if (oldStatus == OrderStatus.DELIVERED || oldStatus == OrderStatus.DISPUTE) {
                    order.setOrderStatus(OrderStatus.COMPLETED);

                    if(oldStatus == OrderStatus.DISPUTE){
                        logDescription = "Giải quyết khiếu nại và xác nhận hoàn tất đơn hàng.";
                    } else {
                        logDescription = "Xác nhận hoàn tất đơn hàng.";
                    }
                }
                break;
        }

        Order savedOrder = orderRepository.save(order);

        if (logDescription != null) {
            String fullDescription = String.format(
                    "Đổi trạng thái từ %s sang %s. %s",
                    oldStatus.name(), newStatus.name(), logDescription
            );
            auditLogService.logActivity(
                    savedOrder,
                    currentUser,
                    fullDescription,
                    "orderStatus",
                    oldStatus.name(),
                    newStatus.name()
            );
        }

        return mapToAdminOrderDTO(savedOrder);
    }


    // (Các hàm Helper - Giữ nguyên)
    private void createPaymentRecord(Order order, PaymentStatus status, String transactionId) {
        // ... (Code của bạn giữ nguyên) ...
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
        // ... (Code của bạn giữ nguyên) ...
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
        // ... (Code của bạn giữ nguyên) ...
        return AdminOrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomerName())
                .createdAt(order.getCreatedAt())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .build();
    }

    private AdminOrderDetailDTO mapToAdminOrderDetailDTO(Order order) {
        // ... (Code của bạn giữ nguyên) ...
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
        // ... (Code của bạn giữ nguyên) ...
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
        // ... (Code của bạn giữ nguyên) ...
        for (OrderItem item : order.getOrderItems()) {
            Variant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            variantRepository.save(variant);
        }
    }


    // (Các hàm của User - Giữ nguyên)
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserOrderDTO> getMyOrders(User user, Pageable pageable) {
        // ... (Code của bạn giữ nguyên) ...
        Page<Order> orderPage = orderRepository.findByUser(user, pageable);

        List<UserOrderDTO> dtos = orderPage.getContent().stream()
                .map(order -> UserOrderDTO.builder()
                        .id(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .createdAt(order.getCreatedAt())
                        .totalAmount(order.getTotalAmount())
                        .orderStatus(order.getOrderStatus())
                        .totalItems(order.getOrderItems().size())
                        .paymentMethod(order.getPaymentMethod())
                        .paymentStatus(order.getPaymentStatus())
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
        // ... (Code của bạn giữ nguyên) ...
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập."));
        return mapToAdminOrderDetailDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO reportDeliveryIssue(Long orderId, User user) {
        // ... (Code của bạn giữ nguyên) ...
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
        // ... (Code của bạn giữ nguyên) ...
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
        // ... (Code của bạn giữ nguyên) ...
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));

        if (order.getOrderStatus() != OrderStatus.DELIVERED && order.getOrderStatus() != OrderStatus.DISPUTE) {
            throw new BusinessRuleException("Đơn hàng chưa được giao.");
        }

        order.setOrderStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepository.save(order);

        return mapOrderToDTO(savedOrder);
    }

    @Override
    @Transactional
    public VnpayResponseDTO retryVnpayPayment(User user, Long orderId, HttpServletRequest httpServletRequest) {
        // ... (Code của bạn giữ nguyên) ...
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));

        if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new BusinessRuleException("Chức năng này chỉ dành cho đơn hàng VNPAY.");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleException("Đơn hàng này đã được thanh toán.");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Chỉ có thể thanh toán lại đơn hàng ở trạng thái 'Chờ xác nhận'.");
        }

        Optional<Payment> oldPendingPayment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PENDING);
        if (oldPendingPayment.isPresent()) {
            Payment paymentToFail = oldPendingPayment.get();
            paymentToFail.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(paymentToFail);
        }

        String paymentUrl = vnpayService.createPaymentUrl(httpServletRequest, order.getId(), order.getTotalAmount());

        createPaymentRecord(order, PaymentStatus.PENDING, null); // Tạo bản ghi PENDING mới

        return VnpayResponseDTO.builder()
                .status("OK")
                .message("Tạo link VNPAY mới thành công")
                .paymentUrl(paymentUrl)
                .build();
    }

    // (Các hàm Get History - Giữ nguyên)
    private OrderAuditLogResponseDTO mapToAuditLogDTO(OrderAuditLog log) {
        // ... (Code của bạn giữ nguyên) ...
        return OrderAuditLogResponseDTO.builder()
                .id(log.getId())
                .staffName(log.getStaffName())
                .description(log.getDescription())
                .fieldChanged(log.getFieldChanged())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderAuditLogResponseDTO> getOrderHistory(Long orderId) {
        // ... (Code của bạn giữ nguyên) ...
        List<OrderAuditLog> historyEntities =
                auditLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        return historyEntities.stream()
                .map(this::mapToAuditLogDTO)
                .collect(Collectors.toList());
    }

}