package org.java.purchaseservice;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRabbit
@SpringBootApplication
@EnableScheduling
public class PurchaseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PurchaseServiceApplication.class, args);
	}

}

