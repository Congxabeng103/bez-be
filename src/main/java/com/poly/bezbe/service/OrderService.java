package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.OrderRequestDTO;
import com.poly.bezbe.dto.request.UpdateStatusRequestDTO;
import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.List; // <-- 2. IMPORT LIST

public interface OrderService {

    /**
     * Tạo đơn hàng (COD hoặc chuẩn bị cho VNPAY)
     */
    Object createOrder(User user, OrderRequestDTO request, HttpServletRequest httpServletRequest);

    /**
     * Xử lý kết quả VNPAY trả về
     */
    OrderResponseDTO handleVnpayReturn(HttpServletRequest request);
    /**
     * Admin xác nhận đơn hàng (thường là COD) và trừ kho
     */
    OrderResponseDTO adminConfirmOrder(Long orderId); // (Gợi ý: Hàm này nên gọi updateOrderStatus)

    /**
     * Admin hủy đơn hàng. Tự động trả hàng về kho nếu cần.
     */
    OrderResponseDTO adminCancelOrder(Long orderId); // (Gợi ý: Hàm này nên gọi updateOrderStatus)

    // --- THÊM 3 HÀM MỚI CHO ADMIN PAGE ---

    /** Lấy danh sách đơn hàng cho Admin (có lọc, tìm kiếm, phân trang) */
    PageResponseDTO<AdminOrderDTO> getAdminOrders(Pageable pageable, String status, String searchTerm);

    /** Lấy chi tiết 1 đơn hàng cho Admin */
    AdminOrderDetailDTO getAdminOrderDetail(Long orderId);

    /** * Cập nhật trạng thái đơn hàng (linh hoạt)
     * HÀM LÕI ĐỂ GHI LOG
     */
    // --- 3. SỬA HÀM NÀY: Thêm 'User currentUser' ---
    AdminOrderDTO updateOrderStatus(Long orderId, UpdateStatusRequestDTO request, User currentUser);

    // --- 4. THÊM HÀM NÀY: Để đọc log ---
    /** Lấy lịch sử thao tác của một đơn hàng */
    List<OrderAuditLogResponseDTO> getOrderHistory(Long orderId);

// --- THÊM 2 HÀM MỚI CHO USER ---

    /** Lấy danh sách đơn hàng của User đang đăng nhập */
    PageResponseDTO<UserOrderDTO> getMyOrders(User user, Pageable pageable);

    /** Lấy chi tiết 1 đơn hàng của User đang đăng nhập (check sở hữu) */
    AdminOrderDetailDTO getMyOrderDetail(User user, Long orderId);

    /** User báo cáo vấn đề (khiếu nại) */

    /** User tự hủy đơn */
    OrderResponseDTO userCancelOrder(Long orderId, User user, String reason);

    OrderResponseDTO reportDeliveryIssue(Long orderId, User user, String reason);

    /** User xác nhận đã nhận hàng */
    OrderResponseDTO userConfirmDelivery(Long orderId, User user); // (Gợi ý: Hàm này nên gọi updateOrderStatus)

    // --- THÊM HÀM MỚI NÀY VÀO INTERFACE ---
    VnpayResponseDTO retryVnpayPayment(User user, Long orderId, HttpServletRequest httpServletRequest);
    String handleVnpayIpn(HttpServletRequest request);

    RefundResponseDTO requestVnpayRefund(
            Long orderId,
            HttpServletRequest request,
            User currentUser
    );

    List<OrderAuditLogResponseDTO> getMyOrderHistory(Long orderId, User user);
    RefundResponseDTO confirmCodRefund(Long orderId, User currentUser);
    AdminOrderDTO confirmStockReturn(Long orderId, User currentUser); // <-- THÊM HÀM NÀY

}