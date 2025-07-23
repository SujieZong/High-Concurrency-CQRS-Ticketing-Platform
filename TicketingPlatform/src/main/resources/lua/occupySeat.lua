-- src/main/resources/occupySeat.lua

-- KEYS[1]=bitmapKey
-- KEYS[2]=zoneRemainKey
-- KEYS[3]=rowRemainKey
-- ARGV[1]=bitPos


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

--
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
redis.log(redis.LOG_DEBUG,
        string.format("[Lua] before occupy → zoneRem=%d, rowRem=%d", zoneRem, rowRem)
)

if zoneRem <= 0 then
  redis.log(redis.LOG_NOTICE, "[Lua] zone full → returning 2")
  return 2
end
if rowRem <= 0 then
  redis.log(redis.LOG_NOTICE, "[Lua] row full → returning 3")
  return 3
end

redis.call("SETBIT", KEYS[1], pos, 1)
redis.call("DECR", KEYS[2])
redis.call("DECR", KEYS[3])
local newZone = redis.call("GET", KEYS[2])
local newRow  = redis.call("GET", KEYS[3])
redis.log(redis.LOG_NOTICE,
        string.format("[Lua] occupied; new zoneRem=%s, new rowRem=%s", newZone, newRow)
)

return 0
