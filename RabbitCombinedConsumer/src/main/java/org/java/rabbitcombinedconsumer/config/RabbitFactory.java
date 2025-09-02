package org.java.rabbitcombinedconsumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitFactory {

	public static final String TICKET_EXCHANGE = "ticket.exchange";
	public static final String ROUTING_TICKET_CREATED = "ticket.created";
	public static final String TICKET_SQL = "ticketSQL";

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

	@Bean
	public Binding bindingSql(Queue ticketSqlQueue, TopicExchange ticketExchange) {
		return BindingBuilder.bind(ticketSqlQueue).to(ticketExchange).with(ROUTING_TICKET_CREATED);
	}

	@Bean
	public MessageConverter jackson2Converter(ObjectMapper om) {
		var c = new Jackson2JsonMessageConverter(om);
		c.setCreateMessageIds(true);
		return c;
	}

	@Bean(name = "manualAckContainerFactory")
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
			ConnectionFactory connectionFactory,
			MessageConverter messageConverter
	) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(messageConverter);

		factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		factory.setDefaultRequeueRejected(true);

		factory.setConcurrentConsumers(20);
		factory.setMaxConcurrentConsumers(50);
		factory.setPrefetchCount(100);

		return factory;
	}
}

