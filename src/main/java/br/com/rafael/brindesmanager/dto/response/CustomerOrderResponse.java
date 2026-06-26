package br.com.rafael.brindesmanager.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerOrderResponse(
        Long id,
        String code,
        Long customerId,
        String customerName,
        Long userId,
        String status,
        BigDecimal totalAmount,
        String notes,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {
}