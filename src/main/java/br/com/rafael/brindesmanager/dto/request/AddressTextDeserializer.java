package br.com.rafael.brindesmanager.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/** Keeps the seven optional address properties textual without changing global coercion. */
public class AddressTextDeserializer extends StdDeserializer<String> {
    public AddressTextDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_STRING)) {
            return parser.getText();
        }
        return (String) context.handleUnexpectedToken(String.class, parser);
    }
}
