package org.java.purchaseservice.repository;

import org.java.purchaseservice.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
	List<OutboxEvent> findTop50BySentFalseOrderByIdAsc();
}
