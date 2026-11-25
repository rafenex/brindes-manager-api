package br.com.rafael.brindesmanager.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {

    BUDGET("Orçamento"),
    APPROVED("Aprovado"),
    IN_PRODUCTION("Em produção"),
    DELIVERED("Entregue"),
    CANCELED("Cancelado"),
    PAID("Pago");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

}
