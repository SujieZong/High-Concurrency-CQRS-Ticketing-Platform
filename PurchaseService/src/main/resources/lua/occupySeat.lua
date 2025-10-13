-- src/main/resources/occupySeat.lua

-- KEYS[1]=bitmapKey
-- KEYS[2]=zoneRemainKey
-- KEYS[3]=rowRemainKey
-- KEYS[4]=eventUsedKey
-- KEYS[5]=eventTotalKey
-- ARGV[1]=bitPos

if #KEYS < 5 then error("need 5 KEYS") end
if #ARGV < 1 then error("need bitPos") end

local pos = tonumber(ARGV[1])
if not pos then
  redis.log(redis.LOG_WARNING, "[Lua:error] tonumber failed for ARGV[1]=" .. tostring(ARGV[1]))
  error("Invalid bit offset (not a number): " .. tostring(ARGV[1]))
end

if pos < 0 then
  redis.log(redis.LOG_WARNING, "[Lua:error] pos < 0: " .. pos)
  error("Invalid bit offset (negative): " .. pos)
end

redis.log(redis.LOG_DEBUG, "[Lua] bitPos validated = " .. pos)

-- check seat status
local occ = redis.call("GETBIT", KEYS[1], pos)
redis.log(redis.LOG_DEBUG,
        string.format("[Lua] GETBIT(%s, %d) = %d", KEYS[1], pos, occ)
)
if occ == 1 then
  redis.log(redis.LOG_NOTICE, "[Lua] seat already occupied → returning 1")
  return 1
end

local zoneRem = tonumber(redis.call("GET", KEYS[2])) or 0
local rowRem  = tonumber(redis.call("GET", KEYS[3])) or 0
local eventUsed = tonumber(redis.call("GET", KEYS[4])) or 0
local eventTotal = tonumber(redis.call("GET", KEYS[5])) or 0

redis.log(redis.LOG_DEBUG,
        string.format("[Lua] before occupy → zoneRem=%d, rowRem=%d, eventUsed=%d, eventTotal=%d",
        zoneRem, rowRem, eventUsed, eventTotal)
)

if zoneRem <= 0 then
  redis.log(redis.LOG_NOTICE, "[Lua] zone full → returning 2")
  return 2
end
if rowRem <= 0 then
  redis.log(redis.LOG_NOTICE, "[Lua] row full → returning 3")
  return 3
end
if eventTotal > 0 and eventUsed >= eventTotal then
  redis.log(redis.LOG_NOTICE, "[Lua] event full → returning 4")
  return 4
end

-- Check setbit result
local setBitResult = redis.call("SETBIT", KEYS[1], pos, 1)
redis.log(redis.LOG_DEBUG,
        string.format("[Lua] SETBIT result: %d (0=was free, 1=was occupied)", setBitResult))

-- Check Setbit resul, update counter once bit flip.
if setBitResult == 0 then
  redis.call("DECR", KEYS[2])
  redis.call("DECR", KEYS[3])
  redis.call("INCR", KEYS[4])

  local newZone = redis.call("GET", KEYS[2])
  local newRow  = redis.call("GET", KEYS[3])
  local newUsed = redis.call("GET", KEYS[4])

  redis.log(redis.LOG_NOTICE,
          string.format("[Lua] seat occupied successfully; new zoneRem=%s, rowRem=%s, eventUsed=%s",
          newZone, newRow, newUsed)
  )
  return 0  -- successfully occupied
else
  -- setBitResult == 1 means seat already taken
  redis.log(redis.LOG_WARNING,
          "[Lua] race condition detected: seat became occupied between checks")
  return 1  -- 座位已被占用
end
