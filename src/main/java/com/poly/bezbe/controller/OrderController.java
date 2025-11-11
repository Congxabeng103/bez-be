package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.CancelRequestDTO;
import com.poly.bezbe.dto.request.DisputeRequestDTO;
import com.poly.bezbe.dto.request.OrderRequestDTO;
import com.poly.bezbe.dto.request.UpdateStatusRequestDTO;
import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.dto.response.OrderAuditLogResponseDTO; // <-- 1. IMPORT DTO LOG
import com.poly.bezbe.entity.User;
import com.poly.bezbe.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List; // <-- 2. IMPORT LIST

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponseDTO<?>> createOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OrderRequestDTO request,
            HttpServletRequest httpServletRequest // Cần để lấy IP cho VNPAY
    ) {
        // (user sẽ không null do Spring Security)

        // Hàm createOrder sẽ trả về OrderResponseDTO (nếu là COD)
        // hoặc VnpayResponseDTO (nếu là VNPAY)
        Object result = orderService.createOrder(user, request, httpServletRequest);

        return ResponseEntity.ok(ApiResponseDTO.success(result, "Xử lý đơn hàng thành công"));
    }
    // --- THÊM 2 API MỚI CHO ADMIN ---

    /**
     * API cho Admin xác nhận đơn hàng (COD) và trừ kho
     */
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> confirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user // Spring Security sẽ kiểm tra quyền (ADMIN/STAFF)
    ) {
        // GỢI Ý: Nên sửa hàm 'adminConfirmOrder' để gọi 'updateOrderStatus'
        // Bằng cách đó, hành động 'confirm' này cũng sẽ được GHI LOG.
        OrderResponseDTO orderResponse = orderService.adminConfirmOrder(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Xác nhận đơn hàng thành công, đã trừ kho."));
    }

    /**
     * API cho Admin hủy đơn hàng (và trả hàng về kho nếu cần)
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user // Spring Security sẽ kiểm tra quyền
    ) {
        // GỢI Ý: Nên sửa hàm 'adminCancelOrder' để gọi 'updateOrderStatus'
        // Bằng cách đó, hành động 'cancel' này cũng sẽ được GHI LOG.
        OrderResponseDTO orderResponse = orderService.adminCancelOrder(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Hủy đơn hàng thành công, đã trả hàng về kho (nếu cần)."));
    }

    // --- THÊM 3 API MỚI CHO ADMIN PAGE ---

    /**
     * Lấy danh sách đơn hàng cho Admin (Lọc, Phân trang)
     */
    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<AdminOrderDTO>>> getAdminOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ALL") String status
    ) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PageResponseDTO<AdminOrderDTO> orderPage = orderService.getAdminOrders(pageable, status, search);
        return ResponseEntity.ok(ApiResponseDTO.success(orderPage, "Lấy danh sách đơn hàng thành công"));
    }

    /**
     * Lấy chi tiết 1 đơn hàng cho Admin (Modal)
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponseDTO<AdminOrderDetailDTO>> getAdminOrderDetail(
            @PathVariable Long orderId
    ) {
        AdminOrderDetailDTO orderDetail = orderService.getAdminOrderDetail(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(orderDetail, "Lấy chi tiết đơn hàng thành công"));
    }

    // --- SỬA 3: API CẬP NHẬT TRẠNG THÁI ---
    /**
     * Cập nhật trạng thái đơn hàng (API đa năng)
     * Đây là API "lõi" sẽ ghi log
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponseDTO<AdminOrderDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateStatusRequestDTO request,
            @AuthenticationPrincipal User currentUser // <-- LẤY USER ĐANG ĐĂNG NHẬP
    ) {
        // Truyền 'currentUser' xuống Service để GHI LOG
        AdminOrderDTO updatedOrder = orderService.updateOrderStatus(orderId, request, currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedOrder, "Cập nhật trạng thái thành công"));
    }
    // --- KẾT THÚC SỬA 3 ---

    // --- SỬA 4: THÊM API MỚI ĐỂ ĐỌC LOG ---
    /**
     * Lấy lịch sử thao tác của 1 đơn hàng (cho Modal)
     */
    @GetMapping("/{orderId}/history")
    public ResponseEntity<ApiResponseDTO<List<OrderAuditLogResponseDTO>>> getOrderHistory(
            @PathVariable Long orderId
    ) {
        // Gọi hàm service mới (đã code ở OrderServiceImpl)
        List<OrderAuditLogResponseDTO> historyDTOs = orderService.getOrderHistory(orderId);

        return ResponseEntity.ok(ApiResponseDTO.success(historyDTOs, "Tải lịch sử thao tác thành công"));
    }
    // --- KẾT THÚC SỬA 4 ---


    // --- CÁC API CỦA USER (Giữ nguyên) ---

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<UserOrderDTO>>> getMyOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponseDTO<UserOrderDTO> orderPage = orderService.getMyOrders(user, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(orderPage, "Lấy đơn hàng thành công"));
    }

    @GetMapping("/my-orders/{orderId}")
    public ResponseEntity<ApiResponseDTO<AdminOrderDetailDTO>> getMyOrderDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId
    ) {
        AdminOrderDetailDTO orderDetail = orderService.getMyOrderDetail(user, orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(orderDetail, "Lấy chi tiết đơn hàng thành công"));
    }


    @PutMapping("/my-orders/{orderId}/cancel")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> userCancelOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId,
            @Valid @RequestBody CancelRequestDTO dto
    ) {
        OrderResponseDTO orderResponse = orderService.userCancelOrder(orderId, user,dto.getReason());
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Hủy đơn hàng thành công."));
    }

    @PutMapping("/my-orders/{orderId}/complete")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> userConfirmDelivery(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId
    ) {
        OrderResponseDTO orderResponse = orderService.userConfirmDelivery(orderId, user);
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Xác nhận đã nhận hàng thành công."));
    }

    @PutMapping("/my-orders/{orderId}/report-issue")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> userReportIssue(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId,
            @Valid @RequestBody DisputeRequestDTO dto
    ) {
        OrderResponseDTO orderResponse = orderService.reportDeliveryIssue(orderId, user,dto.getReason());
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Gửi khiếu nại thành công."));
    }
    @GetMapping("/my-orders/{orderId}/history")
    public ResponseEntity<ApiResponseDTO<List<OrderAuditLogResponseDTO>>> getMyOrderHistory(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId
    ) {
        // Gọi hàm service mới (đã code ở OrderServiceImpl)
        List<OrderAuditLogResponseDTO> historyDTOs = orderService.getMyOrderHistory(orderId, user);

        return ResponseEntity.ok(ApiResponseDTO.success(historyDTOs, "Tải lịch sử thao tác thành công"));
    }

    @PutMapping("/{orderId}/confirm-refund")
    public ResponseEntity<ApiResponseDTO<RefundResponseDTO>> confirmCodRefund(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User currentUser // Lấy admin/staff đang đăng nhập
    ) {
        // Chúng ta sẽ tạo hàm 'confirmCodRefund' trong OrderService
        RefundResponseDTO refundResponse = orderService.confirmCodRefund(orderId, currentUser);

        return ResponseEntity.ok(ApiResponseDTO.success(
                refundResponse,
                refundResponse.getMessage() // Lấy thông báo "Xác nhận hoàn tiền COD thành công!"
        ));
    }
    @PutMapping("/{orderId}/confirm-stock-return")
    public ResponseEntity<ApiResponseDTO<AdminOrderDTO>> confirmStockReturn(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User currentUser
    ) {
        AdminOrderDTO updatedOrder = orderService.confirmStockReturn(orderId, currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedOrder, "Xác nhận nhập kho thành công"));
    }
}