package org.java.queryservice.repository.mysql;

import org.java.queryservice.model.TicketInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface TicketInfoRepository extends JpaRepository<TicketInfo, String> {

	int countByEventId(String eventId);

	@Query(value = """
			select coalesce(sum(z.ticket_price), 0)
			from ticket t
					join zone z on z.zone_id = t.zone_id
			where t.venue_id = :venueId and t.event_id = :eventId
			""", nativeQuery = true)
	BigDecimal sumRevenueByVenueAndEvent(@Param("venueId") String venueId, @Param("eventId") String eventId);
}