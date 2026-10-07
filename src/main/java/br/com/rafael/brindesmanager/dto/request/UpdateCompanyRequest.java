package br.com.rafael.brindesmanager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(

        @NotBlank(message = "O nome da empresa é obrigatório")
        @Size(max = 150, message = "O nome da empresa deve ter no máximo 150 caracteres")
        String name,

        @Size(max = 255, message = "O endereço deve ter no máximo 255 caracteres")
        String address,

        @Email(message = "E-mail inválido")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        String email,

        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String phone
) {
}