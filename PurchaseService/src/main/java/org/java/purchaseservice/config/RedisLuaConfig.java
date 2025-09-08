package org.java.purchaseservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class RedisLuaConfig {

	/*
	 * Try to Occupy seats through Lua Script
	 */
	@Bean(name = "tryOccupySeatScript")
	public DefaultRedisScript<Long> tryOccupySeatScript() {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		ClassPathResource res = new ClassPathResource("lua/occupySeat.lua");

		try {
			String lua = Files.readString(Paths.get(res.getURI()), StandardCharsets.UTF_8);
			log.trace("[RedisLuaConfig]Loaded Lua script for tryOccupySeat:\n{}", lua);
			script.setScriptText(lua);
		} catch (Exception e) {
			log.error("Failed to load occupySeat.lua from classpath", e);
			throw new IllegalStateException("Cannot load Lua script", e);
		}

		script.setResultType(Long.class);
		return script;
	}

	@Bean(name = "tryReleaseSeatScript")
	public DefaultRedisScript<Long> tryReleaseSeatScript() {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		ClassPathResource res = new ClassPathResource("lua/releaseSeat.lua");

		try {
			String lua = Files.readString(Paths.get(res.getURI()));
			log.trace("[RedisLuaConfig]Loaded Lua script for tryReleaseSeat:\n{}", lua);
			script.setScriptText(lua);
		} catch (Exception e) {
			log.error("Failed to load releaseSeat.lua from classpath", e);
			throw new IllegalStateException("Cannot load Lua script", e);
		}

		script.setResultType(Long.class);
		return script;
	}
}