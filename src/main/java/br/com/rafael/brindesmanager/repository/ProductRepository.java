package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByReferenceIgnoreCase(String reference);

    Optional<Product> findByIdAndActiveTrue(Long id);

    List<Product> findAllByActiveTrueOrderByNameAsc();

    List<Product> findAllByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId);
}