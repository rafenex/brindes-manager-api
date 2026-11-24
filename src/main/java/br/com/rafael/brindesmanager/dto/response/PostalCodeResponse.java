package br.com.rafael.brindesmanager.dto.response;

public record PostalCodeResponse(
        String postalCode,
        String street,
        String neighborhood,
        String city,
        String state
) {
}
