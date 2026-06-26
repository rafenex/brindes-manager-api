package br.com.rafael.brindesmanager.dto.request;

import br.com.rafael.brindesmanager.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "O status é obrigatório")
        OrderStatus status
) {
}