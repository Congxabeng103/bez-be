package com.poly.bezbe.service.impl;

// (Thêm 2 import này ở đầu file)
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.List; // <-- Import
import java.util.stream.Collectors; // <-- Import

import com.poly.bezbe.dto.request.OrderRequestDTO;
import com.poly.bezbe.dto.request.UpdateStatusRequestDTO;
import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.dto.response.OrderAuditLogResponseDTO; // <-- Import DTO Log
import com.poly.bezbe.entity.*;
import com.poly.bezbe.entity.OrderAuditLog; // <-- Import Entity Log
import com.poly.bezbe.enums.OrderStatus;
import com.poly.bezbe.enums.PaymentMethod;
import com.poly.bezbe.enums.PaymentStatus;
import com.poly.bezbe.exception.BusinessRuleException;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.*;
import com.poly.bezbe.service.CouponService;
import com.poly.bezbe.service.OrderAuditLogService; // <-- Import "Người Ghi"
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

    // (Các repo/service bạn đã tiêm - Đã CHUẨN)
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
        String orderIdStr = request.getParameter("vnp_TxnRef"); // Đây là "26_d325"
        String transId = request.getParameter("vnp_TransactionNo");

        // --- SỬA LỖI Ở ĐÂY (PHẢI NẰM TRƯỚC KHI GỌI PARSELONG) ---
        String actualOrderId;

        // Thêm check null cho an toàn
        if (orderIdStr != null && orderIdStr.contains("_")) {
            actualOrderId = orderIdStr.split("_")[0]; // Lấy ra "26"
        } else {
            actualOrderId = orderIdStr; // Dự phòng nếu không có "_"
        }

        // --- DÒNG 169 GÂY LỖI CỦA BẠN ĐÂY ---
        // Bạn PHẢI dùng `actualOrderId` đã được làm sạch ở đây!!!
        Order order = orderRepository.findById(Long.parseLong(actualOrderId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + actualOrderId));

        if (order.getPaymentStatus() == PaymentStatus.PENDING || order.getPaymentStatus() == PaymentStatus.FAILED) {
            // --- SỬA LỖI Ở ĐÂY ---
            // Tìm CHÍNH XÁC bản ghi payment đang PENDING, chứ không phải tìm chung chung
            Payment payment = paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
                    .orElse(null); // (Hàm này chúng ta đã thêm ở bước trước)

            if (status.equals("00")) {
                // VNPAY BÁO THÀNH CÔNG
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setOrderStatus(OrderStatus.CONFIRMED);
                try {
                    subtractStockForOrder(order); // Trừ kho
                } catch (BusinessRuleException e) {
                    order.setOrderStatus(OrderStatus.PENDING); // Lỗi trừ kho (oversale)
                    System.err.println("LỖI OVERSALE (VNPAY): " + e.getMessage() + " VỚI ĐƠN HÀNG " + order.getId());
                }

                if (payment != null) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setTransactionId(transId);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                } else {
                    // Trường hợp hi hữu không tìm thấy PENDING, tạo mới
                    createPaymentRecord(order, PaymentStatus.PAID, transId);
                }
            } else {
                // VNPAY BÁO THẤT BẠI (Khách hủy, hết giờ, v.v.)
                order.setPaymentStatus(PaymentStatus.FAILED); // Set trạng thái đơn hàng là FAILED

                // --- SỬA LỖI Ở ĐÂY ---
                // Cập nhật bản ghi payment PENDING thành FAILED
                if (payment != null) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }
                // (Không cần tạo mới nếu không có)
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

    // --- BẮT ĐẦU SỬA HÀM updateOrderStatus ---
    @Override
    @Transactional
    public AdminOrderDTO updateOrderStatus(Long orderId, UpdateStatusRequestDTO request, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        OrderStatus newStatus = request.getNewStatus();

        // --- SỬA 1: Khai báo 2 biến bị lỗi 'Cannot resolve' ---
        OrderStatus oldStatus = order.getOrderStatus(); // Đổi tên 'currentStatus' thành 'oldStatus'
        String logDescription = null; // Khai báo là null
        // --- HẾT SỬA 1 ---

        if (oldStatus == OrderStatus.COMPLETED || oldStatus == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể cập nhật trạng thái cho đơn hàng đã hoàn tất hoặc đã hủy.");
        }

        // --- SỬA 2: Thêm 'logDescription = ...' vào mỗi case ---
        switch (newStatus) {
            case CONFIRMED:
                if (oldStatus == OrderStatus.PENDING) {
                    if (order.getPaymentMethod() == PaymentMethod.COD || order.getPaymentStatus() == PaymentStatus.PAID) {
                        subtractStockForOrder(order);
                    }
                    order.setOrderStatus(OrderStatus.CONFIRMED);
                    logDescription = "Xác nhận đơn hàng, đã trừ kho."; // <-- Thêm
                }
                break;

            case SHIPPING:
                if (oldStatus == OrderStatus.CONFIRMED) {
                    order.setOrderStatus(OrderStatus.SHIPPING);
                    logDescription = "Bắt đầu giao hàng."; // <-- Thêm
                } else {
                    throw new BusinessRuleException("Phải xác nhận đơn hàng trước khi giao.");
                }
                break;

            case DELIVERED:
                if (oldStatus == OrderStatus.SHIPPING) {
                    order.setOrderStatus(OrderStatus.DELIVERED);
                    logDescription = "Đã giao hàng thành công."; // <-- Thêm
                    if (order.getPaymentMethod() == PaymentMethod.COD) {
                        order.setPaymentStatus(PaymentStatus.PAID);
                        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                        if(payment != null){
                            payment.setStatus(PaymentStatus.PAID);
                            payment.setPaidAt(LocalDateTime.now());
                            paymentRepository.save(payment);
                        }
                        logDescription += " Đã thu COD."; // <-- Thêm
                    }
                } else {
                    throw new BusinessRuleException("Đơn hàng phải được giao trước khi hoàn tất.");
                }
                break;

            case CANCELLED:
                // ... (Logic 'returnStockForOrder' của bạn) ...
                if (oldStatus == OrderStatus.CONFIRMED ||
                        oldStatus == OrderStatus.SHIPPING ||
                        oldStatus == OrderStatus.DELIVERED ||
                        oldStatus == OrderStatus.DISPUTE) {

                    if(order.getPaymentStatus() == PaymentStatus.PAID ||
                            (order.getPaymentMethod() == PaymentMethod.COD && oldStatus != OrderStatus.PENDING) )
                    {
                        returnStockForOrder(order); // Trả kho
                        logDescription = "Hủy đơn hàng, đã hoàn kho."; // <-- Thêm
                    }
                }
                if(logDescription == null) logDescription = "Hủy đơn hàng (trước khi trừ kho)."; // <-- Thêm

                order.setOrderStatus(OrderStatus.CANCELLED);

                if (order.getPaymentStatus() == PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
                    logDescription += " Chuyển sang chờ hoàn tiền."; // <-- Thêm
                }
                break;

            case PENDING:
                if (oldStatus == OrderStatus.CONFIRMED) {
                    returnStockForOrder(order);
                    order.setOrderStatus(OrderStatus.PENDING);
                    logDescription = "Hoàn đơn về 'Chờ xác nhận'. Đã hoàn kho."; // <-- Thêm
                }
                break;

            case COMPLETED:
                if (oldStatus == OrderStatus.DELIVERED) {
                    order.setOrderStatus(OrderStatus.COMPLETED);
                    logDescription = "Xác nhận hoàn tất đơn hàng."; // <-- Thêm
                }
                break;

            // (Thêm default hoặc DISPUTE nếu bạn có)
        }
        // --- HẾT SỬA 2 ---

        Order savedOrder = orderRepository.save(order);

        // --- SỬA 3: Thêm khối logic ghi log (giờ đã chạy được) ---
        if (logDescription != null) {

            String fullDescription = String.format(
                    "Đổi trạng thái từ %s sang %s. %s",
                    oldStatus.name(), newStatus.name(), logDescription
            );

            // Gọi "Người Ghi"
            auditLogService.logActivity(
                    savedOrder,
                    currentUser, // Đây là nhân viên admin
                    fullDescription,
                    "orderStatus",
                    oldStatus.name(),
                    newStatus.name()
            );
        }
        // --- KẾT THÚC SỬA 3 ---

        return mapToAdminOrderDTO(savedOrder);
    }
    // --- KẾT THÚC HÀM updateOrderStatus ---


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
                .paymentMethod(order.getPaymentMethod())
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
    // --- THÊM HÀM MỚI NÀY VÀO CUỐI FILE ---

    @Override
    @Transactional
    public VnpayResponseDTO retryVnpayPayment(User user, Long orderId, HttpServletRequest httpServletRequest) {

        // 1. Tìm đơn hàng (đảm bảo đúng là của user này)
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng hoặc bạn không có quyền."));

        // 2. Kiểm tra các điều kiện
        if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new BusinessRuleException("Chức năng này chỉ dành cho đơn hàng VNPAY.");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleException("Đơn hàng này đã được thanh toán.");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Chỉ có thể thanh toán lại đơn hàng ở trạng thái 'Chờ xác nhận'.");
        }

        // 3. (Tùy chọn) Cập nhật lại bản ghi Payment cũ (nếu có) sang FAILED
        // Điều này giúp log được sạch sẽ, biết rằng lần thanh toán cũ đã bị hủy
        Optional<Payment> oldPendingPayment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PENDING);
        if (oldPendingPayment.isPresent()) {
            Payment paymentToFail = oldPendingPayment.get();
            paymentToFail.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(paymentToFail);
        }
        // 4. Tạo link VNPAY mới (sử dụng lại hàm của VnpayService)
        // Lưu ý: Nó sẽ tạo một bản ghi Payment MỚI ở trạng thái PENDING
        // (Hoặc bạn có thể sửa logic 'createPaymentRecord' để cập nhật cái cũ)
        String paymentUrl = vnpayService.createPaymentUrl(httpServletRequest, order.getId(), order.getTotalAmount());

        // Cập nhật/tạo mới bản ghi thanh toán
        createPaymentRecord(order, PaymentStatus.PENDING, null);

        // 5. Trả về DTO chứa link
        return VnpayResponseDTO.builder()
                .status("OK")
                .message("Tạo link VNPAY mới thành công")
                .paymentUrl(paymentUrl)
                .build();
    } // <-- SỬA 4: DẤU NGOẶC KẾT THÚC CỦA `retryVnpayPayment` Ở ĐÂY

    // --- SỬA 5: Dời 2 hàm này ra khỏi retryVnpayPayment ---
    // Chúng là hàm của class, ngang hàng với các hàm public khác

    /**
     * "Công thức" mapper private
     * SỬA 6: Cập nhật tên DTO thành 'Response'
     */
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

    /**
     * Hàm triển khai "hợp đồng" mới
     * SỬA 6: Cập nhật tên DTO thành 'Response'
     */
    @Override
    @Transactional(readOnly = true) // Thao tác đọc
    public List<OrderAuditLogResponseDTO> getOrderHistory(Long orderId) {

        // 1. Lấy List<Entity> từ DB
        List<OrderAuditLog> historyEntities =
                auditLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        // 2. Map sang List<DTO> (sửa typo của bạn từ mapToAuditLogRDTO)
        return historyEntities.stream()
                .map(this::mapToAuditLogDTO) // <-- Sạch sẽ
                .collect(Collectors.toList());
    }
    // --- KẾT THÚC SỬA 5 & 6 ---
}