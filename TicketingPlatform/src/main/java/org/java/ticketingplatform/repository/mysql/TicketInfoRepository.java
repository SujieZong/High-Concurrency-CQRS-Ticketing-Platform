package org.java.ticketingplatform.repository.mysql;

import org.java.ticketingplatform.model.TicketInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface TicketInfoRepository extends JpaRepository<TicketInfo, String> {

	int countByEventId(String eventId);

	@Query("""
            SELECT SUM(z.ticketPrice)
            FROM TicketInfo t JOIN Zone z ON t.zoneId = z.zoneId
            WHERE t.venueId = :venueId AND t.eventId = :eventId
            """)
	BigDecimal sumRevenueByVenueAndEvent(@Param("venueId") String venueId, @Param("eventId") String eventId);

}