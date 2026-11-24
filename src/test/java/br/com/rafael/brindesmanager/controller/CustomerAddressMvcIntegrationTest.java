package br.com.rafael.brindesmanager.controller;

import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.entity.Company;
import br.com.rafael.brindesmanager.entity.Customer;
import br.com.rafael.brindesmanager.repository.CustomerRepository;
import br.com.rafael.brindesmanager.security.*;
import br.com.rafael.brindesmanager.service.CustomerService;
import br.com.rafael.brindesmanager.service.PostalCodeService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MVC + production services/security + simulated HTTP; no application boot, database or Flyway. */
@WebMvcTest(controllers = {CustomerController.class, PostalCodeController.class}, properties = {
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@Import({CustomerService.class, PostalCodeService.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, CustomerAddressMvcIntegrationTest.ProviderStub.class})
class CustomerAddressMvcIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired MockRestServiceServer provider;
    @MockitoBean CustomerRepository repository;
    @MockitoBean CurrentUserService currentUser;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService userDetailsService;
    private Customer customer;

    @BeforeEach
    void setUp() {
        provider.reset();
        Company company = Company.builder().id(3L).build();
        AppUser user = AppUser.builder().id(7L).company(company).build();
        customer = Customer.builder().id(8L).companyName("Cliente").user(user).company(company).active(true).build();
        when(currentUser.getCurrentCompanyId()).thenReturn(3L);
        when(currentUser.getCurrentUser()).thenReturn(user);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void verifyProvider() {
        provider.verify();
    }

    @Test
    void anonymousRequestsAreRejectedBeforeAnyLookupOrPersistence() throws Exception {
        // Existing SecurityConfig uses the framework's forbidden response for anonymous requests.
        mvc.perform(get("/api/postal-codes/01001000")).andExpect(status().isForbidden());
        mvc.perform(get("/api/customers/8")).andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }

    @Test
    void invalidJwtKeepsExistingUnauthorizedResponse() throws Exception {
        when(jwtService.extractUsername("bad")).thenThrow(new JwtException("expired"));
        mvc.perform(get("/api/postal-codes/01001000").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCanLookupThroughProductionServiceAndOnlyPostalCodeLeavesApi() throws Exception {
        provider.expect(requestTo("https://viacep.com.br/ws/01001000/json/"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(headerDoesNotExist("Cookie"))
                .andRespond(withSuccess("{\"cep\":\"01001-000\",\"logradouro\":\"Praça da Sé\"}", MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/postal-codes/01001000").header("Cookie", "session=private"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postalCode").value("01001000"))
                .andExpect(jsonPath("$.street").value("Praça da Sé"))
                .andExpect(jsonPath("$.city").value(nullValue()))
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(jsonPath("$.complement").doesNotExist());
        verifyNoInteractions(repository);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanLookupAndInvalidParametersUseErrorEnvelope() throws Exception {
        mvc.perform(get("/api/postal-codes/01001-000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.messages").isArray());
        provider.expect(anything()).andRespond(withSuccess("{\"cep\":\"01001000\"}", MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/postal-codes/01001000")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void providerErrorsUseSanitizedApplicationEnvelope() throws Exception {
        provider.expect(anything()).andRespond(withSuccess("{\"erro\":true}", MediaType.APPLICATION_JSON));
        provider.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("private details"));
        provider.expect(anything()).andRespond(withException(new HttpTimeoutException("private details")));
        mvc.perform(get("/api/postal-codes/01001000"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.messages[0]").value("CEP não encontrado"));
        mvc.perform(get("/api/postal-codes/01001000"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.messages[0]").value("Consulta de CEP indisponível"));
        mvc.perform(get("/api/postal-codes/01001000"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.messages[0]").value("Tempo de consulta do CEP excedido"));
    }

    @Test
    @WithMockUser
    void createAcceptsOmittedNullEmptyAndInvalidTextWithoutProviderCall() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Cliente\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.postalCode").value(nullValue()));
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"companyName":"Cliente","postalCode":"abc12","street":"Manual",
                         "number":"S/N","complement":"","neighborhood":null,"city":"Cidade","state":"UF livre",
                         "companyId":999}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postalCode").value("abc12"))
                .andExpect(jsonPath("$.street").value("Manual"))
                .andExpect(jsonPath("$.number").value("S/N"))
                .andExpect(jsonPath("$.complement").value(""))
                .andExpect(jsonPath("$.neighborhood").value(nullValue()))
                .andExpect(jsonPath("$.state").value("UF livre"))
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.structuredAddress").doesNotExist());
        verify(repository, times(2)).save(argThat(value -> value.getCompany().getId().equals(3L)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"postalCode\":12345678", "\"street\":true", "\"number\":{}",
            "\"complement\":[]", "\"neighborhood\":false", "\"city\":123", "\"state\":42"})
    @WithMockUser
    void rejectsNonTextAddressProperties(String property) throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Cliente\"," + property + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("Corpo da requisição inválido"));
        verifyNoInteractions(repository);
    }

    @Test
    @WithMockUser
    void existingCompanyNameValidationRemains() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    @WithMockUser
    void putReplacesSevenFieldsAndResponsesAndDropdownExposeOnlyNewContract() throws Exception {
        when(repository.findByIdAndCompanyIdAndActiveTrue(8L, 3L)).thenReturn(Optional.of(customer));
        when(repository.findAllByCompanyIdOrderByCompanyNameAsc(3L)).thenReturn(List.of(customer));
        mvc.perform(put("/api/customers/8").contentType(MediaType.APPLICATION_JSON).content("""
                        {"companyName":"Cliente","postalCode":"12","street":"Rua","number":"4",
                         "complement":"Sala","neighborhood":"Bairro","city":"Cidade","state":"SP"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.postalCode").value("12"));
        mvc.perform(get("/api/customers/8"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.street").value("Rua"))
                .andExpect(jsonPath("$.address").doesNotExist());
        mvc.perform(get("/api/customers/dropdown"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].number").value("4"))
                .andExpect(jsonPath("$[0].complement").value("Sala"))
                .andExpect(jsonPath("$[0].neighborhood").value("Bairro"))
                .andExpect(jsonPath("$[0].city").value("Cidade"))
                .andExpect(jsonPath("$[0].state").value("SP"))
                .andExpect(jsonPath("$[0].address").doesNotExist());
        mvc.perform(put("/api/customers/8").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Cliente\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.street").value(nullValue()))
                .andExpect(jsonPath("$.postalCode").value(nullValue()));
    }

    @Test
    @WithMockUser
    void otherCompanyOrInactiveCustomerIsNotFoundAndCannotBeUpdated() throws Exception {
        when(repository.findByIdAndCompanyIdAndActiveTrue(99L, 3L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/customers/99")).andExpect(status().isNotFound());
        mvc.perform(put("/api/customers/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Cliente\"}"))
                .andExpect(status().isNotFound());
        verify(repository, never()).save(any());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProviderStub {
        @Bean RestClient.Builder providerBuilder() {
            return RestClient.builder().baseUrl("https://viacep.com.br");
        }
        @Bean MockRestServiceServer providerServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
        @Bean RestClient postalCodeRestClient(RestClient.Builder builder, MockRestServiceServer server) {
            return builder.build();
        }
    }
}
