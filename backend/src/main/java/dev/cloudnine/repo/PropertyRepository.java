package dev.cloudnine.repo;

import dev.cloudnine.domain.Property;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    List<Property> findByOwnerIdOrderByNameAsc(UUID ownerId);

    Optional<Property> findByExportToken(UUID exportToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "20000"))
    @Query("select p from Property p where p.id = :id")
    Optional<Property> findByIdForUpdate(@Param("id") UUID id);
}
