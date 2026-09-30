package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCompanyIdAndReferenceIgnoreCase(Long companyId, String reference);

    Optional<Product> findByIdAndCompanyIdAndActiveTrue(Long id, Long companyId);

    List<Product> findAllByCompanyIdAndActiveTrueOrderByNameAsc(Long companyId);

    List<Product> findAllByCompanyIdAndCategoryIdAndActiveTrueOrderByNameAsc(
            Long companyId,
            Long categoryId
    );

    List<Product> findAllByCompanyIdOrderByNameAsc(Long companyId);

    Optional<Product> findByIdAndCompanyId(Long id, Long companyId);
}