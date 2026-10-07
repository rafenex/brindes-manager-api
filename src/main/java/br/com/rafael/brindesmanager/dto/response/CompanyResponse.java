package br.com.rafael.brindesmanager.dto.response;

public record CompanyResponse(
        Long id,
        String name,
        String address,
        String email,
        String phone,
        Boolean hasLogo
) {
}