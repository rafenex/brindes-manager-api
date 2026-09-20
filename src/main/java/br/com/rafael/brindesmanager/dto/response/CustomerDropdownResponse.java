package br.com.rafael.brindesmanager.dto.response;

public record CustomerDropdownResponse(
        Long id,
        String name,
        String companyName,
        Boolean active
) {
}