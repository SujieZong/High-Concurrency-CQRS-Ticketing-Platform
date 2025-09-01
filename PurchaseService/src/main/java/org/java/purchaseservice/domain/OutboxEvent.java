package org.java.purchaseservice.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
@Entity
@Table(
		name = "outbox_events",
		indexes = {
				@Index(name = "idx_outbox_unsent", columnList = "sent, next_attempt_at"),
				@Index(name = "idx_outbox_created", columnList = "created_at")
		}
)
public class OutboxEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64)
	private String eventType;

	@Lob
	@Column(nullable = false)
	private String payload;

	@Column(nullable = false)
	private boolean sent = false;

	@Column(nullable = false)
	private int attempts = 0;

	private java.sql.Timestamp nextAttemptAt;

	@org.hibernate.annotations.CreationTimestamp
	private java.sql.Timestamp createdAt;

	@org.hibernate.annotations.UpdateTimestamp
	private java.sql.Timestamp updatedAt;
}


