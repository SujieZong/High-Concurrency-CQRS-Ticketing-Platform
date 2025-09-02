package org.java.purchaseservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Slf4j
@Configuration
public class RedisLuaConfig {

	/*
	 * Try to Occupy seats through Lua Script
	 */
	@Bean("tryOccupySeatScript")
	public DefaultRedisScript<Long> tryOccupySeatScript() {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setLocation(new ClassPathResource("lua/occupySeat.lua"));
		script.setResultType(Long.class);
		return script;
	}

	@Bean("tryReleaseSeatScript")
	public DefaultRedisScript<Long> tryReleaseSeatScript() {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setLocation(new ClassPathResource("lua/releaseSeat.lua"));
		script.setResultType(Long.class);
		return script;
	}
}