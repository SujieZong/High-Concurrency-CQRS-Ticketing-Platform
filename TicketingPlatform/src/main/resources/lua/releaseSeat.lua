-- KEYS[1]=bitmapKey  KEYS[2]=zoneRemainKey  KEYS[3]=rowRemainKey
-- ARGV[1]=bitPos
local pos = tonumber(ARGV[1])
if not pos or pos < 0 then error("Invalid bit offset: "..ARGV[1]) end

local wasOcc = redis.call('GETBIT', KEYS[1], pos)
if wasOcc == 1 then
    redis.call('SETBIT', KEYS[1], pos, 0)
    redis.call('INCR',   KEYS[2])
    redis.call('INCR',   KEYS[3])
    return 1  -- released
end
return 0
