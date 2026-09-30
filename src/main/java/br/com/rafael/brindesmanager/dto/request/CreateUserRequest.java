package br.com.rafael.brindesmanager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120)
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 160)
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 100)
        String password

) {
}