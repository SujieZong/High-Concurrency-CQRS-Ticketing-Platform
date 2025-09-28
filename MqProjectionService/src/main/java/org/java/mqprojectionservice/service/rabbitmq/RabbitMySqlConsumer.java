//package org.java.mqprojectionservice.service.rabbitmq;
//
//import com.rabbitmq.client.Channel;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.java.mqprojectionservice.config.RabbitFactory;
//import org.java.mqprojectionservice.dto.MqDTO;
//import org.java.mqprojectionservice.mapper.MqMapper;
//import org.java.mqprojectionservice.repository.MySqlTicketDAOInterface;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class RabbitMySqlConsumer {
//	private final MySqlTicketDAOInterface mySqlTicketDAO;
//	private final MqMapper mqMapper;
//
//	@RabbitListener(queues = RabbitFactory.TICKET_SQL, containerFactory = "manualAckContainerFactory")
//	public void mySqlConsume(MqDTO dto, Channel channel,
//	                         @Header(AmqpHeaders.DELIVERY_TAG) long tag,
//	                         @Header(AmqpHeaders.REDELIVERED) boolean redelivered
//	) throws IOException {
//		log.info("【MySqlMQ】Received, redelivered={}, ticketId={}", redelivered, dto.getTicketId());
//		try {
//			mySqlTicketDAO.createTicket(mqMapper.toTicketInfo(dto));
//			channel.basicAck(tag, false);
//		} catch (Exception ex) {
//			log.error("【MySqlMQ】MySQL write failed, requeue={}, ticketId={}, err={}", !redelivered, dto.getTicketId(), ex.getMessage());
//			channel.basicNack(tag, false, !redelivered);
//		}
//	}
//}