package org.java.ticketingplatform.repository.mysql;

import org.java.ticketingplatform.model.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, String> {
}