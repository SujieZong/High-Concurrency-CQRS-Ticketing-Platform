package org.java.ticketingplatform.service.rabbitmq;

import org.java.ticketingplatform.config.rabbitmq.RabbitFactory;
import org.java.ticketingplatform.dto.MqDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitProducer {
	private final RabbitTemplate rabbitTemplate;

	@Autowired
	public RabbitProducer(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	/**
	 * The template will convert the Message to JSON and send to Rabbit MQ Server through exchanger
	 */
	public void sendTicketCreated(MqDTO event) {
		rabbitTemplate.convertAndSend(
				RabbitFactory.TICKET_EXCHANGE,
				"ticket.created",
				event
		);
	}
}
