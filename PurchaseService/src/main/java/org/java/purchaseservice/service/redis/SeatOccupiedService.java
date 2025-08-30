package org.java.purchaseservice.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.service.initialize.VenueConfigService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatOccupiedService {
	private final RedisTemplate<String, byte[]> bitmapRedisTemplate;
	private final StringRedisTemplate stringRedisTemplate;
	private final VenueConfigService venueConfigService;

	public void initializeAllZonesForEvent(String eventId, String venueId) {
		Set<Object> zoneIds = venueConfigService.getVenueZones(venueId);
		if (zoneIds == null || zoneIds.isEmpty()) {
			throw new IllegalStateException("Venue " + venueId + " has no configured zones.");
		}

		for (Object z : zoneIds) {
			int zoneId = Integer.parseInt(z.toString());
			initializeEventSeat(eventId, venueId, zoneId);
		}
	}

	public void initializeEventSeat(String eventId, String venueId, int zoneId) {
		String bitmapKey = RedisKeyUtil.getZoneBitMapKey(eventId, zoneId);
		String zoneRemKey = RedisKeyUtil.getZoneRemainedSeats(eventId, zoneId);
		stringRedisTemplate.delete(bitmapKey);
		stringRedisTemplate.delete(zoneRemKey);

		int rowCount = venueConfigService.getRowCount(venueId, zoneId);
		int seatPerRow = venueConfigService.getSeatPerRow(venueId, zoneId);
		int totalSeats = venueConfigService.getZoneCapacity(venueId, zoneId);
		log.trace("[SeatOccupied][Init] zone={} config at: rowCount={}, seatPerRow={}, totalSeats={}",
				zoneId, rowCount, seatPerRow, totalSeats);

		byte[] initialBitmap = new byte[(totalSeats + 7) / 8];
		bitmapRedisTemplate.opsForValue().set(bitmapKey, initialBitmap);

		stringRedisTemplate.opsForValue().set(zoneRemKey, String.valueOf(totalSeats));
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			String rowKey = RedisKeyUtil.getRowRemainedSeats(eventId, zoneId, rowIndex);
			stringRedisTemplate.delete(rowKey);
			stringRedisTemplate.opsForValue().set(rowKey, String.valueOf(seatPerRow));
		}
	}
}
