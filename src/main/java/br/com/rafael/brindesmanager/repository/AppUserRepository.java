package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByEmailIgnoreCaseAndActiveTrue(String email);

    Optional<AppUser> findByIdAndActiveTrue(Long id);
}