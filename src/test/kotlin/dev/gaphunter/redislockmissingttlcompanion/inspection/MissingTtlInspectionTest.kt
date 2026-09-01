package dev.gaphunter.redislockmissingttlcompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MissingTtlInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MissingTtlInspection::class.java)
    }

    fun `test Jedis SET NX with no expiry is flagged`() {
        myFixture.configureByText(
            "LockService.java",
            """
            class LockService {
                void acquire(Jedis jedis, String key) {
                    jedis.set(key, "locked", SetParams.setParams().nx());
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("never expires") == true })
    }

    fun `test Jedis SET NX with EX expiry is not flagged`() {
        myFixture.configureByText(
            "LockService2.java",
            """
            class LockService2 {
                void acquire(Jedis jedis, String key) {
                    jedis.set(key, "locked", SetParams.setParams().nx().ex(30));
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("never expires") == true })
    }

    fun `test Lettuce SET NX with no expiry is flagged`() {
        myFixture.configureByText(
            "LockService3.java",
            """
            class LockService3 {
                void acquire(RedisCommands commands, String key) {
                    commands.set(key, "locked", SetArgs.Builder.nx());
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("never expires") == true })
    }

    fun `test Lettuce SET NX with PX expiry is not flagged`() {
        myFixture.configureByText(
            "LockService4.java",
            """
            class LockService4 {
                void acquire(RedisCommands commands, String key) {
                    commands.set(key, "locked", SetArgs.Builder.nx().px(30000));
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("never expires") == true })
    }

    fun `test Redisson plain lock with no leaseTime is never flagged -- watchdog handles it`() {
        myFixture.configureByText(
            "LockService5.java",
            """
            class LockService5 {
                void acquire(RedissonClient redisson, String key) {
                    RLock lock = redisson.getLock(key);
                    lock.lock();
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("never expires") == true })
    }

    fun `test an unrelated set call with two args is never flagged`() {
        myFixture.configureByText(
            "PlainMap.java",
            """
            class PlainMap {
                void store(java.util.Map<String, String> map, String key, String value) {
                    map.put(key, value);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("never expires") == true })
    }
}
