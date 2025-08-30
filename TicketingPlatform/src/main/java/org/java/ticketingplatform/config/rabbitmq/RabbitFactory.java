package org.java.ticketingplatform.config.rabbitmq;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitFactory {

	public static final String TICKET_EXCHANGE = "ticket.exchange";
	public static final String TICKET_NOSQL = "ticketNoSQL";
	public static final String TICKET_SQL = "ticketSQL";

	//	NoSQL queue
	@Bean
	public Queue ticketNosqlQueue() {
		return new Queue(TICKET_NOSQL, true);
	}

	//SQL queue
	@Bean
	public Queue ticketSqlQueue() {
		return new Queue(TICKET_SQL, true);
	}

	// put the message into TicketExchange
	@Bean
	public TopicExchange ticketExchange() {
		return new TopicExchange(TICKET_EXCHANGE, true, false);
	}

}
