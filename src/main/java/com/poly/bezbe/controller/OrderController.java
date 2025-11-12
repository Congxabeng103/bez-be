package com.poly.bezbe.controller;

import com.poly.bezbe.dto.request.CancelRequestDTO;
import com.poly.bezbe.dto.request.DisputeRequestDTO;
import com.poly.bezbe.dto.request.OrderRequestDTO;
import com.poly.bezbe.dto.request.UpdateStatusRequestDTO;
import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.dto.response.OrderAuditLogResponseDTO;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- IMPORT
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ----- API CỦA USER -----
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()") // Chỉ user đăng nhập
    public ResponseEntity<ApiResponseDTO<?>> createOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OrderRequestDTO request,
            HttpServletRequest httpServletRequest
    ) {
        Object result = orderService.createOrder(user, request, httpServletRequest);
        return ResponseEntity.ok(ApiResponseDTO.success(result, "Xử lý đơn hàng thành công"));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<AdminOrderDetailDTO>> getMyOrderDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId
    ) {
        AdminOrderDetailDTO orderDetail = orderService.getMyOrderDetail(user, orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(orderDetail, "Lấy chi tiết đơn hàng thành công"));
    }


    @PutMapping("/my-orders/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> userCancelOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId,
            @Valid @RequestBody CancelRequestDTO dto
    ) {
        OrderResponseDTO orderResponse = orderService.userCancelOrder(orderId, user,dto.getReason());
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Hủy đơn hàng thành công."));
    }

    @PutMapping("/my-orders/{orderId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> userConfirmDelivery(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId
    ) {
        OrderResponseDTO orderResponse = orderService.userConfirmDelivery(orderId, user);
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Xác nhận đã nhận hàng thành công."));
    }

    @PutMapping("/my-orders/{orderId}/report-issue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> userReportIssue(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId,
            @Valid @RequestBody DisputeRequestDTO dto
    ) {
        OrderResponseDTO orderResponse = orderService.reportDeliveryIssue(orderId, user,dto.getReason());
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Gửi khiếu nại thành công."));
    }

    @GetMapping("/my-orders/{orderId}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<List<OrderAuditLogResponseDTO>>> getMyOrderHistory(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId
    ) {
        List<OrderAuditLogResponseDTO> historyDTOs = orderService.getMyOrderHistory(orderId, user);
        return ResponseEntity.ok(ApiResponseDTO.success(historyDTOs, "Tải lịch sử thao tác thành công"));
    }


    // ----- API CỦA ADMIN/MANAGER/STAFF -----

    // Lấy danh sách cho Admin (Cả 3 vai trò)
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
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

    // Lấy chi tiết cho Admin (Cả 3 vai trò)
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponseDTO<AdminOrderDetailDTO>> getAdminOrderDetail(
            @PathVariable Long orderId
    ) {
        AdminOrderDetailDTO orderDetail = orderService.getAdminOrderDetail(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(orderDetail, "Lấy chi tiết đơn hàng thành công"));
    }

    // Lấy lịch sử cho Admin (Cả 3 vai trò)
    @GetMapping("/{orderId}/history")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponseDTO<List<OrderAuditLogResponseDTO>>> getOrderHistory(
            @PathVariable Long orderId
    ) {
        List<OrderAuditLogResponseDTO> historyDTOs = orderService.getOrderHistory(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(historyDTOs, "Tải lịch sử thao tác thành công"));
    }

    // API Cập nhật trạng thái "Lõi" (Cả 3 vai trò, Service sẽ kiểm tra logic)
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponseDTO<AdminOrderDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateStatusRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        // (Logic kiểm tra quyền Hủy đơn SHIPPING/DELIVERED sẽ nằm trong Service)
        AdminOrderDTO updatedOrder = orderService.updateOrderStatus(orderId, request, currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedOrder, "Cập nhật trạng thái thành công"));
    }

    // API Hoàn tiền COD (NGHIỆP VỤ TIỀN)
    @PutMapping("/{orderId}/confirm-refund")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')") // <-- KHÓA (STAFF không được)
    public ResponseEntity<ApiResponseDTO<RefundResponseDTO>> confirmCodRefund(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User currentUser
    ) {
        RefundResponseDTO refundResponse = orderService.confirmCodRefund(orderId, currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success(
                refundResponse,
                refundResponse.getMessage()
        ));
    }

    // API Nhập kho (NGHIỆP VỤ HÀNG)
    @PutMapping("/{orderId}/confirm-stock-return")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')") // <-- Cả 3 được
    public ResponseEntity<ApiResponseDTO<AdminOrderDTO>> confirmStockReturn(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User currentUser
    ) {
        AdminOrderDTO updatedOrder = orderService.confirmStockReturn(orderId, currentUser);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedOrder, "Xác nhận nhập kho thành công"));
    }


    // --- CÁC API CŨ (Bạn nên xóa nếu đã dùng /status) ---
    // (Nếu bạn vẫn dùng, hãy thêm phân quyền)
    @PutMapping("/{orderId}/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> confirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user
    ) {
        OrderResponseDTO orderResponse = orderService.adminConfirmOrder(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Xác nhận đơn hàng thành công, đã trừ kho."));
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user
    ) {
        // (Lưu ý: Nếu hàm này có thể hủy đơn SHIPPING,
        // bạn phải thêm logic check quyền trong service)
        OrderResponseDTO orderResponse = orderService.adminCancelOrder(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(orderResponse, "Hủy đơn hàng thành công, đã trả hàng về kho (nếu cần)."));
    }
}