package br.com.rafael.brindesmanager.controller;

import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.entity.Customer;
import br.com.rafael.brindesmanager.entity.CustomerOrder;
import br.com.rafael.brindesmanager.enums.OrderStatus;
import br.com.rafael.brindesmanager.repository.CustomerOrderRepository;
import br.com.rafael.brindesmanager.security.CurrentUserService;
import br.com.rafael.brindesmanager.security.CustomUserDetailsService;
import br.com.rafael.brindesmanager.security.JwtAuthenticationFilter;
import br.com.rafael.brindesmanager.security.JwtService;
import br.com.rafael.brindesmanager.security.SecurityConfig;
import br.com.rafael.brindesmanager.service.CustomerOrderService;
import br.com.rafael.brindesmanager.service.CustomerService;
import br.com.rafael.brindesmanager.service.ProductService;
import br.com.rafael.brindesmanager.service.pdf.OrderPdfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerOrderController.class)
@Import({CustomerOrderService.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class OrderStatusMvcIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean CustomerOrderRepository orders;
    @MockitoBean CustomerService customers;
    @MockitoBean ProductService products;
    @MockitoBean CurrentUserService currentUser;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService userDetailsService;
    @MockitoBean OrderPdfService pdfService;

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "PAID", mode = EnumSource.Mode.EXCLUDE)
    @WithMockUser
    void marksEachExistingStatusPaidAndReadsItBack(OrderStatus original) throws Exception {
        CustomerOrder order = CustomerOrder.builder().id(9L).status(original).active(true)
                .customer(Customer.builder().id(3L).build())
                .user(AppUser.builder().id(5L).build()).items(List.of()).build();
        when(currentUser.getCurrentCompanyId()).thenReturn(2L);
        when(orders.findByIdAndCompanyIdAndActiveTrue(9L, 2L)).thenReturn(Optional.of(order));
        when(orders.save(order)).thenReturn(order);

        update(9, "PAID").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        verify(orders).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        mvc.perform(get("/api/orders/9"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        update(9, original.name()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(original.name()));
        assertThat(order.getStatus()).isEqualTo(original);
    }

    @Test
    void rejectsAnonymousChanges() throws Exception {
        update(9, "PAID").andExpect(status().isForbidden());
        verifyNoInteractions(orders);
    }

    @Test
    @WithMockUser
    void neverSavesAnInactiveOrderOrAnOrderFromAnotherCompany() throws Exception {
        when(currentUser.getCurrentCompanyId()).thenReturn(2L);
        update(9, "PAID").andExpect(status().isNotFound());
        verify(orders).findByIdAndCompanyIdAndActiveTrue(9L, 2L);
        verify(orders, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "\"5\"", "\"UNKNOWN\"", "\"paid\"", "\" PAID \"", "null"})
    @WithMockUser
    void rejectsInvalidStatusBeforeReadingOrSaving(String jsonValue) throws Exception {
        mvc.perform(patch("/api/orders/9/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":" + jsonValue + "}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orders);
    }

    private org.springframework.test.web.servlet.ResultActions update(long id, String status) throws Exception {
        return mvc.perform(patch("/api/orders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }
}
