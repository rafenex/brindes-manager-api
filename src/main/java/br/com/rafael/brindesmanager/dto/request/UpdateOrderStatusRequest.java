package br.com.rafael.brindesmanager.dto.request;

import br.com.rafael.brindesmanager.enums.OrderStatus;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;

public record UpdateOrderStatusRequest(

        @NotNull(message = "O status é obrigatório")
        @JsonDeserialize(using = UpdateOrderStatusRequest.StatusDeserializer.class)
        OrderStatus status
) {
    public static final class StatusDeserializer extends StdDeserializer<OrderStatus> {
        public StatusDeserializer() {
            super(OrderStatus.class);
        }

        @Override
        public OrderStatus deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return (OrderStatus) context.handleUnexpectedToken(OrderStatus.class, parser);
            }
            String code = parser.getText();
            try {
                return OrderStatus.valueOf(code);
            } catch (IllegalArgumentException ex) {
                return (OrderStatus) context.handleWeirdStringValue(OrderStatus.class, code, "Código de status inválido");
            }
        }
    }
}
