package br.com.rafael.brindesmanager.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "A referência do produto é obrigatória")
        @Size(max = 50, message = "A referência deve ter no máximo 50 caracteres")
        String reference,

        @NotBlank(message = "O nome do produto é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String name,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String description,

        @NotNull(message = "O preço base é obrigatório")
        @DecimalMin(value = "0.00", message = "O preço base não pode ser negativo")
        BigDecimal basePrice,

        @NotNull(message = "A categoria é obrigatória")
        Long categoryId
) {
}