package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.CustomerRequest;
import br.com.rafael.brindesmanager.dto.response.CustomerDropdownResponse;
import br.com.rafael.brindesmanager.dto.response.CustomerResponse;
import br.com.rafael.brindesmanager.entity.AppUser;
import br.com.rafael.brindesmanager.entity.Customer;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.repository.CustomerRepository;
import br.com.rafael.brindesmanager.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        AppUser user = currentUserService.getCurrentUser();

        Customer customer = Customer.builder()
                .name(request.name())
                .companyName(request.companyName())
                .address(request.address())
                .document(request.document())
                .email(request.email())
                .phone(request.phone())
                .user(user)
                .company(user.getCompany())
                .build();

        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        Long companyId = currentUserService.getCurrentCompanyId();

        return customerRepository
                .findAllByCompanyIdAndActiveTrueOrderByNameAsc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        Long companyId = currentUserService.getCurrentCompanyId();

        return toResponse(findActiveCustomerByIdAndCompany(id, companyId));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Long companyId = currentUserService.getCurrentCompanyId();

        Customer customer = findActiveCustomerByIdAndCompany(id, companyId);

        customer.setName(request.name());
        customer.setCompanyName(request.companyName());
        customer.setAddress(request.address());
        customer.setDocument(request.document());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setUpdatedAt(LocalDateTime.now());

        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Long companyId = currentUserService.getCurrentCompanyId();

        Customer customer = findActiveCustomerByIdAndCompany(id, companyId);

        customer.setActive(false);
        customer.setUpdatedAt(LocalDateTime.now());

        customerRepository.save(customer);
    }

    public Customer findActiveCustomerByIdAndCurrentUser(Long id) {
        Long companyId = currentUserService.getCurrentCompanyId();

        return findActiveCustomerByIdAndCompany(id, companyId);
    }

    private Customer findActiveCustomerByIdAndCompany(Long id, Long companyId) {
        return customerRepository
                .findByIdAndCompanyIdAndActiveTrue(id, companyId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<CustomerDropdownResponse> findAllDropdown() {
        Long companyId = currentUserService.getCurrentCompanyId();

        return customerRepository
                .findAllByCompanyIdOrderByCompanyNameAsc(companyId)
                .stream()
                .map(customer -> new CustomerDropdownResponse(
                        customer.getId(),
                        customer.getName(),
                        customer.getCompanyName(),
                        customer.getAddress(),
                        customer.getActive()
                ))
                .toList();
    }

    public Customer findCustomerByIdAndCurrentUser(Long id) {
        Long companyId = currentUserService.getCurrentCompanyId();

        return customerRepository
                .findByIdAndCompanyId(id, companyId)
                .orElseThrow(() ->
                        new NotFoundException("Cliente não encontrado")
                );
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getCompanyName(),
                customer.getAddress(),
                customer.getDocument(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getActive(),
                customer.getUser().getId(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
