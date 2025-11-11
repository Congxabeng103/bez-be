package com.poly.bezbe.service.impl;

// (Thêm 2 import này ở đầu file)
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map; // <-- THÊM IMPORT NÀY

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
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.*;
import com.poly.bezbe.service.*;
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
    private final EmailService emailService; // <-- TIÊM (INJECT) EMAILSERVICE
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

        // --- THÊM DÒNG NÀY ---
        // Ghi log (Timeline) cho hành động "Tạo đơn hàng"
        // (Hàm formatUserTimelineMessage ở FE sẽ bắt được "Đơn hàng đã được tạo")
        auditLogService.logActivity(
                savedOrder,
                "Khách hàng", // 1. Truyền String "Khách hàng"
                "Đơn hàng đã được tạo.", // 2. Mô tả
                "orderStatus", // 3. Field
                null, // 4. Old
                OrderStatus.PENDING.name() // 5. New
        );
        emailService.sendOrderConfirmationEmail(savedOrder);

        // --- KẾT THÚC THÊM ---
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
                // 1. CẬP NHẬT ORDER (Bạn đã làm)
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setOrderStatus(OrderStatus.CONFIRMED);

                // --- THÊM 2 DÒNG NÀY ---
                // Ghi log (Timeline) cho hành động "Thanh toán thành công"
                auditLogService.logActivity(
                        order,
                        "Hệ thống", // 1. Truyền String "Hệ thống"
                        "Thanh toán VNPAY thành công (IPN).", // 2. Mô tả
                        "paymentStatus", // 3. Field
                        PaymentStatus.PENDING.name(), // 4. Old
                        PaymentStatus.PAID.name() // 5. New
                );



                // Log 2: Xác nhận
                auditLogService.logActivity(
                        order,
                        "Hệ thống", // 1. Truyền String "Hệ thống"
                        "Đơn hàng tự động xác nhận sau khi thanh toán VNPAY.", // 2. Mô tả
                        "orderStatus", // 3. Field
                        OrderStatus.PENDING.name(), // 4. Old
                        OrderStatus.CONFIRMED.name() // 5. New
                );
                emailService.sendPaymentSuccessEmail(order);
                // --- KẾT THÚC THÊM ---
                try {
                    subtractStockForOrder(order);
                } catch (BusinessRuleException e) {
                    order.setOrderStatus(OrderStatus.PENDING);
                    System.err.println("--- LOG TEST IPN WARNING: Trừ kho thất bại (Oversale) ---");
                }

                // 2. --- THÊM PHẦN NÀY ---
                // CẬP NHẬT PAYMENT (Rất quan trọng cho hoàn tiền)
                Payment payment = paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
                        .orElse(null); // Tìm payment PENDING

                if (payment != null) {
                    payment.setStatus(PaymentStatus.PAID);

                    // LƯU LẠI 2 THÔNG TIN SỐNG CÒN ĐỂ HOÀN TIỀN
                    payment.setTransactionId(vnp_TransactionNo); // vnp_TransactionNo
                    payment.setVnpTxnRef(vnp_TxnRef); // LƯU LẠI vnp_TxnRef
                    // Chuyển đổi vnp_PayDate (yyyyMMddHHmmss) sang LocalDateTime
                    try {
                        DateTimeFormatter vnpFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                        LocalDateTime paidTime = LocalDateTime.parse(vnp_PayDate, vnpFormatter);
                        payment.setPaidAt(paidTime); // vnp_PayDate
                    } catch (Exception e) {
                        payment.setPaidAt(LocalDateTime.now()); // Fallback
                    }

                    paymentRepository.save(payment);
                }
                // --- KẾT THÚC PHẦN THÊM ---

            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);

                // --- THÊM PHẦN NÀY ---
                // Nếu VNPAY báo lỗi, cũng cập nhật Payment FAILED
                Payment payment = paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
                        .orElse(null);
                if (payment != null) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setTransactionId(vnp_TransactionNo); // Lưu lại mã giao dịch (lỗi)
                    paymentRepository.save(payment);
                }
                // --- KẾT THÚC PHẦN THÊM ---
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        OrderStatus newStatus = request.getNewStatus(); // <-- LỖI 1: Sửa getNewStatus() thành getStatus()
        OrderStatus oldStatus = order.getOrderStatus();
        String adminNote = request.getNote();
        String logDescription = null; // Mô tả đầy đủ cho log

        if (oldStatus == OrderStatus.COMPLETED || oldStatus == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể cập nhật trạng thái cho đơn hàng đã hoàn tất hoặc đã hủy.");
        }
        if (oldStatus == newStatus) {
            throw new BusinessRuleException("Đơn hàng đã ở trạng thái này.");
        }

        switch (newStatus) {
            case PENDING:
                if (oldStatus == OrderStatus.CONFIRMED) {
                    // (Logic gốc của bạn)
                    returnStockForOrder(order);
                    order.setOrderStatus(OrderStatus.PENDING);
                    logDescription = "Hoàn đơn về 'Chờ xác nhận'. Đã hoàn kho.";
                } else {
                    // Thêm 1 Ghi chú lỗi rõ ràng hơn
                    throw new BusinessRuleException("Chỉ có thể hoàn tác từ 'Đã xác nhận' về 'Chờ xác nhận'.");
                }
                break;
            case CONFIRMED:
                if (oldStatus == OrderStatus.PENDING) {
                    if (order.getPaymentMethod() == PaymentMethod.COD || order.getPaymentStatus() == PaymentStatus.PAID) {
                        subtractStockForOrder(order);
                        logDescription = "Admin xác nhận đơn hàng, đã trừ kho.";
                    } else {
                        logDescription = "Admin xác nhận đơn hàng (chưa trừ kho, chờ thanh toán).";
                    }
                    order.setOrderStatus(OrderStatus.CONFIRMED);
                }
                break;
            case SHIPPING:
                if (oldStatus == OrderStatus.CONFIRMED) {
                    order.setOrderStatus(OrderStatus.SHIPPING);
                    logDescription = "Bắt đầu giao hàng.";
                    // LỖI 2: Thêm logic note vào log
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
                    throw new BusinessRuleException("Admin hủy đơn bắt buộc phải nhập lý do.");
                }
                if (oldStatus == OrderStatus.CONFIRMED) {
                    // Hàng CHƯA rời kho. Tự động hoàn kho.
                    returnStockForOrder(order);
                    order.setStockReturned(true); // <-- SỬA 1: Đã hoàn
                    logDescription = "Đơn hàng bị hủy... ĐÃ TỰ ĐỘNG HOÀN KHO. Lý do: " + adminNote;

                } else if (oldStatus == OrderStatus.PENDING) {
                    // Đơn PENDING (chưa trừ kho). Không cần hoàn kho.
                    order.setStockReturned(true); // <-- SỬA 2: Đánh dấu là "không cần"
                    logDescription = "Đơn hàng bị hủy... (Không cần hoàn kho). Lý do: " + adminNote;

                } else {
                    // Bao gồm SHIPPING, DELIVERED, DISPUTE
                    // Hàng ĐÃ rời kho.
                    order.setStockReturned(false); // <-- SỬA 3: ĐÁNH DẤU LÀ "CHƯA HOÀN" (CHỜ BẤM NÚT)
                    logDescription = "Đơn hàng bị hủy/trả hàng... (CHỜ NHẬP KHO). Lý do: " + adminNote;
                }

                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setCancellationReason(adminNote); // LƯU LÝ DO HỦY

                logDescription = "Đơn hàng bị hủy bởi Admin. Lý do: " + adminNote; // LỖI 3: Log description sạch

                if (order.getPaymentStatus() == PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
                    logDescription += " Chuyển sang chờ hoàn tiền.";
                }

                // LỖI 4: Truyền 'logDescription' (chứa lý do) vào mail
                emailService.sendOrderCancellationEmail(order, logDescription);
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
                order.setDisputeReason(adminNote); // Admin cũng có thể set lý do khiếu nại
                logDescription = "Admin đánh dấu đơn hàng là 'Khiếu nại'. Lý do: " + (adminNote != null ? adminNote : "Không có");
                break;
            default:
                throw new BusinessRuleException("Trạng thái cập nhật không hợp lệ: " + newStatus);
        }

        Order savedOrder = orderRepository.save(order);

        // Ghi log (Chỉ 1 lần ở cuối, đã dọn dẹp)
        if (logDescription != null) {
            auditLogService.logActivity(
                    savedOrder,
                    currentUser,
                    logDescription, // Dùng mô tả đầy đủ
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

    /**
     * Mapper cho Phản hồi API của USER
     * (DTO này CẦN 3 trường: note, cancellationReason, disputeReason)
     */
    private OrderResponseDTO mapOrderToDTO(Order order) {
        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .customerName(order.getCustomerName())
                .phone(order.getPhone())
                .address(order.getAddress())
                // --- CÁC TRƯỜNG ĐÃ THÊM ---
                .note(order.getNote()) // Ghi chú của khách
                .cancellationReason(order.getCancellationReason()) // Lý do hủy
                .disputeReason(order.getDisputeReason()) // Lý do khiếu nại
                .build();
    }

    /**
     * Mapper cho DANH SÁCH (Admin)
     * (DTO này KHÔNG cần lý do, giữ đơn giản để tải list cho nhanh)
     */
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
                .stockReturned(order.isStockReturned()) // <-- THÊM DÒNG NÀY
                .build();
    }

    /**
     * Mapper cho CHI TIẾT (Admin & User Detail)
     * (DTO này CẦN TẤT CẢ MỌI THỨ, bao gồm 3 trường lý do/note)
     * (Đã sửa lỗi NullPointerException cho variantInfo)
     */
    private AdminOrderDetailDTO mapToAdminOrderDetailDTO(Order order) {
        List<AdminOrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .map(item -> {
                    Variant variant = item.getVariant();

                    // Sửa lỗi NullPointerException nếu getAttributeValues() là null
                    String variantInfo = java.util.Optional.ofNullable(variant.getAttributeValues())
                            .orElse(java.util.Collections.emptySet()).stream()
                            .map(v -> v.getAttributeValue().getAttribute().getName() + ": " + v.getAttributeValue().getValue())
                            .collect(java.util.stream.Collectors.joining(", "));

                    return AdminOrderItemDTO.builder()
                            .variantId(variant.getId())
                            .productName(variant.getProduct().getName())
                            .variantInfo(variantInfo)
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .imageUrl(variant.getImageUrl() != null ? variant.getImageUrl() : variant.getProduct().getImageUrl())
                            .build();
                }).collect(java.util.stream.Collectors.toList());

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
                // --- CÁC TRƯỜNG ĐÃ THÊM ---
                .note(order.getNote()) // Ghi chú của khách
                .cancellationReason(order.getCancellationReason()) // Lý do hủy
                .disputeReason(order.getDisputeReason()) // Lý do khiếu nại
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
    public OrderResponseDTO reportDeliveryIssue(Long orderId, User user,String reason) {
        // ... (Code của bạn giữ nguyên) ...
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Chỉ có thể khiếu nại đơn hàng ở trạng thái 'Đã giao'.");
        }

        order.setOrderStatus(OrderStatus.DISPUTE);
        order.setDisputeReason(reason); // 1. LƯU LÝ DO KHIẾU NẠI
        Order savedOrder = orderRepository.save(order);
        auditLogService.logActivity(
                savedOrder,
                "Khách hàng", // 1. Truyền String "Khách hàng"
                "Khách hàng gửi khiếu nại Lý do: " + reason, // 2. Mô tả
                "orderStatus", // 3. Field
                OrderStatus.DELIVERED.name(), // 4. Old
                OrderStatus.DISPUTE.name() // 5. New
        );
        emailService.sendDisputeReceivedEmail(savedOrder, reason);
        return mapOrderToDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO userCancelOrder(Long orderId, User user,String reason) {
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
        order.setCancellationReason(reason); // 1. LƯU LÝ DO HỦY
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
        }

        Order savedOrder = orderRepository.save(order);
        auditLogService.logActivity(
                savedOrder,
                "Khách hàng", // 1. Truyền String "Khách hàng"
                "Khách hàng đã hủy đơn hàng. Lý do: " + reason,
                "orderStatus", // 3. Field
                currentStatus.name(), // 4. Old (Dùng biến đã khai báo)
                OrderStatus.CANCELLED.name() // 5. New
        );
        emailService.sendOrderCancellationEmail(savedOrder,reason);

        return mapOrderToDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO userConfirmDelivery(Long orderId, User user) {

        // ... (Code của bạn giữ nguyên) ...
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));
        OrderStatus oldStatus = order.getOrderStatus(); // <-- Lấy trạng thái cũ
        if (order.getOrderStatus() != OrderStatus.DELIVERED && order.getOrderStatus() != OrderStatus.DISPUTE) {
            throw new BusinessRuleException("Đơn hàng chưa được giao.");
        }

        order.setOrderStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepository.save(order);
        auditLogService.logActivity(
                savedOrder,
                "Khách hàng", // 1. Truyền String "Khách hàng"
                "Khách hàng xác nhận đã nhận hàng.", // 2. Mô tả
                "orderStatus", // 3. Field
                oldStatus.name(), // 4. Old
                OrderStatus.COMPLETED.name() // 5. New
        );
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
    @Override
    @Transactional
    public RefundResponseDTO requestVnpayRefund(
            Long orderId,
            HttpServletRequest request,
            User currentUser
    ) {
        // 1. Tìm Order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        // 2. Tìm bản ghi Payment (đã PAID hoặc PENDING_REFUND)
        // FE của bạn chỉ gọi khi status là PENDING_REFUND, nhưng ta nên check cả PAID cho chắc
        Payment payment = paymentRepository
                .findByOrderIdAndStatusIn(orderId, List.of(PaymentStatus.PAID, PaymentStatus.PENDING_REFUND))
                .stream()
                // Ưu tiên cái PAID (nếu có) hoặc PENDING_REFUND
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy giao dịch VNPAY hợp lệ cho đơn hàng này."));

        // 3. Kiểm tra các thông tin cần thiết (Đã lưu ở Bước 1)
        if (payment.getVnpTxnRef() == null ||
                payment.getTransactionId() == null ||
                payment.getPaidAt() == null) {

            // Log lỗi này để debug
            System.err.println("--- LỖI HOÀN TIỀN: Đơn hàng " + orderId + " thiếu thông tin ---");
            System.err.println("vnpTxnRef: " + payment.getVnpTxnRef());
            System.err.println("transactionId: " + payment.getTransactionId());
            System.err.println("paidAt: " + payment.getPaidAt());

            throw new BusinessRuleException("Thiếu thông tin giao dịch VNPAY (TxnRef, TransactionNo, PaidAt). Không thể hoàn tiền. (Có thể đây là đơn hàng cũ?)");
        }

        // 4. Chuẩn bị dữ liệu cho VNPAY
        String vnp_TxnRef = payment.getVnpTxnRef();
        String vnp_TransactionNo = payment.getTransactionId();
        // Format ngày thanh toán sang yyyyMMddHHmmss
        String vnp_TransactionDate = payment.getPaidAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        BigDecimal vnp_Amount = payment.getAmount(); // Hoàn toàn bộ số tiền
        String vnp_CreateBy = currentUser.getEmail(); // Lấy email admin/staff
        String vnp_IpAddr = vnpayService.getIpAddress(request); // Lấy IP từ request

        // 5. Gọi VnpayService (cỗ máy ở Bước 3)
        String vnpayResponseJson = vnpayService.requestRefund(
                vnp_TxnRef,
                vnp_TransactionNo,
                vnp_TransactionDate,
                vnp_Amount,
                vnp_CreateBy,
                vnp_IpAddr
        );

        // 6. Phân tích JSON response từ VNPAY
        Map<String, String> vnpayResponseMap;
        try {
            // Dùng ObjectMapper để chuyển JSON string thành Map
            vnpayResponseMap = new ObjectMapper().readValue(vnpayResponseJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new BusinessRuleException("Lỗi khi đọc phản hồi từ VNPAY: " + e.getMessage());
        }

        String vnp_ResponseCode = vnpayResponseMap.get("vnp_ResponseCode");
        String vnp_Message = vnpayResponseMap.getOrDefault("vnp_Message", "Không có thông báo từ VNPAY");

        // 7. Xử lý kết quả
        if ("00".equals(vnp_ResponseCode)) {
            // THÀNH CÔNG
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setStatus(PaymentStatus.REFUNDED);

            orderRepository.save(order);
            paymentRepository.save(payment);
            // Ghi log (Rất quan trọng cho FE)
            auditLogService.logActivity(
                    order,
                    currentUser, // 1. Truyền User
                    "Thực hiện hoàn tiền VNPAY thành công. Số tiền: " + vnp_Amount +
                            ". Mã GD VNPAY: " + vnp_TransactionNo, // 2. Mô tả
                    "paymentStatus", // 3. Field
                    PaymentStatus.PENDING_REFUND.name(), // 4. Old
                    PaymentStatus.REFUNDED.name() // 5. New
            );
            emailService.sendOrderRefundNotificationEmail(order, vnp_Amount);

            return RefundResponseDTO.builder()
                    .orderId(orderId)
                    .newPaymentStatus(PaymentStatus.REFUNDED)
                    .message("Hoàn tiền VNPAY thành công!")
                    .vnpayResponseCode(vnp_ResponseCode)
                    .build();
        } else {
            // THẤT BẠI
            // Ghi log lỗi
            auditLogService.logActivity(
                    order,
                    currentUser, // 1. Truyền User
                    "Yêu cầu hoàn tiền VNPAY THẤT BẠI. Mã lỗi: " + vnp_ResponseCode +
                            ". Lý do: " + vnp_Message, // 2. Mô tả
                    "paymentStatus", // 3. Field
                    PaymentStatus.PENDING_REFUND.name(), // 4. Old
                    PaymentStatus.PENDING_REFUND.name() // 5. New
            );
            // Ném lỗi về cho FE (FE của bạn sẽ bắt lỗi này và toast.error)
            throw new BusinessRuleException("Hoàn tiền VNPAY thất bại: " + vnp_Message + " (Mã lỗi: " + vnp_ResponseCode + ")");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderAuditLogResponseDTO> getMyOrderHistory(Long orderId, User user) {
        // 1. KIỂM TRA BẢO MẬT:
        // Tìm đơn hàng VÀ xác thực nó thuộc về user này
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập."));

        // 2. TÁI SỬ DỤNG LOGIC:
        // Lấy lịch sử từ repository (hoặc gọi hàm getOrderHistory(order.getId()) đều được)
        List<OrderAuditLog> historyEntities =
                auditLogRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());

        return historyEntities.stream()
                .map(this::mapToAuditLogDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RefundResponseDTO confirmCodRefund(Long orderId, User currentUser) {
        // 1. Tìm Order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        // 2. KIỂM TRA BẢO MẬT VÀ LOGIC
        // Phải là đơn COD
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            throw new BusinessRuleException("Chức năng này chỉ dành cho đơn hàng COD.");
        }

        // Phải đang ở trạng thái PENDING_REFUND
        if (order.getPaymentStatus() != PaymentStatus.PENDING_REFUND) {
            throw new BusinessRuleException("Đơn hàng không ở trạng thái 'Chờ hoàn tiền'.");
        }

        // 3. THỰC THI HÀNH ĐỘNG
        // Đây chính là logic bạn muốn: chỉ cập nhật trạng thái
        PaymentStatus oldStatus = order.getPaymentStatus();
        order.setPaymentStatus(PaymentStatus.REFUNDED);

        // (Nếu bạn có lưu Payment cho COD, bạn cũng nên cập nhật nó)
        Payment payment = paymentRepository
                .findByOrderIdAndStatus(orderId, PaymentStatus.PENDING_REFUND)
                .orElse(null); // Tìm bản ghi PENDING_REFUND

        if (payment == null) {
            // Nếu không có bản ghi PENDING_REFUND (ví dụ đơn COD cũ không tạo payment),
            // ta tìm bản ghi PENDING (lúc tạo đơn)
            payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PENDING)
                    .orElse(null);
        }

        if (payment != null) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        orderRepository.save(order);

        // 4. GHI LOG (Rất quan trọng)
        auditLogService.logActivity(
                order,
                currentUser, // Admin/Staff đang thực hiện
                "Admin xác nhận hoàn tiền COD (chuyển trạng thái).",
                "paymentStatus", // Field thay đổi
                oldStatus.name(), // Old
                PaymentStatus.REFUNDED.name() // New
        );
        // (Bạn có thể thêm gửi mail thông báo cho khách ở đây nếu muốn)
        emailService.sendOrderRefundNotificationEmail(order, order.getTotalAmount());


        // 5. Trả về DTO giống hệt VNPAY
        return RefundResponseDTO.builder()
                .orderId(orderId)
                .newPaymentStatus(PaymentStatus.REFUNDED)
                .message("Xác nhận hoàn tiền COD thành công!")
                .vnpayResponseCode(null) // Không có mã VNPAY
                .build();
    }
    @Override
    @Transactional
    public AdminOrderDTO confirmStockReturn(Long orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        // 1. Kiểm tra nghiệp vụ
        if (order.getOrderStatus() != OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Đơn hàng phải ở trạng thái Đã hủy mới được nhập kho.");
        }

        if (order.isStockReturned()) {
            throw new BusinessRuleException("Đơn hàng này đã được nhập kho trước đó.");
        }

        // (Kiểm tra xem đơn này có cần nhập kho không,
        // ví dụ: đơn PENDING bị hủy thì không cần)
        // -> Logic này đã được xử lý bằng cờ 'stockReturned'

        // 2. Thực thi
        returnStockForOrder(order);
        order.setStockReturned(true); // <-- ĐÁNH DẤU
        Order savedOrder = orderRepository.save(order);

        // 3. Ghi log
        auditLogService.logActivity(
                savedOrder, currentUser,
                "Admin kho xác nhận đã nhận hàng trả và nhập kho.",
                "stock", "N/A", "RETURNED"
        );

        return mapToAdminOrderDTO(savedOrder);
    }
}