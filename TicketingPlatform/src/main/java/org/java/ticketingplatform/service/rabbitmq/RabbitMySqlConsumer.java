package org.java.ticketingplatform.service.rabbitmq;//package org.java.ticketingplatform.service.rabbitmq;
//
//import com.rabbitmq.client.Channel;
//import lombok.extern.slf4j.Slf4j;
//import org.java.ticketingplatform.config.rabbitmq.RabbitFactory;
//import org.java.ticketingplatform.dto.MqDTO;
//import org.java.ticketingplatform.mapper.MqMapper;
//import org.java.ticketingplatform.model.TicketInfo;
//import org.java.ticketingplatform.repository.MySqlTicketDAOInterface;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//
//@Slf4j
//@Service
//public class RabbitMySqlConsumer {
//	private final MySqlTicketDAOInterface mySqlTicketDAO;
//	private final MqMapper mqMapper;
//
//	public RabbitMySqlConsumer(MySqlTicketDAOInterface mySqlTicketDAO, MqMapper mqMapper) {
//		this.mySqlTicketDAO = mySqlTicketDAO;
//		this.mqMapper = mqMapper;
//	}
//
//	@RabbitListener(queues = RabbitFactory.TICKET_SQL, containerFactory = "manualAckFactory")
//	public void noSqlConsume(MqDTO dto, Channel channel,
//	                         @Header(AmqpHeaders.DELIVERY_TAG) long tag,
//	                         @Header(AmqpHeaders.REDELIVERED) boolean redelivered
//	) throws IOException {
//		log.trace("【MySqlMQ】 redelivered={}, dto={}", redelivered, dto);
//		TicketInfo ticket = mqMapper.toTicketInfo(dto);
//
//		try {
//			mySqlTicketDAO.createTicket(ticket);
//			channel.basicAck(tag, false);
//			log.trace("【MySqlMQ】write Success，ticketId={}", ticket.getTicketId());
//		} catch (Exception ex) {
//			log.error("【MySqlMQ】write Failed, back to queue! ticketId={}，Reason: {}", ticket.getTicketId(), ex.getMessage(), ex);
//			channel.basicNack(tag, false, !redelivered);
//		}
//	}
//}
