package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByUserIdAndActiveTrueOrderByNameAsc(Long userId);

    Optional<Customer> findByIdAndUserIdAndActiveTrue(Long id, Long userId);

    List<Customer> findAllByUserIdOrderByNameAsc(Long userId);

}