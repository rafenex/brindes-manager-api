package br.com.rafael.brindesmanager.dto.response;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String name,
        String companyName,
        String document,
        String email,
        String phone,
        Boolean active,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}