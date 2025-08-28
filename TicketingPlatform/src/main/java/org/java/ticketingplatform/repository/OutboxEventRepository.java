package org.java.ticketingplatform.repository;

import org.java.ticketingplatform.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
	List<OutboxEvent> findTop50BySentFalseOrderByIdAsc();
}
