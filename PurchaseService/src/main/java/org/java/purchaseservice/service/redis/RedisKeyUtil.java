package org.java.purchaseservice.service.redis;

public class RedisKeyUtil {

	// zone, row, seat count related key
	public static String getRowCountKey(String venueId, int zoneId) {
		return String.format("venue:%s:zone:%s:rowCount", venueId, zoneId);
	}

	public static String getSeatPerRowKey(String venueId, int zoneId) {
		return String.format("venue:%s:zone:%s:seatPerRow", venueId, zoneId);
	}

	public static String getZoneCapacityKey(String venueId, int zoneId) {
		return String.format("venue:%s:zone:%s:capacity", venueId, zoneId);
	}

	public static String getZoneSetKey(String venueId) {
		return String.format("venue:%s", venueId);
	}

	//Bitmap related Key
	public static String getZoneBitMapKey(String eventId, int zoneId) {
		return String.format("event:%s:zone:%s:occupied", eventId, zoneId);
	}

	public static String getZoneRemainedSeats(String eventId, int zoneId) {
		return String.format("event:%s:zone:%s:remainingZoneSeats", eventId, zoneId);
	}

	public static String getRowRemainedSeats(String eventId, int zoneId, int rowIndex) {
		return String.format("event:%s:zone:%s:row:%d:remainingSeats", eventId, zoneId, rowIndex);
	}
}
