package com.ozin.infrastructure.booking.adapter.out.persistence;

import com.ozin.application.booking.AppointmentView;
import com.ozin.domain.booking.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, UUID> {

    String VIEW = """
            select new com.ozin.application.booking.AppointmentView(
                a.id, a.customerId, c.fullName, a.stylistId, s.fullName, a.serviceId, sv.name,
                a.branchId, a.startsAt, a.endsAt, a.price, a.currency, a.status)
            from AppointmentJpaEntity a
            join com.ozin.infrastructure.customer.adapter.out.persistence.CustomerJpaEntity c on c.id = a.customerId
            join com.ozin.infrastructure.staff.adapter.out.persistence.StylistJpaEntity s on s.id = a.stylistId
            join com.ozin.infrastructure.catalog.adapter.out.persistence.SalonServiceJpaEntity sv on sv.id = a.serviceId
            """;

    @Query("""
            select a from AppointmentJpaEntity a
            where a.stylistId = :stylistId and a.status in :statuses
              and a.startsAt < :to and a.endsAt > :from
            """)
    List<AppointmentJpaEntity> findByStylistOverlapping(@Param("stylistId") UUID stylistId,
                                                        @Param("statuses") Collection<AppointmentStatus> statuses,
                                                        @Param("from") Instant from,
                                                        @Param("to") Instant to);

    long countByCustomerIdAndStatusInAndStartsAtAfter(UUID customerId,
                                                      Collection<AppointmentStatus> statuses,
                                                      Instant startsAt);

    @Query(value = VIEW + " where a.customerId = :customerId order by a.startsAt desc",
            countQuery = "select count(a) from AppointmentJpaEntity a where a.customerId = :customerId")
    Page<AppointmentView> findViewsByCustomer(@Param("customerId") UUID customerId, Pageable pageable);

    @Query(VIEW + " where a.stylistId = :stylistId and a.startsAt < :to and a.endsAt > :from order by a.startsAt")
    List<AppointmentView> findViewsByStylist(@Param("stylistId") UUID stylistId,
                                             @Param("from") Instant from,
                                             @Param("to") Instant to);

    @Query(VIEW + " where a.branchId = :branchId and a.startsAt < :to and a.endsAt > :from order by a.startsAt")
    List<AppointmentView> findViewsByBranch(@Param("branchId") UUID branchId,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);
}
