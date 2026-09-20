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
                .document(request.document())
                .email(request.email())
                .phone(request.phone())
                .user(user)
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        return toResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        Long userId = currentUserService.getCurrentUserId();

        return customerRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        Long userId = currentUserService.getCurrentUserId();

        Customer customer = findActiveCustomerByIdAndUser(id, userId);

        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Long userId = currentUserService.getCurrentUserId();

        Customer customer = findActiveCustomerByIdAndUser(id, userId);

        customer.setName(request.name());
        customer.setCompanyName(request.companyName());
        customer.setDocument(request.document());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setUpdatedAt(LocalDateTime.now());

        Customer updatedCustomer = customerRepository.save(customer);

        return toResponse(updatedCustomer);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserService.getCurrentUserId();

        Customer customer = findActiveCustomerByIdAndUser(id, userId);

        customer.setActive(false);
        customer.setUpdatedAt(LocalDateTime.now());

        customerRepository.save(customer);
    }

    public Customer findActiveCustomerByIdAndCurrentUser(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        return findActiveCustomerByIdAndUser(id, userId);
    }

    private Customer findActiveCustomerByIdAndUser(Long id, Long userId) {
        return customerRepository.findByIdAndUserIdAndActiveTrue(id, userId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getCompanyName(),
                customer.getDocument(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getActive(),
                customer.getUser().getId(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CustomerDropdownResponse> findAllDropdown() {
        Long userId = currentUserService.getCurrentUserId();

        return customerRepository.findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(customer -> new CustomerDropdownResponse(
                        customer.getId(),
                        customer.getName(),
                        customer.getCompanyName(),
                        customer.getActive()
                ))
                .toList();
    }
}