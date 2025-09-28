package org.java.rabbitcombinedconsumer.service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.rabbitcombinedconsumer.dto.MqDTO;
import org.java.rabbitcombinedconsumer.mapper.MqMapper;
import org.java.rabbitcombinedconsumer.repository.MySqlTicketDAOInterface;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaMySqlConsumer {

	private final MySqlTicketDAOInterface mySqlTicketDAO;
	private final MqMapper mqMapper;

	/**
	 * Kafka implementation, FP instead od OOP
	 */
	@Bean
	public Consumer<Message<MqDTO>> ticket() {
		return message -> {
			try {
				MqDTO dto = message.getPayload();
				String partition = message.getHeaders().get("kafka_receivedPartitionId", String.class);
				String offset = message.getHeaders().get("kafka_offset", String.class);

				log.info("【KafkaMQ】Received message: ticketId={}, partition={}, offset={}",
						dto.getTicketId(), partition, offset);

				// Write to MySQL calling mysqlTicketDao
				mySqlTicketDAO.createTicket(mqMapper.toTicketInfo(dto));

				log.debug("【KafkaMQ】Successfully processed ticketId={}", dto.getTicketId());

			} catch (Exception ex) {
				log.error("【KafkaMQ】Processing failed: {}", ex.getMessage(), ex);
				throw ex; // throw to retry, should set a retry limit
			}
		};
	}
}