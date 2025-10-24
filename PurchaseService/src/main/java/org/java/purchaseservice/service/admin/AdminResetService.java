package org.java.purchaseservice.service.admin;

import com.mysql.cj.jdbc.MysqlDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.service.initialize.EventConfigService;
import org.java.purchaseservice.service.initialize.VenueConfigService;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Maintenance operations exposed through the admin REST API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminResetService {

	private final VenueConfigService venueConfigService;
	private final EventConfigService eventConfigService;
	private final StringRedisTemplate stringRedisTemplate;
	private final Environment environment;
	private final DataSource dataSource;

	public void resetRedis() {
		log.info("[AdminReset] Redis reset requested");

		try {
			long clearedEvents = scanDelete("event:*", 500);
			long clearedTickets = scanDelete("ticket:*", 500);
			log.info("[AdminReset] Deleted Redis keys event:*={} ticket:*={}", clearedEvents, clearedTickets);

			venueConfigService.initializeVenues();
			eventConfigService.initializeEvents();
			log.info("[AdminReset] Redis structures rebuilt successfully");
		} catch (RuntimeException ex) {
			log.error("[AdminReset] Redis reset failed: {}", ex.getMessage(), ex);
			throw ex;
		}
	}

	private long scanDelete(String pattern, long batchSize) {
		Long removed = stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
			long deleted = 0;
			Cursor<byte[]> cursor = null;
			try {
				cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(batchSize).build());
				while (cursor.hasNext()) {
					byte[] key = cursor.next();
					connection.keyCommands().del(key);
					deleted++;
				}
			} catch (Exception e) {
				log.error("[AdminReset] scanDelete failed for pattern {}: {}", pattern, e.getMessage(), e);
			} finally {
				if (cursor != null) {
					try {
						cursor.close();
					} catch (Exception ignored) {
					}
				}
			}
			return deleted;
		});
		return removed == null ? 0 : removed;
	}

	public void resetMySql() {
		log.info("[AdminReset] MySQL reset requested");
		DataSource effectiveDataSource = resolveDataSourceOverride();

		try (Connection connection = effectiveDataSource.getConnection();
				Statement statement = connection.createStatement()) {

			statement.execute("SET FOREIGN_KEY_CHECKS=0");
			statement.execute("TRUNCATE TABLE ticket");
			statement.execute("SET FOREIGN_KEY_CHECKS=1");

			log.info("[AdminReset] MySQL ticket table truncated successfully");
		} catch (SQLException ex) {
			log.error("[AdminReset] MySQL reset failed: {}", ex.getMessage(), ex);
			throw new DataAccessException("Failed to truncate ticket table", ex) {
			};
		}
	}

	private DataSource resolveDataSourceOverride() {
		String overrideUrl = environment.getProperty("admin.reset.mysql.url");
		if (!StringUtils.hasText(overrideUrl)) {
			return dataSource;
		}

		log.info("[AdminReset] Using override JDBC URL for reset operation");

		MysqlDataSource override = new MysqlDataSource();
		override.setUrl(overrideUrl);

		String overrideUsername = environment.getProperty("admin.reset.mysql.username");
		String overridePassword = environment.getProperty("admin.reset.mysql.password");

		if (StringUtils.hasText(overrideUsername)) {
			override.setUser(overrideUsername);
		}
		if (overridePassword != null) {
			override.setPassword(overridePassword);
		}

		return override;
	}
}
