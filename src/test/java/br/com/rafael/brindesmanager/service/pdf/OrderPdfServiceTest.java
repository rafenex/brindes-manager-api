package br.com.rafael.brindesmanager.service.pdf;

import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.entity.Company;
import br.com.rafael.brindesmanager.entity.Customer;
import br.com.rafael.brindesmanager.entity.CustomerOrder;
import br.com.rafael.brindesmanager.enums.OrderStatus;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.service.CustomerOrderService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPdfServiceTest {
    @ParameterizedTest
    @CsvSource({"PAID,Pago", "BUDGET,Orçamento", "APPROVED,Aprovado",
            "IN_PRODUCTION,Em produção", "DELIVERED,Entregue", "CANCELED,Cancelado"})
    void presentsTheCurrentStatusInThePdf(OrderStatus status, String label) throws Exception {
        order.setStatus(status);
        assertThat(generatedText().replaceAll("\\s+", " ")).contains("Status " + label);
    }

    @Mock CustomerOrderService orders;
    private OrderPdfService service;
    private Customer customer;
    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        service = new OrderPdfService(orders);
        customer = Customer.builder().companyName("Cliente").build();
        order = CustomerOrder.builder().id(8L).code("PED-8").status(OrderStatus.BUDGET)
                .createdAt(LocalDateTime.of(2026, 11, 14, 10, 0))
                .company(Company.builder().name("Empresa").address("company-only-address").build())
                .user(AppUser.builder().name("Vendedor").build()).customer(customer)
                .totalAmount(BigDecimal.TEN).build();
    }

    @Test
    void generatedPdfUsesAllSevenFieldsAndPreservesOrderPresentation() throws Exception {
        customer.setPostalCode("01001000"); customer.setStreet("Rua X"); customer.setNumber("12");
        customer.setComplement("Sala 2"); customer.setNeighborhood("Centro");
        customer.setCity("Cidade"); customer.setState("SP");
        String text = generatedText().replaceAll("\\s+", " ");
        assertThat(text).contains("Rua X, 12 — Sala 2 — Centro — Cidade/SP — CEP 01001-000");
        assertThat(text).contains("PED-8", "Vendedor", "Cliente", "Total:");
        assertThat(text).doesNotContain("company-only-address");
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(order.getCustomer()).isSameAs(customer);
        verify(orders).findActiveOrderByIdAndCurrentUser(8L);
    }

    @Test
    void numberAloneAndStateAloneHaveNoOrphanSeparators() throws Exception {
        customer.setNumber("S/N");
        assertThat(generatedText()).contains("S/N").doesNotContain("—", "CEP ");
        customer.setNumber(null); customer.setState("SP");
        assertThat(generatedText()).contains("SP").doesNotContain("/SP", "—", "CEP ");
    }

    @Test
    void emptyAddressUsesExistingPlaceholderWithoutRecoveringAnyLegacyText() throws Exception {
        String text = generatedText().replaceAll("\\s+", " ");
        assertThat(text).contains("Endereço - Documento");
        assertThat(text).doesNotContain("CEP ", "—", "null");
    }

    @Test
    void manualInvalidCepAndAccentedLongTextAreStillPresentedAsPlainText() throws Exception {
        customer.setPostalCode("abc-12");
        customer.setStreet("Praça São João ".repeat(40));
        customer.setComplement("<b>Manual</b>");
        String text = generatedText().replaceAll("\\s+", " ");
        assertThat(text).contains("Praça São João", "<b>Manual</b>", "CEP abc-12");
    }

    @Test
    void forbiddenOrMissingOrderNeverGeneratesPdf() {
        when(orders.findActiveOrderByIdAndCurrentUser(8L)).thenThrow(new NotFoundException("Pedido não encontrado"));
        assertThatThrownBy(() -> service.generateOrderPdf(8L)).isInstanceOf(NotFoundException.class);
    }

    private String generatedText() throws Exception {
        when(orders.findActiveOrderByIdAndCurrentUser(8L)).thenReturn(order);
        PdfReader reader = new PdfReader(service.generateOrderPdf(8L));
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
