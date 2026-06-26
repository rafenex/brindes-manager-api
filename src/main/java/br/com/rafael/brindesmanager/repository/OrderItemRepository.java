package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}