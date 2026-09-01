# Demo data for screenshots

`LockService.java` — `acquireUnsafe` flagged (no expiry);
`acquireSafe`/`acquireWithRedisson` not flagged.

## How to get the screenshot

1. `./gradlew runIde` from `redis-lock-missing-ttl-companion`, open
   this `demo/` folder as the project.
2. Full Screen, open `LockService.java` — a warning should appear on
   `acquireUnsafe`'s `.set(...)` call but not on the other two methods.
3. Screenshot with all three methods visible, save into
   `redis-lock-missing-ttl-companion/docs/screenshots/`. Close the
   sandbox.
