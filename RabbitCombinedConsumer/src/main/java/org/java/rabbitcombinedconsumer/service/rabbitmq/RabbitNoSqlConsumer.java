//package org.java.rabbitcombinedconsumer.service.rabbitmq;
//
//import com.rabbitmq.client.Channel;
//import lombok.extern.slf4j.Slf4j;
//import org.java.rabbitcombinedconsumer.config.RabbitFactory;
//import org.java.rabbitcombinedconsumer.dto.MqDTO;
//import org.java.rabbitcombinedconsumer.mapper.MqMapper;
//import org.java.rabbitcombinedconsumer.model.TicketCreation;
//import org.java.rabbitcombinedconsumer.repository.DynamoTicketDAOInterface;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Service;
//import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
//import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
//import software.amazon.awssdk.services.dynamodb.model.PutRequest;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Service
//public class RabbitNoSqlConsumer {
//	private static final int BATCH_SIZE = 100;
//	private final DynamoTicketDAOInterface dynamoDao;
//	private final MqMapper mqMapper;
//
//
//	private final List<WriteRequest> buffer = new ArrayList<>(BATCH_SIZE);
//
//	@Autowired
//	public RabbitNoSqlConsumer(DynamoTicketDAOInterface dynamoDao, MqMapper mqMapper) {
//		this.dynamoDao = dynamoDao;
//		this.mqMapper = mqMapper;
//	}
//
//	@RabbitListener(queues = RabbitFactory.TICKET_NOSQL, containerFactory = "rabbitListenerContainerFactory")
//	public void mySqlConsume(MqDTO dto, Channel channel,
//	                         @Header(AmqpHeaders.DELIVERY_TAG) long tag,
//	                         @Header(AmqpHeaders.REDELIVERED) boolean redelivered
//	) throws IOException {
//		log.info("【NoSqlMQ】Received message, redelivered={}, ticketId={}", redelivered, dto.getTicketId());
//		TicketCreation ticket = mqMapper.toTicketCreation(dto);
//		log.debug("【NoSqlMQ】Mapped to TicketCreation: {}", ticket);
//
//		try {
//			dynamoDao.createTicket(ticket);
//			channel.basicAck(tag, false);
//			log.info("【NoSqlMQ】DynamoDB write success: id={}", ticket.getId());
//		} catch (Exception ex) {
//			log.error("【NoSqlMQ】DynamoDB write failed, requeue={}, ticketId={}, error={}", !redelivered, ticket.getId(), ex.getMessage());
//			channel.basicNack(tag, false, !redelivered);
//		}
//	}
//}
