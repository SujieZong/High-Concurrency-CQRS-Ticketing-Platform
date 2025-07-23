package org.java.ticketingplatform.service.rabbitmq;//package org.java.ticketingplatform.service.rabbitmq;
//
//import com.rabbitmq.client.Channel;
//import lombok.extern.slf4j.Slf4j;
//import org.java.ticketingplatform.config.rabbitmq.RabbitFactory;
//import org.java.ticketingplatform.dto.MqDTO;
//import org.java.ticketingplatform.mapper.MqMapper;
//import org.java.ticketingplatform.model.TicketCreation;
//import org.java.ticketingplatform.repository.DynamoTicketDAOInterface;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//
//@Slf4j
//@Service
//public class RabbitNoSqlConsumer {
//	private final DynamoTicketDAOInterface dynamoDao;
//	private final MqMapper mqMapper;
//
//	@Autowired
//	public RabbitNoSqlConsumer(DynamoTicketDAOInterface dynamoDao, MqMapper mqMapper) {
//		this.dynamoDao = dynamoDao;
//		this.mqMapper = mqMapper;
//	}
//
//	@RabbitListener(queues = RabbitFactory.TICKET_NOSQL, containerFactory = "manualAckFactory")
//	public void mySqlConsume(MqDTO dto, Channel channel,
//	                         @Header(AmqpHeaders.DELIVERY_TAG) long tag,
//	                         @Header(AmqpHeaders.REDELIVERED) boolean redelivered
//	) throws IOException {
//		log.trace("【NoSqlMQ】 redelivered={}, dto={}", redelivered, dto);
//		TicketCreation ticket = mqMapper.toTicketCreation(dto);
//
//		try {
//			dynamoDao.createTicket(ticket);
//			channel.basicAck(tag, false);
//			log.trace("【NoSqlMQ】write Success, id={}", ticket.getId());
//		} catch (Exception ex) {
//			log.error("【NoSqlMQ】write Failed, back to queue! ticketId={}，Cause: {}", ticket.getId(), ex.getMessage());
//			channel.basicNack(tag, false, !redelivered);
//		}
//	}
//}
