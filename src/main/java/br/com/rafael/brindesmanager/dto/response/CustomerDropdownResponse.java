package br.com.rafael.brindesmanager.dto.response;

public record CustomerDropdownResponse(
        Long id,
        String name,
        String companyName,
        String postalCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        Boolean active
) {
}