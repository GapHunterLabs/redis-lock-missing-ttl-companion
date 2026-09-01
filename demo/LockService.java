class LockService {

    // Flagged: no expiry -- lock never expires if this process crashes.
    void acquireUnsafe(Jedis jedis, String key) {
        jedis.set(key, "locked", SetParams.setParams().nx());
    }

    // Not flagged: EX expiry present.
    void acquireSafe(Jedis jedis, String key) {
        jedis.set(key, "locked", SetParams.setParams().nx().ex(30));
    }

    // Not flagged: Redisson's watchdog auto-renews when no leaseTime is given.
    void acquireWithRedisson(RedissonClient redisson, String key) {
        RLock lock = redisson.getLock(key);
        lock.lock();
    }
}
