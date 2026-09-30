package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findAllByCompanyIdAndActiveTrueOrderByCreatedAtDesc(Long companyId);

    Optional<CustomerOrder> findByIdAndCompanyIdAndActiveTrue(
            Long id,
            Long companyId
    );
}