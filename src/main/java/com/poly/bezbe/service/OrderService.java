package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.OrderRequestDTO;
import com.poly.bezbe.dto.request.UpdateStatusRequestDTO;
import com.poly.bezbe.dto.response.*;
import com.poly.bezbe.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

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
    OrderResponseDTO adminConfirmOrder(Long orderId);

    /**
     * Admin hủy đơn hàng. Tự động trả hàng về kho nếu cần.
     */
    OrderResponseDTO adminCancelOrder(Long orderId);
    // --- THÊM 3 HÀM MỚI CHO ADMIN PAGE ---

    /** Lấy danh sách đơn hàng cho Admin (có lọc, tìm kiếm, phân trang) */
    PageResponseDTO<AdminOrderDTO> getAdminOrders(Pageable pageable, String status, String searchTerm);

    /** Lấy chi tiết 1 đơn hàng cho Admin */
    AdminOrderDetailDTO getAdminOrderDetail(Long orderId);

    /** Cập nhật trạng thái đơn hàng (linh hoạt) */
    AdminOrderDTO updateOrderStatus(Long orderId, UpdateStatusRequestDTO request);

// --- THÊM 2 HÀM MỚI CHO USER ---

    /** Lấy danh sách đơn hàng của User đang đăng nhập */
    PageResponseDTO<UserOrderDTO> getMyOrders(User user, Pageable pageable);

    /** Lấy chi tiết 1 đơn hàng của User đang đăng nhập (check sở hữu) */
    AdminOrderDetailDTO getMyOrderDetail(User user, Long orderId);

    /** User báo cáo vấn đề (khiếu nại) */
    OrderResponseDTO reportDeliveryIssue(Long orderId, User user);

    /** User tự hủy đơn */
    OrderResponseDTO userCancelOrder(Long orderId, User user);

    /** User xác nhận đã nhận hàng */
    OrderResponseDTO userConfirmDelivery(Long orderId, User user);
}