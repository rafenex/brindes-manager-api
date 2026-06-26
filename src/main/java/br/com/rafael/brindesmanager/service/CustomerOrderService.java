package br.com.rafael.brindesmanager.service;

import br.com.rafael.brindesmanager.dto.request.CustomerOrderRequest;
import br.com.rafael.brindesmanager.dto.request.OrderItemRequest;
import br.com.rafael.brindesmanager.dto.request.UpdateOrderStatusRequest;
import br.com.rafael.brindesmanager.dto.response.CustomerOrderResponse;
import br.com.rafael.brindesmanager.dto.response.OrderItemResponse;
import br.com.rafael.brindesmanager.entity.*;
import br.com.rafael.brindesmanager.exception.NotFoundException;
import br.com.rafael.brindesmanager.repository.CustomerOrderRepository;
import br.com.rafael.brindesmanager.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final CurrentUserService currentUserService;
    private final CustomerService customerService;
    private final ProductService productService;

    @Transactional
    public CustomerOrderResponse create(CustomerOrderRequest request) {
        AppUser user = currentUserService.getCurrentUser();
        Customer customer = customerService.findActiveCustomerByIdAndCurrentUser(request.customerId());

        CustomerOrder order = CustomerOrder.builder()
                .code(generateOrderCode())
                .customer(customer)
                .user(user)
                .notes(request.notes())
                .totalAmount(BigDecimal.ZERO)
                .build();

        return getCustomerOrderResponse(request, order);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> findAll() {
        Long userId = currentUserService.getCurrentUserId();

        return customerOrderRepository.findAllByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerOrderResponse findById(Long id) {
        CustomerOrder order = findActiveOrderByIdAndCurrentUser(id);
        return toResponse(order);
    }

    @Transactional
    public CustomerOrderResponse update(Long id, CustomerOrderRequest request) {
        CustomerOrder order = findActiveOrderByIdAndCurrentUser(id);
        Customer customer = customerService.findActiveCustomerByIdAndCurrentUser(request.customerId());

        order.setCustomer(customer);
        order.setNotes(request.notes());
        order.setUpdatedAt(LocalDateTime.now());

        order.clearItems();

        return getCustomerOrderResponse(request, order);
    }

    private CustomerOrderResponse getCustomerOrderResponse(CustomerOrderRequest request, CustomerOrder order) {
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productService.findActiveProductById(itemRequest.productId());

            BigDecimal itemTotal = calculateItemTotal(itemRequest.quantity(), itemRequest.unitPrice());

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .totalPrice(itemTotal)
                    .customDescription(itemRequest.customDescription())
                    .build();

            order.addItem(item);
        }

        order.setTotalAmount(calculateOrderTotal(order.getItems()));

        CustomerOrder updatedOrder = customerOrderRepository.save(order);

        return toResponse(updatedOrder);
    }

    @Transactional
    public CustomerOrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        CustomerOrder order = findActiveOrderByIdAndCurrentUser(id);

        order.setStatus(request.status());
        order.setUpdatedAt(LocalDateTime.now());

        CustomerOrder updatedOrder = customerOrderRepository.save(order);

        return toResponse(updatedOrder);
    }

    @Transactional
    public void delete(Long id) {
        CustomerOrder order = findActiveOrderByIdAndCurrentUser(id);

        order.setActive(false);
        order.setUpdatedAt(LocalDateTime.now());

        customerOrderRepository.save(order);
    }

    public CustomerOrder findActiveOrderByIdAndCurrentUser(Long id) {
        Long userId = currentUserService.getCurrentUserId();

        return customerOrderRepository.findByIdAndUserIdAndActiveTrue(id, userId)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
    }

    private BigDecimal calculateItemTotal(Integer quantity, BigDecimal unitPrice) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private BigDecimal calculateOrderTotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateOrderCode() {
        return "PED-" + System.currentTimeMillis();
    }

    private CustomerOrderResponse toResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new CustomerOrderResponse(
                order.getId(),
                order.getCode(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getUser().getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getNotes(),
                order.getActive(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getReference(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getCustomDescription()
        );
    }
}