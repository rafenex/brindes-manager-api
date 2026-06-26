package br.com.rafael.brindesmanager.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CustomerOrderRequest(

        @NotNull(message = "O cliente é obrigatório")
        Long customerId,

        @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres")
        String notes,

        @NotEmpty(message = "O pedido deve ter pelo menos um item")
        List<@Valid OrderItemRequest> items
) {
}