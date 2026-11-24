package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.CustomerRequest;
import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.entity.Company;
import br.com.rafael.brindesmanager.entity.Customer;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.repository.CustomerRepository;
import br.com.rafael.brindesmanager.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock CustomerRepository repository;
    @Mock CurrentUserService currentUser;
    private CustomerService service;
    private AppUser user;
    private Customer customer;

    @BeforeEach
    void setUp() {
        service = new CustomerService(repository, currentUser);
        user = AppUser.builder().id(7L).company(Company.builder().id(3L).build()).build();
        customer = Customer.builder().id(8L).companyName("Cliente").user(user)
                .company(user.getCompany()).active(true).build();
    }

    @Test
    void createKeepsAllManualAddressFieldsIncludingInvalidPostalCodeAndLongText() {
        when(currentUser.getCurrentUser()).thenReturn(user);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(request("abc-12", "Rua ".repeat(100), "S/N", "Manual", "Bairro", "Cidade", "UF manual"));
        assertThat(response.postalCode()).isEqualTo("abc-12");
        assertThat(response.street()).isEqualTo("Rua ".repeat(100));
        assertThat(response.number()).isEqualTo("S/N");
        assertThat(response.complement()).isEqualTo("Manual");
        assertThat(response.neighborhood()).isEqualTo("Bairro");
        assertThat(response.city()).isEqualTo("Cidade");
        assertThat(response.state()).isEqualTo("UF manual");
        verify(repository).save(argThat(value -> value.getCompany() == user.getCompany() && value.getUser() == user));
    }

    @Test
    void updateReplacesAllSevenValuesAndCanClearEverything() {
        when(currentUser.getCurrentCompanyId()).thenReturn(3L);
        when(repository.findByIdAndCompanyIdAndActiveTrue(8L, 3L)).thenReturn(Optional.of(customer));
        when(repository.save(customer)).thenReturn(customer);
        service.update(8L, request("01001-000", "Rua", "12", "Sala", "Centro", "Cidade", "SP"));
        var read = service.findById(8L);
        assertThat(read.postalCode()).isEqualTo("01001-000");
        assertThat(read.street()).isEqualTo("Rua");
        assertThat(read.number()).isEqualTo("12");
        assertThat(read.complement()).isEqualTo("Sala");
        assertThat(read.neighborhood()).isEqualTo("Centro");
        assertThat(read.city()).isEqualTo("Cidade");
        assertThat(read.state()).isEqualTo("SP");
        var cleared = service.update(8L, request(null, null, null, null, null, null, null));
        assertThat(cleared.postalCode()).isNull();
        assertThat(cleared.street()).isNull();
        assertThat(cleared.number()).isNull();
        assertThat(cleared.complement()).isNull();
        assertThat(cleared.neighborhood()).isNull();
        assertThat(cleared.city()).isNull();
        assertThat(cleared.state()).isNull();
        assertThat(customer.getId()).isEqualTo(8L);
        assertThat(customer.getCompany()).isSameAs(user.getCompany());
        assertThat(customer.getUser()).isSameAs(user);
        assertThat(customer.getActive()).isTrue();
    }

    @Test
    void missingInactiveOrOtherCompanyCustomerCannotBeReadOrChanged() {
        when(currentUser.getCurrentCompanyId()).thenReturn(3L);
        when(repository.findByIdAndCompanyIdAndActiveTrue(99L, 3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.update(99L, request(null, null, null, null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void dropdownKeepsInactiveRecordsAndEveryAddressFieldWithinCompany() {
        customer.setActive(false);
        customer.setPostalCode("12"); customer.setStreet("Rua"); customer.setNumber("S/N");
        customer.setComplement("Sala"); customer.setNeighborhood("Centro");
        customer.setCity("Cidade"); customer.setState("SP");
        when(currentUser.getCurrentCompanyId()).thenReturn(3L);
        when(repository.findAllByCompanyIdOrderByCompanyNameAsc(3L)).thenReturn(List.of(customer));
        var result = service.findAllDropdown().getFirst();
        assertThat(result.active()).isFalse();
        assertThat(result.postalCode()).isEqualTo("12");
        assertThat(result.street()).isEqualTo("Rua");
        assertThat(result.number()).isEqualTo("S/N");
        assertThat(result.complement()).isEqualTo("Sala");
        assertThat(result.neighborhood()).isEqualTo("Centro");
        assertThat(result.city()).isEqualTo("Cidade");
        assertThat(result.state()).isEqualTo("SP");
    }

    private CustomerRequest request(String postal, String street, String number, String complement,
                                    String neighborhood, String city, String state) {
        return new CustomerRequest("Contato", "Cliente", postal, street, number, complement,
                neighborhood, city, state, "doc", "mail@example.com", "phone");
    }
}
