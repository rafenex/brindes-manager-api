package br.com.rafael.brindesmanager.controller;

import br.com.rafael.brindesmanager.dto.request.CustomerRequest;
import br.com.rafael.brindesmanager.dto.response.CustomerDropdownResponse;
import br.com.rafael.brindesmanager.dto.response.CustomerResponse;
import br.com.rafael.brindesmanager.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@RequestBody @Valid CustomerRequest request) {
        return customerService.create(request);
    }

    @GetMapping
    public List<CustomerResponse> findAll() {
        return customerService.findAll();
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(
            @PathVariable Long id,
            @RequestBody @Valid CustomerRequest request
    ) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }

    @GetMapping("/dropdown")
    public List<CustomerDropdownResponse> findAllDropdown() {
        return customerService.findAllDropdown();
    }
}