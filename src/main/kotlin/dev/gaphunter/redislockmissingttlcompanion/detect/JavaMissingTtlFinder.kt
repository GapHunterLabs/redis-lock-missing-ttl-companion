package dev.gaphunter.redislockmissingttlcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.redislockmissingttlcompanion.model.MissingTtlHit
import dev.gaphunter.redislockmissingttlcompanion.model.RedisClientKind

/**
 * Finds a `SET ... NX` call (the manual distributed-lock pattern via
 * Jedis's `SetParams.nx()`/Lettuce's `SetArgs.Builder.nx()`) with no
 * `EX`/`PX` expiry chained onto the same params/args builder -- a lock
 * acquired this way never expires on its own if the process holding it
 * crashes before releasing it, permanently blocking the resource for
 * every other caller.
 *
 * **Redisson is explicitly considered, and deliberately never flagged**
 * for its own plain `RLock.lock()` (no `leaseTime` argument): Redisson's
 * own watchdog mechanism automatically renews the lock while the holder
 * is alive precisely BECAUSE no lease time was given -- flagging that
 * shape the same way as Jedis/Lettuce's SET NX would be a real, wrong
 * false positive.
 *
 * **v0.1 scope, stated honestly:** matches Jedis's `SetParams`/Lettuce's
 * `SetArgs.Builder` NX-builder pattern by text within the `.set(...)`
 * call's own argument -- never resolves the real client type, so an
 * unrelated `SetParams`/`SetArgs`-named class from a different library
 * is a possible (rare) false positive. Never follows a `leaseTime`/
 * expiry value passed in from a variable computed elsewhere -- only
 * detects total absence of the expiry method call in the same
 * argument's text.
 */
object JavaMissingTtlFinder {

    private val NX_CALL = Regex("""\.nx\s*\(""", RegexOption.IGNORE_CASE)
    private val EXPIRY_CALL = Regex("""\.(ex|px|exAt|pxAt)\s*\(""", RegexOption.IGNORE_CASE)

    fun findAll(file: PsiFile): List<MissingTtlHit> {
        val hits = mutableListOf<MissingTtlHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                hitFor(expression)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitFor(call: PsiMethodCallExpression): MissingTtlHit? {
        val methodExpr = call.methodExpression
        if (methodExpr.referenceName != "set") return null
        val args = call.argumentList.expressions
        if (args.size < 3) return null

        val paramsArg = args[2].text
        if (!NX_CALL.containsMatchIn(paramsArg)) return null
        if (EXPIRY_CALL.containsMatchIn(paramsArg)) return null

        val kind = when {
            paramsArg.contains("SetParams") -> RedisClientKind.JEDIS
            paramsArg.contains("SetArgs") -> RedisClientKind.LETTUCE
            else -> return null // neither known builder -- don't guess
        }

        return MissingTtlHit(anchorOf(methodExpr), kind)
    }

    private fun anchorOf(methodExpr: PsiReferenceExpression): PsiElement = methodExpr.referenceNameElement ?: methodExpr
}
