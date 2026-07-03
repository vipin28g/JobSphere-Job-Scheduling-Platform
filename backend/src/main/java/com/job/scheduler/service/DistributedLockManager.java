package com.job.scheduler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class DistributedLockManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String RELEASE_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";

    /**
     * Acquire a distributed lock.
     * @param lockKey Key name
     * @param value Owner identifier
     * @param expireMs Expiration time in milliseconds
     * @return true if lock acquired successfully, false otherwise
     */
    public boolean acquireLock(String lockKey, String value, long expireMs) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, value, expireMs, TimeUnit.MILLISECONDS);
        return success != null && success;
    }

    /**
     * Release a distributed lock atomically using Lua script.
     * @param lockKey Key name
     * @param value Owner identifier
     * @return true if lock was held by owner and successfully released
     */
    public boolean releaseLock(String lockKey, String value) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RELEASE_LUA_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), value);
        return result != null && result == 1L;
    }
}
