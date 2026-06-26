package br.com.rafael.brindesmanager.controller;

import br.com.rafael.brindesmanager.dto.request.CustomerOrderRequest;
import br.com.rafael.brindesmanager.dto.request.UpdateOrderStatusRequest;
import br.com.rafael.brindesmanager.dto.response.CustomerOrderResponse;
import br.com.rafael.brindesmanager.service.CustomerOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerOrderResponse create(@RequestBody @Valid CustomerOrderRequest request) {
        return customerOrderService.create(request);
    }

    @GetMapping
    public List<CustomerOrderResponse> findAll() {
        return customerOrderService.findAll();
    }

    @GetMapping("/{id}")
    public CustomerOrderResponse findById(@PathVariable Long id) {
        return customerOrderService.findById(id);
    }

    @PutMapping("/{id}")
    public CustomerOrderResponse update(
            @PathVariable Long id,
            @RequestBody @Valid CustomerOrderRequest request
    ) {
        return customerOrderService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public CustomerOrderResponse updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrderStatusRequest request
    ) {
        return customerOrderService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerOrderService.delete(id);
    }
}