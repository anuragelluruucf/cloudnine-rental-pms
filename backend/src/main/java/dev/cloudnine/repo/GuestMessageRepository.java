package dev.cloudnine.repo;

import dev.cloudnine.domain.GuestMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GuestMessageRepository extends JpaRepository<GuestMessage, UUID> {

    @Query("""
            select m from GuestMessage m
            join fetch m.property
            where m.property.id = :propertyId
            order by m.createdAt desc
            """)
    List<GuestMessage> findForProperty(@Param("propertyId") UUID propertyId);

    @Query("""
            select m from GuestMessage m
            join fetch m.property p
            where p.id in :propertyIds
            order by m.createdAt desc
            """)
    List<GuestMessage> findForProperties(@Param("propertyIds") List<UUID> propertyIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GuestMessage m where m.property.id = :propertyId")
    void deleteByPropertyId(@Param("propertyId") UUID propertyId);
}
