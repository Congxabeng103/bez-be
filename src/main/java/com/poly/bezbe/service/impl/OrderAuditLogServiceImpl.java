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
        log.setStaff(staff);
        log.setDescription(description);
        log.setFieldChanged(field);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);

        // --- DÙNG CHÍNH XÁC User.java CỦA BẠN ---
        // Ghép firstName và lastName
        String staffName = staff.getFirstName();
        if (staff.getLastName() != null && !staff.getLastName().isEmpty()) {
            staffName += " " + staff.getLastName();
        }
        log.setStaffName(staffName); // Đã sửa, không còn "bịa"
        // --- KẾT THÚC SỬA ---

        // Không cần .setCreatedAt(), @CreationTimestamp tự lo
        auditLogRepository.save(log);
    }
}