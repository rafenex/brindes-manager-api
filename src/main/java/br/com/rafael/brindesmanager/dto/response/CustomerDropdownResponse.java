package br.com.rafael.brindesmanager.dto.response;

public record CustomerDropdownResponse(
        Long id,
        String name,
        String companyName,
        String address,
        Boolean active
) {
}