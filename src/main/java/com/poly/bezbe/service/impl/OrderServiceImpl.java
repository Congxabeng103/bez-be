package com.poly.bezbe.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poly.bezbe.dto.request.OrderRequestDTO;
import com.poly.bezbe.dto.request.UpdateStatusRequestDTO;
import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.dto.response.OrderAuditLogResponseDTO;
import com.poly.bezbe.entity.*;
import com.poly.bezbe.entity.OrderAuditLog;
import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.enums.PaymentMethod;
import com.poly.bezbe.enums.PaymentStatus;
import com.poly.bezbe.enums.Role;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.*;
import com.poly.bezbe.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;


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
    private final OrderAuditLogService auditLogService;
    private final OrderAuditLogRepository auditLogRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public Object createOrder(User user, OrderRequestDTO request, HttpServletRequest httpServletRequest) {

        List<Cart> cartItems = cartRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new BusinessRuleException("Giỏ hàng của bạn đang rỗng");
        }

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

        for (Cart cartItem : cartItems) {
            Variant variant = cartItem.getVariant();
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new BusinessRuleException("Sản phẩm '" + variant.getProduct().getName() + "' không đủ tồn kho.");
            }
        }

        String datePart = DateTimeFormatter.ofPattern("yyMMdd").format(LocalDateTime.now());
        String randomPart = UUID.randomUUID().toString().toUpperCase().substring(0, 6);
        String orderNumber = "DH-" + datePart + "-" + randomPart;

        Order order = Order.builder()
                .orderNumber(orderNumber)
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


        auditLogService.logActivity(
                savedOrder,
                "Khách hàng",
                "Đơn hàng đã được tạo.",
                "orderStatus",
                null,
                OrderStatus.PENDING.name()
        );


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

        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        cartRepository.deleteAll(cartItems);

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            createPaymentRecord(savedOrder, PaymentStatus.PENDING, null);
            emailService.sendOrderConfirmationEmail(savedOrder);
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

    @Override
    @Transactional
    public OrderResponseDTO handleVnpayReturn(HttpServletRequest request) {
        String status = request.getParameter("vnp_ResponseCode");
        String orderIdStr = request.getParameter("vnp_TxnRef");
        String transId = request.getParameter("vnp_TransactionNo");

        String actualOrderId;
        if (orderIdStr != null && orderIdStr.contains("_")) {
            actualOrderId = orderIdStr.split("_")[0];
        } else {
            actualOrderId = orderIdStr;
        }

        Order order = orderRepository.findById(Long.parseLong(actualOrderId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + actualOrderId));

        return mapOrderToDTO(order);
    }

    @Override
    @Transactional
    public String handleVnpayIpn(HttpServletRequest request) {
        try {
            Map<String, String> vnp_Params = vnpayService.getVnpayData(request);

            boolean isValidSignature = vnpayService.validateIpnSignature(vnp_Params);
            if (!isValidSignature) {
                System.err.println("--- LOG TEST IPN FAILED: Chữ ký (Signature) KHÔNG HỢP LỆ ---");
                return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
            }

            String vnp_TxnRef = vnp_Params.get("vnp_TxnRef");
            String vnp_ResponseCode = vnp_Params.get("vnp_ResponseCode");
            String vnp_Amount = vnp_Params.get("vnp_Amount");
            String vnp_TransactionNo = vnp_Params.get("vnp_TransactionNo");
            String vnp_PayDate = vnp_Params.get("vnp_PayDate");
            String actualOrderId;
            if (vnp_TxnRef != null && vnp_TxnRef.contains("_")) {
                actualOrderId = vnp_TxnRef.split("_")[0];
            } else {
                actualOrderId = vnp_TxnRef;
            }
            Order order = orderRepository.findById(Long.parseLong(actualOrderId))
                    .orElse(null);

            if (order == null) {
                return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
            }

            BigDecimal vnpAmountDecimal = new BigDecimal(vnp_Amount).divide(new BigDecimal("100"));
            if (order.getTotalAmount().compareTo(vnpAmountDecimal) != 0) {
                return "{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}";
            }

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                System.out.println("--- LOG TEST IPN INFO: Đơn hàng này đã PAID (do 'return' chạy trước), bỏ qua. ---");
                return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
            }

            if ("00".equals(vnp_ResponseCode)) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setOrderStatus(OrderStatus.CONFIRMED);

                auditLogService.logActivity(
                        order,
                        "Hệ thống",
                        "Thanh toán VNPAY thành công (IPN).",
                        "paymentStatus",
                        PaymentStatus.PENDING.name(),
                        PaymentStatus.PAID.name()
                );

                auditLogService.logActivity(
                        order,
                        "Hệ thống",
                        "Đơn hàng tự động xác nhận sau khi thanh toán VNPAY.",
                        "orderStatus",
                        OrderStatus.PENDING.name(),
                        OrderStatus.CONFIRMED.name()
                );
                emailService.sendPaymentSuccessEmail(order);
                try {
                    subtractStockForOrder(order);
                } catch (BusinessRuleException e) {
                    order.setOrderStatus(OrderStatus.PENDING);
                    System.err.println("--- LOG TEST IPN WARNING: Trừ kho thất bại (Oversale) ---");
                }


                Payment payment = paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
                        .orElse(null);

                if (payment != null) {
                    payment.setStatus(PaymentStatus.PAID);

                    payment.setTransactionId(vnp_TransactionNo);
                    payment.setVnpTxnRef(vnp_TxnRef);
                    try {
                        DateTimeFormatter vnpFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                        LocalDateTime paidTime = LocalDateTime.parse(vnp_PayDate, vnpFormatter);
                        payment.setPaidAt(paidTime);
                    } catch (Exception e) {
                        payment.setPaidAt(LocalDateTime.now());
                    }

                    paymentRepository.save(payment);
                }

            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);

                Payment payment = paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
                        .orElse(null);
                if (payment != null) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setTransactionId(vnp_TransactionNo);
                    paymentRepository.save(payment);
                }
            }

            orderRepository.save(order);

            System.out.println("--- LOG TEST IPN SUCCESS: Đã cập nhật DB thành công! ---");
            return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";

        } catch (Exception e) {

            System.err.println("--- LOG TEST IPN FAILED: Lỗi EXCEPTION BẤT NGỜ ---");
            e.printStackTrace();

            return "{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AdminOrderDTO> getAdminOrders(Pageable pageable, String status, String searchTerm) {

        Page<Order> orderPage;


        // 1. Xử lý Tab "Chờ hoàn tiền" (PENDING_REFUND)
        if ("PENDING_REFUND".equalsIgnoreCase(status)) {

            orderPage = orderRepository.findOrdersPendingRefund(searchTerm, pageable);

        } else if ("PENDING_STOCK_RETURN".equalsIgnoreCase(status)) {

            orderPage = orderRepository.findOrdersPendingStockReturn(searchTerm, pageable);

        } else {
            OrderStatus statusEnum = null;
            if (status != null && !status.equalsIgnoreCase("ALL")) {
                try {
                    statusEnum = OrderStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {

                    System.err.println("Trạng thái lọc không hợp lệ: " + status + ". Trả về 'ALL'.");
                    statusEnum = null;
                }
            }
            orderPage = orderRepository.findByAdminFilters(statusEnum, searchTerm, pageable);
        }

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
    public AdminOrderDTO updateOrderStatus(Long orderId, UpdateStatusRequestDTO request, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        OrderStatus newStatus = request.getNewStatus();
        OrderStatus oldStatus = order.getOrderStatus();
        String adminNote = request.getNote();
        String logDescription = null;

        if (newStatus == OrderStatus.CANCELLED &&
                (oldStatus == OrderStatus.CONFIRMED || oldStatus == OrderStatus.SHIPPING || oldStatus == OrderStatus.DELIVERED || oldStatus == OrderStatus.DISPUTE)) {


            if (currentUser.getRole() == Role.STAFF) {
                throw new AccessDeniedException("Nhân viên không có quyền hủy đơn hàng đã giao hoặc đang khiếu nại.");
            }
        }

        if (newStatus == OrderStatus.PENDING) {
            if (currentUser.getRole() == Role.STAFF) {
                throw new AccessDeniedException("Nhân viên không có quyền hoàn tác đơn hàng về 'Chờ xác nhận'.");
            }
        }

        if (oldStatus == OrderStatus.COMPLETED || oldStatus == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể cập nhật trạng thái cho đơn hàng đã hoàn tất hoặc đã hủy.");
        }
        if (oldStatus == newStatus) {
            throw new BusinessRuleException("Đơn hàng đã ở trạng thái này.");
        }

        switch (newStatus) {
            case PENDING:
                if (oldStatus == OrderStatus.CONFIRMED) {
                    returnStockForOrder(order);
                    order.setOrderStatus(OrderStatus.PENDING);
                    logDescription = "Hoàn đơn về 'Chờ xác nhận'. Đã hoàn kho.";
                } else {
                    throw new BusinessRuleException("Chỉ có thể hoàn tác từ 'Đã xác nhận' về 'Chờ xác nhận'.");
                }
                break;
            case CONFIRMED:
                if (oldStatus == OrderStatus.PENDING) {
                    if (order.getPaymentMethod() == PaymentMethod.COD || order.getPaymentStatus() == PaymentStatus.PAID) {
                        subtractStockForOrder(order);
                        logDescription = "Đã xác nhận đơn hàng, đã trừ kho.";
                    } else {
                        logDescription = "Đã xác nhận đơn hàng (chưa trừ kho, chờ thanh toán).";
                    }
                    order.setOrderStatus(OrderStatus.CONFIRMED);
                    emailService.sendOrderConfirmedEmail(order);
                }
                break;
            case SHIPPING:
                if (oldStatus == OrderStatus.CONFIRMED) {
                    order.setOrderStatus(OrderStatus.SHIPPING);
                    logDescription = "Bắt đầu giao hàng.";
                    if (adminNote != null && !adminNote.isBlank()) {
                        logDescription += " Mã vận đơn: " + adminNote;
                    }
                    emailService.sendShippingNotificationEmail(order);
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
                    emailService.sendOrderDeliveredEmail(order);
                } else {
                    throw new BusinessRuleException("Đơn hàng phải được giao trước khi hoàn tất.");
                }
                break;
            case CANCELLED:
                if (adminNote == null || adminNote.isBlank()) {
                    throw new BusinessRuleException("Để hủy đơn bắt buộc phải nhập lý do.");
                }

                if (oldStatus == OrderStatus.CONFIRMED) {
                    returnStockForOrder(order);
                    order.setStockReturned(true);
                    logDescription = "Đơn hàng bị hủy. ĐÃ TỰ ĐỘNG HOÀN KHO. Lý do: " + adminNote;

                } else if (oldStatus == OrderStatus.PENDING) {
                    order.setStockReturned(true);
                    logDescription = "Đơn hàng bị hủy. (Không cần hoàn kho). Lý do: " + adminNote;

                } else {

                    order.setStockReturned(false);
                    logDescription = "Đơn hàng bị hủy/trả hàng. (CHỜ NHẬP KHO). Lý do: " + adminNote;
                }

                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setCancellationReason(adminNote);

                if (order.getPaymentStatus() == PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
                    logDescription += " Chuyển sang chờ hoàn tiền.";
                }

                emailService.sendOrderCancellationEmail(order, adminNote);
                break;
            case COMPLETED:
                if (oldStatus == OrderStatus.DELIVERED || oldStatus == OrderStatus.DISPUTE) {
                    order.setOrderStatus(OrderStatus.COMPLETED);
                    logDescription = (oldStatus == OrderStatus.DISPUTE)
                            ? "Giải quyết khiếu nại và xác nhận hoàn tất đơn hàng."
                            : "Xác nhận hoàn tất đơn hàng.";
                } else {
                    throw new BusinessRuleException("Chỉ hoàn tất đơn hàng đã giao hoặc đang khiếu nại.");
                }
                break;
            case DISPUTE:
                order.setOrderStatus(OrderStatus.DISPUTE);
                order.setDisputeReason(adminNote);
                logDescription = "Đã đánh dấu đơn hàng là 'Khiếu nại'. Lý do: " + (adminNote != null ? adminNote : "Không có");
                break;
            default:
                throw new BusinessRuleException("Trạng thái cập nhật không hợp lệ: " + newStatus);
        }

        Order savedOrder = orderRepository.save(order);

        if (logDescription != null) {
            auditLogService.logActivity(
                    savedOrder,
                    currentUser,
                    logDescription,
                    "orderStatus",
                    oldStatus.name(),
                    newStatus.name()
            );
        }

        return mapToAdminOrderDTO(savedOrder);
    }


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
                .note(order.getNote())
                .cancellationReason(order.getCancellationReason())
                .disputeReason(order.getDisputeReason())
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
                .paymentMethod(order.getPaymentMethod())
                .stockReturned(order.isStockReturned())
                .build();
    }


    private AdminOrderDetailDTO mapToAdminOrderDetailDTO(Order order) {
        List<AdminOrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .map(item -> {
                    Variant variant = item.getVariant();


                    String variantInfo = Optional.ofNullable(variant.getOptionValues())
                            .orElse(java.util.Collections.emptySet()).stream()
                            .map(vov -> {
                                String optionName = vov.getOption().getName();
                                String optionValue = vov.getOptionValue().getValue();
                                return optionName + ": " + optionValue;
                            })
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
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .couponDiscount(order.getCouponDiscount())
                .totalAmount(order.getTotalAmount())
                .items(itemDTOs)
                .note(order.getNote())
                .cancellationReason(order.getCancellationReason())
                .disputeReason(order.getDisputeReason())
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


    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserOrderDTO> getMyOrders(User user, Pageable pageable) {
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
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập."));
        return mapToAdminOrderDetailDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO reportDeliveryIssue(Long orderId, User user,String reason) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Chỉ có thể khiếu nại đơn hàng ở trạng thái 'Đã giao'.");
        }

        order.setOrderStatus(OrderStatus.DISPUTE);
        order.setDisputeReason(reason);
        Order savedOrder = orderRepository.save(order);
        auditLogService.logActivity(
                savedOrder,
                "Khách hàng",
                "Khách hàng gửi khiếu nại Lý do: " + reason,
                "orderStatus",
                OrderStatus.DELIVERED.name(),
                OrderStatus.DISPUTE.name()
        );
        emailService.sendDisputeReceivedEmail(savedOrder, reason);
        return mapOrderToDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO userCancelOrder(Long orderId, User user,String reason) {
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
        order.setCancellationReason(reason);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
        }

        Order savedOrder = orderRepository.save(order);
        auditLogService.logActivity(
                savedOrder,
                "Khách hàng",
                "Khách hàng đã hủy đơn hàng. Lý do: " + reason,
                "orderStatus",
                currentStatus.name(),
                OrderStatus.CANCELLED.name()
        );
        emailService.sendOrderCancellationEmail(savedOrder,reason);

        return mapOrderToDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO userConfirmDelivery(Long orderId, User user) {

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));
        OrderStatus oldStatus = order.getOrderStatus();
        if (order.getOrderStatus() != OrderStatus.DELIVERED && order.getOrderStatus() != OrderStatus.DISPUTE) {
            throw new BusinessRuleException("Đơn hàng chưa được giao.");
        }

        order.setOrderStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepository.save(order);
        auditLogService.logActivity(
                savedOrder,
                "Khách hàng",
                "Khách hàng xác nhận đã nhận hàng.",
                "orderStatus",
                oldStatus.name(),
                OrderStatus.COMPLETED.name()
        );
        return mapOrderToDTO(savedOrder);
    }

    @Override
    @Transactional
    public VnpayResponseDTO retryVnpayPayment(User user, Long orderId, HttpServletRequest httpServletRequest) {
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

        createPaymentRecord(order, PaymentStatus.PENDING, null);

        return VnpayResponseDTO.builder()
                .status("OK")
                .message("Tạo link VNPAY mới thành công")
                .paymentUrl(paymentUrl)
                .build();
    }

    private OrderAuditLogResponseDTO mapToAuditLogDTO(OrderAuditLog log) {
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
        List<OrderAuditLog> historyEntities =
                auditLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        return historyEntities.stream()
                .map(this::mapToAuditLogDTO)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public RefundResponseDTO requestVnpayRefund(
            Long orderId,
            HttpServletRequest request,
            User currentUser
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));


        Payment payment = paymentRepository
                .findByOrderIdAndStatusIn(orderId, List.of(PaymentStatus.PAID, PaymentStatus.PENDING_REFUND))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy giao dịch VNPAY hợp lệ cho đơn hàng này."));

        if (payment.getVnpTxnRef() == null ||
                payment.getTransactionId() == null ||
                payment.getPaidAt() == null) {

            System.err.println("--- LỖI HOÀN TIỀN: Đơn hàng " + orderId + " thiếu thông tin ---");
            System.err.println("vnpTxnRef: " + payment.getVnpTxnRef());
            System.err.println("transactionId: " + payment.getTransactionId());
            System.err.println("paidAt: " + payment.getPaidAt());

            throw new BusinessRuleException("Thiếu thông tin giao dịch VNPAY (TxnRef, TransactionNo, PaidAt). Không thể hoàn tiền. (Có thể đây là đơn hàng cũ?)");
        }

        String vnp_TxnRef = payment.getVnpTxnRef();
        String vnp_TransactionNo = payment.getTransactionId();
        String vnp_TransactionDate = payment.getPaidAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        BigDecimal vnp_Amount = payment.getAmount();
        String vnp_CreateBy = currentUser.getEmail();
        String vnp_IpAddr = vnpayService.getIpAddress(request);

        String vnpayResponseJson = vnpayService.requestRefund(
                vnp_TxnRef,
                vnp_TransactionNo,
                vnp_TransactionDate,
                vnp_Amount,
                vnp_CreateBy,
                vnp_IpAddr
        );

        Map<String, String> vnpayResponseMap;
        try {
            vnpayResponseMap = new ObjectMapper().readValue(vnpayResponseJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new BusinessRuleException("Lỗi khi đọc phản hồi từ VNPAY: " + e.getMessage());
        }

        String vnp_ResponseCode = vnpayResponseMap.get("vnp_ResponseCode");
        String vnp_Message = vnpayResponseMap.getOrDefault("vnp_Message", "Không có thông báo từ VNPAY");

        if ("00".equals(vnp_ResponseCode)) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setStatus(PaymentStatus.REFUNDED);

            orderRepository.save(order);
            paymentRepository.save(payment);
            auditLogService.logActivity(
                    order,
                    currentUser,
                    "Thực hiện hoàn tiền VNPAY thành công. Số tiền: " + vnp_Amount +
                            ". Mã GD VNPAY: " + vnp_TransactionNo,
                    "paymentStatus",
                    PaymentStatus.PENDING_REFUND.name(),
                    PaymentStatus.REFUNDED.name()
            );
            emailService.sendOrderRefundNotificationEmail(order, vnp_Amount);

            return RefundResponseDTO.builder()
                    .orderId(orderId)
                    .newPaymentStatus(PaymentStatus.REFUNDED)
                    .message("Hoàn tiền VNPAY thành công!")
                    .vnpayResponseCode(vnp_ResponseCode)
                    .build();
        } else {

            auditLogService.logActivity(
                    order,
                    currentUser,
                    "Yêu cầu hoàn tiền VNPAY THẤT BẠI. Mã lỗi: " + vnp_ResponseCode +
                            ". Lý do: " + vnp_Message,
                    "paymentStatus",
                    PaymentStatus.PENDING_REFUND.name(),
                    PaymentStatus.PENDING_REFUND.name()
            );
            throw new BusinessRuleException("Hoàn tiền VNPAY thất bại: " + vnp_Message + " (Mã lỗi: " + vnp_ResponseCode + ")");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderAuditLogResponseDTO> getMyOrderHistory(Long orderId, User user) {

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập."));


        List<OrderAuditLog> historyEntities =
                auditLogRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());

        return historyEntities.stream()
                .map(this::mapToAuditLogDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RefundResponseDTO confirmCodRefund(Long orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));


        if (order.getPaymentMethod() != PaymentMethod.COD) {
            throw new BusinessRuleException("Chức năng này chỉ dành cho đơn hàng COD.");
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING_REFUND) {
            throw new BusinessRuleException("Đơn hàng không ở trạng thái 'Chờ hoàn tiền'.");
        }

        PaymentStatus oldStatus = order.getPaymentStatus();
        order.setPaymentStatus(PaymentStatus.REFUNDED);

        Payment payment = paymentRepository
                .findByOrderIdAndStatus(orderId, PaymentStatus.PENDING_REFUND)
                .orElse(null);

        if (payment == null) {

            payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PENDING)
                    .orElse(null);
        }

        if (payment != null) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        orderRepository.save(order);

        auditLogService.logActivity(
                order,
                currentUser,
                "Admin xác nhận hoàn tiền COD (chuyển trạng thái).",
                "paymentStatus",
                oldStatus.name(),
                PaymentStatus.REFUNDED.name()
        );
        emailService.sendOrderRefundNotificationEmail(order, order.getTotalAmount());


        return RefundResponseDTO.builder()
                .orderId(orderId)
                .newPaymentStatus(PaymentStatus.REFUNDED)
                .message("Xác nhận hoàn tiền COD thành công!")
                .vnpayResponseCode(null)
                .build();
    }
    @Override
    @Transactional
    public AdminOrderDTO confirmStockReturn(Long orderId, User currentUser) {
        if (currentUser.getRole() == Role.STAFF) {
            throw new AccessDeniedException("Nhân viên không có quyền xác nhận hoàn kho.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));
        OrderStatus oldStatus = order.getOrderStatus();

        if (order.getOrderStatus() != OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Đơn hàng phải ở trạng thái Đã hủy mới được nhập kho.");
        }

        if (order.isStockReturned()) {
            throw new BusinessRuleException("Đơn hàng này đã được nhập kho trước đó.");
        }


        returnStockForOrder(order);
        order.setStockReturned(true);
        Order savedOrder = orderRepository.save(order);

        auditLogService.logActivity(
                savedOrder, currentUser,
                "Admin kho xác nhận đã nhận hàng trả và nhập kho.",
                "stock", oldStatus.name(), "RETURNED"
        );

        return mapToAdminOrderDTO(savedOrder);
    }
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserOrderDTO> getMyOrders(User user, Pageable pageable, String status) {
        Page<Order> orderPage;

        if (status == null || status.equalsIgnoreCase("ALL")) {
            orderPage = orderRepository.findByUser(user, pageable);
        } else {
            try {
                OrderStatus statusEnum = OrderStatus.valueOf(status.toUpperCase());
                orderPage = orderRepository.findByUserAndOrderStatus(user, statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                orderPage = orderRepository.findByUser(user, pageable);
            }
        }

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
}