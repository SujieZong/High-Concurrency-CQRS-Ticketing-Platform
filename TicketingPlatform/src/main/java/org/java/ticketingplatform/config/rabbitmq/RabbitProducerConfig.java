package org.java.ticketingplatform.config.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitProducerConfig {

	@Bean
	public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
		return new Jackson2JsonMessageConverter(objectMapper);
	}

	@Bean
	public RabbitTemplate rabbitTemplate(
			ConnectionFactory connectionFactory,
			MessageConverter messageConverter
	) {
		RabbitTemplate tpl = new RabbitTemplate(connectionFactory);
		tpl.setMessageConverter(messageConverter);
		return tpl;
	}
}
