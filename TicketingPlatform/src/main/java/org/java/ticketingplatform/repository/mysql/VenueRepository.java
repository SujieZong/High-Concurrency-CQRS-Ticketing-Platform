package org.java.ticketingplatform.repository.mysql;

import org.java.ticketingplatform.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueRepository extends JpaRepository<Venue, String> {
}