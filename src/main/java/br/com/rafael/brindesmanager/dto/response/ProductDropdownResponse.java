package br.com.rafael.brindesmanager.dto.response;

import java.math.BigDecimal;

public record ProductDropdownResponse(
        Long id,
        String reference,
        String name,
        BigDecimal basePrice,
        Boolean active
) {
}