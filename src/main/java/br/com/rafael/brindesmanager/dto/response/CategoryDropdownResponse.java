package br.com.rafael.brindesmanager.dto.response;

public record CategoryDropdownResponse(
        Long id,
        String name,
        Boolean active
) {
}