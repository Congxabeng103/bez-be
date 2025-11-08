package com.poly.bezbe.service.impl;

import com.poly.bezbe.entity.Order;
import com.poly.bezbe.entity.OrderAuditLog;
import com.poly.bezbe.entity.User;
import com.poly.bezbe.repository.OrderAuditLogRepository;
import com.poly.bezbe.service.OrderAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderAuditLogServiceImpl implements OrderAuditLogService {

    private final OrderAuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void logActivity(Order order, User staff, String description,
                            String field, String oldVal, String newVal) {

        OrderAuditLog log = new OrderAuditLog();
        log.setOrder(order);
        log.setDescription(description);
        log.setFieldChanged(field);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);

        // --- BẮT ĐẦU SỬA LỖI ---
        // Phải kiểm tra staff CÓ NULL HAY KHÔNG trước khi sử dụng
        if (staff != null) {
            // Nếu staff tồn tại (Admin/Nhân viên làm)
            log.setStaff(staff); // Gán entity User

            // Ghép firstName và lastName
            String staffName = staff.getFirstName();
            if (staff.getLastName() != null && !staff.getLastName().isEmpty()) {
                staffName += " " + staff.getLastName();
            }
            log.setStaffName(staffName); // Gán tên
        } else {
            // Nếu staff là null (Hệ thống tự động hoặc User đặt hàng)
            log.setStaff(null); // Đảm bảo gán null (nếu cột này cho phép null)
            log.setStaffName("Hệ thống"); // Gán tên mặc định
        }
        // --- KẾT THÚC SỬA ---

        // Không cần .setCreatedAt(), @CreationTimestamp tự lo
        auditLogRepository.save(log);
    }
    // --- THÊM HÀM IMPLEMENT MỚI ---
    /**
     * Implement hàm cho HỆ THỐNG / KHÁCH HÀNG (Chỉ có tên)
     */
    @Override
    @Transactional
    public void logActivity(Order order, String actorName, String description,
                            String field, String oldVal, String newVal) {

        OrderAuditLog log = new OrderAuditLog();
        log.setOrder(order);
        log.setDescription(description);
        log.setFieldChanged(field);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);

        log.setStaff(null); // Không có User entity
        log.setStaffName(actorName); // Dùng tên được truyền vào trực tiếp

        auditLogRepository.save(log);
    }
}