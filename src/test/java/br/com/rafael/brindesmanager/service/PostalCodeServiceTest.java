package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.exception.BusinessException;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.exception.PostalCodeIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PostalCodeServiceTest {
    private MockRestServiceServer server;
    private PostalCodeService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.com.br");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new PostalCodeService(builder.build());
    }

    @Test
    void mapsOnlyApprovedFieldsWithoutForwardingCredentials() {
        server.expect(requestTo("https://viacep.com.br/ws/01001000/json/"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(headerDoesNotExist("Cookie"))
                .andRespond(withSuccess("""
                        {"cep":"01001-000","logradouro":"Praça da Sé","bairro":"Sé",
                         "localidade":"São Paulo","uf":"SP","complemento":"ignored","extra":42}
                        """, MediaType.APPLICATION_JSON));
        var result = service.find("01001000");
        assertThat(result.postalCode()).isEqualTo("01001000");
        assertThat(result.street()).isEqualTo("Praça da Sé");
        assertThat(result.neighborhood()).isEqualTo("Sé");
        assertThat(result.city()).isEqualTo("São Paulo");
        assertThat(result.state()).isEqualTo("SP");
        server.verify();
    }

    @Test
    void acceptsPartialSuccessAsNullFields() {
        respond("{\"cep\":\"01001000\",\"logradouro\":\"\",\"bairro\":null}");
        var result = service.find("01001000");
        assertThat(result.street()).isNull();
        assertThat(result.neighborhood()).isNull();
        assertThat(result.city()).isNull();
        assertThat(result.state()).isNull();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "01001-000", "abcdefgh", "123456789", "１２３４５６７８", "../other"})
    void rejectsInvalidLookupWithoutExternalCall(String value) {
        assertThatThrownBy(() -> service.find(value)).isInstanceOf(BusinessException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"erro\":true}", "{\"erro\":\"true\"}", "{\"erro\":\"TRUE\"}"})
    void reportsNotFound(String body) {
        respond(body);
        assertThatThrownBy(() -> service.find("01001000")).isInstanceOf(NotFoundException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "{", "{}", "{\"cep\":123}",
            "{\"cep\":\"99999-999\"}", "{\"cep\":\"01001-000\",\"uf\":42}",
            "{\"cep\":\"01001-000\",\"logradouro\":[]}", "{\"erro\":1}",
            "{\"cep\":\"01001-000\",\"erro\":\"unexpected\"}"})
    void invalidProviderBodyIsUnavailable(String body) {
        respond(body);
        assertUnavailable(false);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 400, 401, 404, 429, 500, 503})
    void unexpectedHttpStatusIsUnavailableAndNotRetried(int code) {
        server.expect(requestTo("https://viacep.com.br/ws/01001000/json/"))
                .andRespond(withStatus(HttpStatus.valueOf(code)));
        assertUnavailable(false);
        server.verify();
    }

    @Test
    void networkFailureIsUnavailable() {
        server.expect(anything()).andRespond(withException(new IOException("private provider details")));
        assertUnavailable(false);
        server.verify();
    }

    @Test
    void httpTimeoutIsDistinctFromUnavailable() {
        server.expect(anything()).andRespond(withException(new HttpTimeoutException("private details")));
        assertUnavailable(true);
        server.verify();
    }

    @Test
    void wrappedJdkReadTimeoutIsRecognized() {
        server.expect(anything()).andRespond(withException(new IOException(new TimeoutException())));
        assertUnavailable(true);
        server.verify();
    }

    private void respond(String body) {
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void assertUnavailable(boolean timeout) {
        assertThatThrownBy(() -> service.find("01001000"))
                .isInstanceOfSatisfying(PostalCodeIntegrationException.class,
                        ex -> assertThat(ex.isTimeout()).isEqualTo(timeout))
                .hasMessage(timeout ? "Tempo de consulta do CEP excedido" : "Consulta de CEP indisponível");
    }
}
