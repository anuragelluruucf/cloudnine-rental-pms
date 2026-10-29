package dev.cloudnine.repo;

import dev.cloudnine.domain.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    @Query("select c from KnowledgeChunk c join fetch c.property")
    List<KnowledgeChunk> findAllWithProperty();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from KnowledgeChunk c where c.property.id = :propertyId")
    void deleteByPropertyId(@Param("propertyId") UUID propertyId);
}
