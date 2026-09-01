package dev.gaphunter.redislockmissingttlcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import dev.gaphunter.redislockmissingttlcompanion.detect.JavaMissingTtlFinder
import dev.gaphunter.redislockmissingttlcompanion.model.MissingTtlHit
import dev.gaphunter.redislockmissingttlcompanion.model.RedisClientKind
import dev.gaphunter.redislockmissingttlcompanion.review.ReviewPrompt

/**
 * Flags a `SET ... NX` call (Jedis/Lettuce manual distributed-lock
 * pattern) with no `EX`/`PX` expiry -- a lock acquired this way never
 * expires on its own if the process holding it crashes, permanently
 * blocking the resource for every other caller. Runs via `checkFile`
 * (same shape as every other inspection in this catalog);
 * [JavaMissingTtlFinder] does the real PSI walk.
 */
class MissingTtlInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null
        if (file !is PsiJavaFile) return null

        val hits = JavaMissingTtlFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: MissingTtlHit): String {
        val clientName = when (hit.clientKind) {
            RedisClientKind.JEDIS -> "Jedis"
            RedisClientKind.LETTUCE -> "Lettuce"
        }
        return "$clientName SET ... NX with no EX/PX expiry -- this lock never expires on its own if the " +
            "holder crashes before releasing it, permanently blocking the resource for every other caller"
    }
}
