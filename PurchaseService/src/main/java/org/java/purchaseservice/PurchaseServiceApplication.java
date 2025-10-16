package org.java.purchaseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.java.purchaseservice.config.EventConfig;
import org.java.purchaseservice.config.VenueConfig;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({VenueConfig.class, EventConfig.class})
public class PurchaseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PurchaseServiceApplication.class, args);
	}

}

