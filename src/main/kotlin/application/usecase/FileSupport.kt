package application.usecase

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Writes [target] through a temporary sibling file so that a failed write never leaves a partial
 * [target] behind. An existing [target] is replaced only after [write] succeeds.
 *
 * [write] creates the temporary file itself. Files.createTempFile is avoided on purpose: it makes the file
 * readable by its owner only, and that permission would carry over to [target].
 */
internal fun writeAtomically(target: Path, write: (Path) -> Unit) {
    val tempFile = target.resolveSibling("${target.fileName}.${UUID.randomUUID()}.tmp")
    try {
        write(tempFile)
        moveReplacing(tempFile, target)
    } catch (error: Throwable) {
        Files.deleteIfExists(tempFile)
        throw error
    }
}

internal fun isSameExistingFile(path: Path, other: Path): Boolean =
    Files.exists(other) && Files.isSameFile(path, other)

private fun moveReplacing(source: Path, target: Path) {
    runCatching {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }.getOrElse {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
    }
}
