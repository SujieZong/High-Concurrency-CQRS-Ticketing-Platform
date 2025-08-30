package org.java.purchaseservice.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
@Entity @Table(name = "outbox_events")
public class OutboxEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64)
	private String eventType;

	@Lob
	@Column(nullable = false) // 若上面用 JSON，这里也可以是 String 字段
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


