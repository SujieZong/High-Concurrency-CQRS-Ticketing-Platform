package org.java.purchaseservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Data
@Configuration
@ConfigurationProperties(prefix = "aws.dynamodb")
public class DynamoDbConfig {

	private String endPoint;
	private String region;
	private String accessKey;
	private String accessPass;

	@Bean
	public DynamoDbClient dynamoDbClient() {

		System.out.println("=== DynamoDB Configuration ===");
		System.out.println("endPoint: " + endPoint);
		System.out.println("region: " + region);
		System.out.println("==============================");


		if (region == null || region.isEmpty()) {
			System.out.println("WARNING: region is null or empty!");
		}


		return DynamoDbClient.builder()
				.endpointOverride(URI.create(endPoint))
				.region(Region.of(region))
				.credentialsProvider(
						StaticCredentialsProvider.create(
								AwsBasicCredentials.create(accessKey, accessPass)
						)
				)
				.httpClientBuilder(
						ApacheHttpClient.builder()
								.maxConnections(3000)
								.connectionTimeout(java.time.Duration.ofSeconds(10))
								.socketTimeout(java.time.Duration.ofSeconds(30))
				)
				.build();
	}
}

