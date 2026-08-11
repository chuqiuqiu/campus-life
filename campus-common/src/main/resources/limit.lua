-- 固定窗口限流 Lua 脚本
-- KEYS[1] = 限流 key，例如 limit:ip:xxx
-- ARGV[1] = 窗口内最大请求数 max
-- ARGV[2] = 窗口时长（秒）window
-- 返回 1 放行，0 拒绝
local key = KEYS[1]
local max = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local current = tonumber(redis.call('get', key) or '0')
if current >= max then
    return 0
end
current = redis.call('incr', key)
if current == 1 then
    redis.call('expire', key, window)
end
return 1
