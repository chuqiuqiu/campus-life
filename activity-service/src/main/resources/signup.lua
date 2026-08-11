-- 活动报名防超卖 Lua 脚本（对标黑马点评秒杀库存扣减）
-- KEYS[1] = 当前报名数 key：activity:signup:count:{id}
-- KEYS[2] = 名额上限 key：activity:signup:max:{id}
-- 返回 1 报名成功（名额扣减），0 名额已满
local count = tonumber(redis.call('get', KEYS[1]) or '0')
local max = tonumber(redis.call('get', KEYS[2]))
if max == nil then
    -- 名额未初始化，视为不可报名
    return 0
end
if count >= max then
    return 0
end
redis.call('incr', KEYS[1])
return 1
