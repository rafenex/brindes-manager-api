package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.response.PostalCodeResponse;
import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.exception.PostalCodeIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

@Service
public class PostalCodeService {
    private static final Logger log = LoggerFactory.getLogger(PostalCodeService.class);
    private final RestClient client;

    public PostalCodeService(@Qualifier("postalCodeRestClient") RestClient client) {
        this.client = client;
    }

    public PostalCodeResponse find(String postalCode) {
        if (postalCode == null || !postalCode.matches("[0-9]{8}")) {
            throw new BusinessException("Informe oito dígitos para consultar o CEP");
        }
        long started = System.nanoTime();
        String outcome = "unavailable";
        try {
            JsonNode body = client.get().uri("/ws/{postalCode}/json/", postalCode)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        throw new PostalCodeIntegrationException(false);
                    })
                    .body(JsonNode.class);
            PostalCodeResponse result = parse(body, postalCode);
            outcome = "success";
            return result;
        } catch (NotFoundException ex) {
            outcome = "not_found";
            throw ex;
        } catch (RestClientException ex) {
            boolean timeout = isTimeout(ex);
            outcome = timeout ? "timeout" : "unavailable";
            throw new PostalCodeIntegrationException(timeout);
        } finally {
            // No CEP, response payload, credentials or exception messages in operational logs.
            log.info("Postal lookup outcome={} durationMs={}", outcome,
                    (System.nanoTime() - started) / 1_000_000);
        }
    }

    private PostalCodeResponse parse(JsonNode body, String postalCode) {
        if (body == null || !body.isObject()) {
            throw new PostalCodeIntegrationException(false);
        }
        JsonNode error = body.get("erro");
        if (error != null && !error.isNull()) {
            if ((error.isBoolean() && error.booleanValue())
                    || (error.isTextual() && "true".equalsIgnoreCase(error.textValue()))) {
                throw new NotFoundException("CEP não encontrado");
            }
            if (!(error.isBoolean() && !error.booleanValue())
                    && !(error.isTextual() && "false".equalsIgnoreCase(error.textValue()))) {
                throw new PostalCodeIntegrationException(false);
            }
        }
        String returnedCode = field(body, "cep");
        if (returnedCode == null || !returnedCode.matches("[0-9]{5}-?[0-9]{3}")
                || !postalCode.equals(returnedCode.replace("-", ""))) {
            throw new PostalCodeIntegrationException(false);
        }
        return new PostalCodeResponse(postalCode, field(body, "logradouro"), field(body, "bairro"),
                field(body, "localidade"), field(body, "uf"));
    }

    private String field(JsonNode body, String name) {
        JsonNode value = body.get(name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new PostalCodeIntegrationException(false);
        }
        return value.textValue().isBlank() ? null : value.textValue();
    }

    private boolean isTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException
                    || cause instanceof TimeoutException) {
                return true;
            }
        }
        return false;
    }
}
