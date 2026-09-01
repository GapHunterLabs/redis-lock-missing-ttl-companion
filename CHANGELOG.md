<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Redis/Redisson Distributed Lock Missing TTL Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Warning on a Jedis/Lettuce `SET ... NX` call with no `EX`/`PX`
  expiry -- the lock never expires on its own if the holder crashes.
- Redisson's own `RLock.lock()` (no leaseTime) is explicitly
  considered and never flagged -- its watchdog mechanism auto-renews
  the lock in that exact shape.

[Unreleased]: https://github.com/GapHunterLabs/redis-lock-missing-ttl-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/redis-lock-missing-ttl-companion/commits/0.1.0
