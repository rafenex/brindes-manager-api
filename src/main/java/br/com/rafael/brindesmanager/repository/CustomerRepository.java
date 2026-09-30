package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByCompanyIdAndActiveTrueOrderByNameAsc(Long companyId);

    Optional<Customer> findByIdAndCompanyIdAndActiveTrue(
            Long id,
            Long companyId
    );


    Optional<Customer> findByIdAndCompanyId(
            Long id,
            Long companyId
    );

    List<Customer> findAllByCompanyIdOrderByCompanyNameAsc(Long companyId);
}