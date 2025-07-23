package org.java.rabbitcombinedconsumer.service.rabbitmq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.java.rabbitcombinedconsumer.config.RabbitFactory;
import org.java.rabbitcombinedconsumer.dto.MqDTO;
import org.java.rabbitcombinedconsumer.mapper.MqMapper;
import org.java.rabbitcombinedconsumer.model.TicketInfo;
import org.java.rabbitcombinedconsumer.repository.MySqlTicketDAOInterface;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class RabbitMySqlConsumer {
	private final MySqlTicketDAOInterface mySqlTicketDAO;
	private final MqMapper mqMapper;

	public RabbitMySqlConsumer(MySqlTicketDAOInterface mySqlTicketDAO, MqMapper mqMapper) {
		this.mySqlTicketDAO = mySqlTicketDAO;
		this.mqMapper = mqMapper;
	}

	@RabbitListener(queues = RabbitFactory.TICKET_SQL, containerFactory = "rabbitListenerContainerFactory")
	public void noSqlConsume(MqDTO dto, Channel channel,
	                         @Header(AmqpHeaders.DELIVERY_TAG) long tag,
	                         @Header(AmqpHeaders.REDELIVERED) boolean redelivered
	) throws IOException {
		log.info("【MySqlMQ】Received message, redelivered={}, ticketId={}", redelivered, dto.getTicketId());
		TicketInfo ticket = mqMapper.toTicketInfo(dto);
		log.debug("【MySqlMQ】Mapped to TicketInfo: {}", ticket);

		try {
			mySqlTicketDAO.createTicket(ticket);
			log.info("【MySqlMQ】MySQL write success: ticketId={}", ticket.getTicketId());
			channel.basicAck(tag, false);
		} catch (Exception ex) {
			log.error("【MySqlMQ】MySQL write failed, requeue={}, ticketId={}, error={}", !redelivered, ticket.getTicketId(), ex.getMessage());
			channel.basicNack(tag, false, !redelivered);
		}
	}
}
