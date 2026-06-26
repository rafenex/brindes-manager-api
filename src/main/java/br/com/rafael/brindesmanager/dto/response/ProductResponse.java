package br.com.rafael.brindesmanager.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String reference,
        String name,
        String description,
        BigDecimal basePrice,
        Boolean active,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}