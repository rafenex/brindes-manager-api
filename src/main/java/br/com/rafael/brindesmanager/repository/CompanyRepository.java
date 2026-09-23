package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}