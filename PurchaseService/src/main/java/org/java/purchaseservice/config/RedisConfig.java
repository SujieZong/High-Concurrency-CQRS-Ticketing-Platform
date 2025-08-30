package org.java.purchaseservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// serialize key when saving to Redis Key
		template.setKeySerializer(new StringRedisSerializer());

		// turn value into JSON to save as value
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		return template;
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
		return new StringRedisTemplate(cf);
	}

	@Bean
	public RedisTemplate<String, byte[]> bitmapRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, byte[]> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// serialize key when saving to Redis Key
		StringRedisSerializer strSer = new StringRedisSerializer();
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(strSer);

		// value and hash value use byte instead of change to JSON
		RedisSerializer<byte[]> bytesSer = RedisSerializer.byteArray();
		template.setValueSerializer(bytesSer);
		template.setHashValueSerializer(bytesSer);

		template.afterPropertiesSet();
		return template;
	}
}
