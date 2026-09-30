package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);

    Optional<Category> findByIdAndCompanyIdAndActiveTrue(Long id, Long companyId);

    List<Category> findAllByCompanyIdAndActiveTrueOrderByNameAsc(Long companyId);

    List<Category> findAllByCompanyIdOrderByNameAsc(Long companyId);

    Optional<Category> findByIdAndCompanyId(Long id, Long companyId);
}