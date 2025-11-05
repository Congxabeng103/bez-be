package com.poly.bezbe.service;

import com.poly.bezbe.entity.Order;
import com.poly.bezbe.entity.User;

public interface OrderAuditLogService {

    void logActivity(Order order, User staff, String description,
                     String field, String oldVal, String newVal);
}