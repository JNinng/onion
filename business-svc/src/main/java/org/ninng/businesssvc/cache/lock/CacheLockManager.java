package org.ninng.businesssvc.cache.lock;

import org.ninng.businesssvc.cache.exception.CacheLockException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CacheLockManager {

    private static final String LOCK_PREFIX = "lock:business-cache:";

    private final RedissonClient redisson;

    public CacheLockManager(RedissonClient redisson) {
        this.redisson = redisson;
    }

    public boolean tryLock(String scope) {
        RLock lock = redisson.getLock(LOCK_PREFIX + scope);
        try {
            return lock.tryLock(0, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread()
                    .interrupt();
            return false;
        }
    }

    public void unlock(String scope) {
        RLock lock = redisson.getLock(LOCK_PREFIX + scope);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public <T> T withLock(String scope, Supplier<T> action, Supplier<T> onFail) {
        if (!tryLock(scope)) {
            return onFail.get();
        }
        try {
            return action.get();
        } finally {
            unlock(scope);
        }
    }

    public void withLockOrThrow(String scope, Runnable action) {
        withLock(scope, () -> {
            action.run();
            return null;
        }, () -> {
            throw new CacheLockException("Failed to acquire lock for " + scope);
        });
    }
}
