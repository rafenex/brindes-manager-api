package br.com.rafael.brindesmanager.repository;

import br.com.rafael.brindesmanager.entity.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // Spring Data uses UPPER for these IgnoreCase comparisons, matching the V13 unique index.
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = "company")
    Optional<AppUser> findByEmailIgnoreCaseAndActiveTrue(String email);

    @EntityGraph(attributePaths = "company")
    Optional<AppUser> findByIdAndActiveTrue(Long id);

    List<AppUser> findAllByCompanyIdAndDeletedAtIsNullOrderByNameAsc(Long companyId);

    Optional<AppUser> findByIdAndCompanyIdAndDeletedAtIsNull(Long id, Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from AppUser u
            where u.company.id = :companyId and u.id in (:actorId, :targetId)
            order by u.id
            """)
    List<AppUser> lockForManagement(@Param("companyId") Long companyId,
                                    @Param("actorId") Long actorId,
                                    @Param("targetId") Long targetId);
}
