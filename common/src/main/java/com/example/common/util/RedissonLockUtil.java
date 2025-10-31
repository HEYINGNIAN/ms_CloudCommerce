package com.example.common.util;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redisson的分布式锁工具类
 */
@Component
@Slf4j
public class RedissonLockUtil {

    @Resource
    private RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "distributed:lock:";
    private static final long DEFAULT_EXPIRE = 30; // 默认锁过期时间30秒
    private static final long DEFAULT_WAIT_TIME = 3; // 默认等待时间3秒

    /**
     * 获取分布式锁
     * @param key 锁的键
     * @param expire 过期时间（秒）
     * @return 锁对象
     */
    public RLock lock(String key, long expire) {
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);
        
        // 加锁，设置过期时间，Redisson会自动续期（看门狗机制）
        lock.lock(expire, TimeUnit.SECONDS);
        log.info("获取锁成功: {}", lockKey);
        return lock;
    }

    /**
     * 获取分布式锁（使用默认过期时间）
     */
    public RLock lock(String key) {
        return lock(key, DEFAULT_EXPIRE);
    }

    /**
     * 尝试获取锁
     * @param key 锁的键
     * @param expire 过期时间（秒）
     * @param waitTime 等待时间（秒）
     * @return 锁对象，如果获取失败返回null
     */
    public RLock tryLock(String key, long expire, long waitTime) {
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试加锁
            boolean success = lock.tryLock(waitTime, expire, TimeUnit.SECONDS);
            if (success) {
                log.info("尝试获取锁成功: {}", lockKey);
                return lock;
            } else {
                log.warn("尝试获取锁失败: {}", lockKey);
                return null;
            }
        } catch (InterruptedException e) {
            log.error("尝试获取锁被中断: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 尝试获取锁（使用默认过期时间和等待时间）
     */
    public RLock tryLock(String key) {
        return tryLock(key, DEFAULT_EXPIRE, DEFAULT_WAIT_TIME);
    }

    /**
     * 释放锁
     * @param lock 锁对象
     */
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("释放锁成功");
        }
    }

    /**
     * 释放指定key的锁
     * @param key 锁的键
     */
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("释放锁成功: {}", lockKey);
        }
    }

    /**
     * 检查锁是否被当前线程持有
     * @param lock 锁对象
     * @return 是否持有
     */
    public boolean isHeldByCurrentThread(RLock lock) {
        return lock != null && lock.isHeldByCurrentThread();
    }
}