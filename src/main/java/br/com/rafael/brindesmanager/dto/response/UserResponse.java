package br.com.rafael.brindesmanager.dto.response;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        Boolean active
) {
}