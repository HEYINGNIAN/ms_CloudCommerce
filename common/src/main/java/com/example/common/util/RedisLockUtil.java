package com.example.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 分布式锁工具类
 */
@Component
@Slf4j
public class RedisLockUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "distributed:lock:";
    private static final long DEFAULT_EXPIRE = 30; // 默认锁过期时间30秒

    /**
     * 获取分布式锁
     * @param key 锁的键
     * @param expire 过期时间（秒）
     * @return 锁标识，用于释放锁
     */
    public String lock(String key, long expire) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expire, TimeUnit.SECONDS);
        if (success) {
            log.info("获取锁成功: {}, value: {}", lockKey, lockValue);
            return lockValue;
        }
        return null;
    }

    /**
     * 获取分布式锁（使用默认过期时间）
     */
    public String lock(String key) {
        return lock(key, DEFAULT_EXPIRE);
    }

    /**
     * 释放分布式锁
     * @param key 锁的键
     * @param value 锁标识
     * @return 是否释放成功
     */
    public boolean unlock(String key, String value) {
        if (value == null) {
            return false;
        }
        String lockKey = LOCK_PREFIX + key;
        // 使用Lua脚本确保原子性
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), value);
        boolean success = result != null && result > 0;
        if (success) {
            log.info("释放锁成功: {}, value: {}", lockKey, value);
        } else {
            log.warn("释放锁失败: {}, value: {}", lockKey, value);
        }
        return success;
    }

    /**
     * 尝试获取锁
     * @param key 锁的键
     * @param expire 过期时间
     * @param waitTime 等待时间
     * @return 锁标识
     */
    public String tryLock(String key, long expire, long waitTime) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < waitTime * 1000) {
            String lockValue = lock(key, expire);
            if (lockValue != null) {
                return lockValue;
            }
            // 避免线程频繁竞争，短暂休眠
            Thread.sleep(100);
        }
        return null;
    }
}