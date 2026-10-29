package dev.stayledger.repo;

import dev.stayledger.domain.Reservation;
import dev.stayledger.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("""
            select count(r) from Reservation r
            where r.property.id = :propertyId
              and r.status in :statuses
              and r.checkIn < :checkOut
              and r.checkOut > :checkIn
            """)
    long countOverlap(
            @Param("propertyId") UUID propertyId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    @Query("""
            select count(r) from Reservation r
            where r.property.id = :propertyId
              and r.status in :statuses
              and r.id <> :excludeId
              and r.checkIn < :checkOut
              and r.checkOut > :checkIn
            """)
    long countOverlapExcluding(
            @Param("propertyId") UUID propertyId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeId") UUID excludeId
    );

    Optional<Reservation> findByChannelSourceIdAndExternalUid(UUID channelSourceId, String externalUid);

    @Query("""
            select r from Reservation r
            where r.channelSource.id = :channelSourceId
              and r.checkIn = :checkIn
              and r.checkOut = :checkOut
            """)
    List<Reservation> findByChannelSourceAndDates(
            @Param("channelSourceId") UUID channelSourceId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    @Query("""
            select r from Reservation r
            where r.property.id = :propertyId
              and r.checkIn < :rangeEnd
              and r.checkOut > :rangeStart
            order by r.checkIn asc
            """)
    List<Reservation> findOverlappingRange(
            @Param("propertyId") UUID propertyId,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd
    );

    List<Reservation> findByPropertyIdOrderByCheckInAsc(UUID propertyId);

    List<Reservation> findByPropertyIdAndStatusIn(UUID propertyId, Collection<ReservationStatus> statuses);

    @Query("""
            select r from Reservation r
            join fetch r.property p
            where p.id in :propertyIds
              and r.status in :statuses
              and r.checkIn < :rangeEnd
              and r.checkOut > :rangeStart
            order by r.checkIn asc
            """)
    List<Reservation> findForPropertiesOverlapping(
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd
    );

    long countByPropertyId(UUID propertyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Reservation r where r.property.id = :propertyId")
    void deleteByPropertyId(@Param("propertyId") UUID propertyId);
}
