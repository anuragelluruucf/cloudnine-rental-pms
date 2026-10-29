package dev.stayledger.repo;

import dev.stayledger.domain.ChannelSource;
import dev.stayledger.domain.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelSourceRepository extends JpaRepository<ChannelSource, UUID> {

    List<ChannelSource> findByPropertyIdOrderByChannelAsc(UUID propertyId);

    Optional<ChannelSource> findByPropertyIdAndChannel(UUID propertyId, ChannelType channel);

    List<ChannelSource> findByEnabledTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ChannelSource c where c.property.id = :propertyId")
    void deleteByPropertyId(@Param("propertyId") UUID propertyId);
}
