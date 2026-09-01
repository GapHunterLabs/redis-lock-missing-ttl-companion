package dev.gaphunter.redislockmissingttlcompanion.model

import com.intellij.psi.PsiElement

enum class RedisClientKind {
    JEDIS,
    LETTUCE,
}

/** One `SET ... NX` call (Jedis or Lettuce) with no `EX`/`PX` expiry -- the lock can never expire if the process crashes before releasing it. */
data class MissingTtlHit(
    val anchor: PsiElement,
    val clientKind: RedisClientKind,
)
