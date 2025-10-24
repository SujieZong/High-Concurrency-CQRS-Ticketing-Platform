package org.java.purchaseservice.service.redis;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.exception.RowFullException;
import org.java.purchaseservice.exception.SeatOccupiedException;
import org.java.purchaseservice.exception.ZoneFullException;
import org.java.purchaseservice.service.initialize.VenueConfigService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SeatOccupiedRedisFacade {
  private final VenueConfigService venueConfigService;
  private final DefaultRedisScript<Long> tryOccupySeatScript; // load lua script method
  private final DefaultRedisScript<Long> tryReleaseSeatScript; // load lua script method
  private final StringRedisTemplate stringRedisTemplate;

  public SeatOccupiedRedisFacade(
      VenueConfigService venueConfigService,
      StringRedisTemplate stringRedisTemplate,
      @Qualifier("tryOccupySeatScript") DefaultRedisScript<Long> tryOccupySeatScript,
      @Qualifier("tryReleaseSeatScript") DefaultRedisScript<Long> tryReleaseSeatScript) {
    this.venueConfigService = venueConfigService;
    this.stringRedisTemplate = stringRedisTemplate;
    this.tryOccupySeatScript = tryOccupySeatScript;
    this.tryReleaseSeatScript = tryReleaseSeatScript;
  }

  /** Check row && zone full check if a seat taken update 'bit' occupancy and update counter */
  public void tryOccupySeat(String eventId, String venueId, int zoneId, String row, String col) {
    log.debug(
        "[SeatOccupiedRedisFacade] tryOccupySeat start: event={}, venue={}, zone={}, row={}, col={}",
        eventId,
        venueId,
        zoneId,
        row,
        col);

    int seatPerRow = venueConfigService.getSeatPerRow(venueId, zoneId);
    int rowIndex = convertRowToIndex(row);
    int bitPos = calcBitPosition(row, col, seatPerRow);

    String bitmapKey = RedisKeyUtil.getZoneBitMapKey(eventId, zoneId);
    String zoneRemainKey = RedisKeyUtil.getZoneRemainedSeats(eventId, zoneId);
    String rowRemainKey = RedisKeyUtil.getRowRemainedSeats(eventId, zoneId, rowIndex);
    String eventUsedKey = RedisKeyUtil.getEventUsedSeatsKey(eventId);
    String eventTotalKey = RedisKeyUtil.getEventTotalCapacityKey(eventId);
    log.trace(
        "[SeatOccupiedRedisFacade] Lua keys: bitmap={}, zoneRem={}, rowRem={}, eventUsed={}, eventTotal={}, bitPos={}",
        bitmapKey,
        zoneRemainKey,
        rowRemainKey,
        eventUsedKey,
        eventTotalKey,
        bitPos);

    Long res;
    try {
      res =
          stringRedisTemplate.execute(
              tryOccupySeatScript,
              List.of(bitmapKey, zoneRemainKey, rowRemainKey, eventUsedKey, eventTotalKey),
              String.valueOf(bitPos));
      log.debug("[SeatOccupiedRedisFacade] Lua script execution returned: {}", res);

    } catch (Exception ex) {
      log.error(
          """
					[SeatOccupiedRedisFacade] !!! Lua script execution FAILED !!!
					  KEYS = [{}, {}, {}, {}, {}]
					  ARGV = [{}]
					  Exception: {}""",
          bitmapKey,
          zoneRemainKey,
          rowRemainKey,
          eventUsedKey,
          eventTotalKey,
          bitPos,
          ex,
          ex);
      throw ex;
    }

    // Lua will either throw exception or return a number
    switch (res.intValue()) {
      case 0:
        log.trace(
            "[SeatOccupiedRedisFacade] Seat occupied successfully: event={}, venue={}, zone={}, row={}, col={}",
            eventId,
            venueId,
            zoneId,
            row,
            col);
        return;
      case 1:
        log.warn(
            "[SeatOccupiedRedisFacade] Seat already occupied: event={}, zone={}, row={}, col={}",
            eventId,
            zoneId,
            row,
            col);
        throw new SeatOccupiedException("Seat already occupied.");
      case 2:
        log.warn("[SeatOccupiedRedisFacade] Zone full: event={}, zone={}", eventId, zoneId);
        throw new ZoneFullException("Zone already Full.");
      case 3:
        log.warn(
            "[SeatOccupiedRedisFacade] Row full: event={}, zone={}, row={}", eventId, zoneId, row);
        throw new RowFullException("Row already Full.");
      default:
        log.error("[SeatOccupiedRedisFacade] Unknown result from Lua script: {}", res);
        throw new RuntimeException("Unknown Lua script return code: " + res);
    }
  }

  public void releaseSeat(String eventId, String venueId, int zoneId, String row, String col) {
    log.debug(
        "[SeatOccupiedRedisFacade] releaseSeat start: event={}, venue={}, zone={}, row={}, col={}",
        eventId,
        venueId,
        zoneId,
        row,
        col);

    int seatPerRow = venueConfigService.getSeatPerRow(venueId, zoneId);
    int bitPos = calcBitPosition(row, col, seatPerRow);

    String bitmapKey = RedisKeyUtil.getZoneBitMapKey(eventId, zoneId);
    String zoneRemainKey = RedisKeyUtil.getZoneRemainedSeats(eventId, zoneId);

    int rowIndex = convertRowToIndex(row);
    String rowRemainKey = RedisKeyUtil.getRowRemainedSeats(eventId, zoneId, rowIndex);

    stringRedisTemplate.execute(
        tryReleaseSeatScript,
        List.of(bitmapKey, zoneRemainKey, rowRemainKey),
        String.valueOf(bitPos));

    log.trace(
        "[SeatOccupiedRedisFacade] Seat released: event={}, venue={}, zone={}, row={}, col={}",
        eventId,
        venueId,
        zoneId,
        row,
        col);
  }

  private int calcBitPosition(String row, String col, int seatPerRow) {
    int rowIndex = convertRowToIndex(row);
    int colIndex = Integer.parseInt(col) - 1;
    return rowIndex * seatPerRow + colIndex;
  }

  // turn the row name from A - zz as numbers
  private int convertRowToIndex(String row) {
    row = row.toUpperCase();
    int idx = 0;
    for (char c : row.toCharArray()) {
      idx = idx * 26 + (c - 'A' + 1);
    }
    return idx - 1;
  }
}
