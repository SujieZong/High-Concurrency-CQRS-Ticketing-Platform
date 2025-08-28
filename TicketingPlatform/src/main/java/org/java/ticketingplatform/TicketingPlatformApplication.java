package org.java.ticketingplatform;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRabbit
@SpringBootApplication
@EnableScheduling
public class TicketingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketingPlatformApplication.class, args);
	}

}
