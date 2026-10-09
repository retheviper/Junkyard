package application.usecase

import application.processing.ProcessingContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class ChangeExtensionUseCase {
    suspend fun execute(
        basePath: Path,
        fromExtension: String,
        toExtension: String,
        ignoreCase: Boolean,
        context: ProcessingContext
    ) {
        val normalizedFromExtension = fromExtension.normalizeExtension()
        val normalizedToExtension = toExtension.normalizeExtension()

        val targets = if (Files.isRegularFile(basePath)) {
            listOf(basePath).filter { it.extension.equals(normalizedFromExtension, ignoreCase = ignoreCase) }
        } else {
            Files.walk(basePath).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .filter { it.extension.equals(normalizedFromExtension, ignoreCase = ignoreCase) }
                    .toList()
            }
        }

        context.setTotal(targets.size)

        targets.forEach {
            context.checkpoint()
            val newFileName = "${it.nameWithoutExtension}.$normalizedToExtension"
            context.processWithCount {
                context.updateCurrentFile(it)
                val targetPath = it.resolveSibling(newFileName)
                if (targetPath.normalize() != it.normalize()) {
                    rename(it, targetPath)
                }
            }
        }
    }

    private fun rename(source: Path, target: Path) {
        if (!isSameExistingFile(source, target)) {
            Files.move(source, target)
            return
        }

        // Case-insensitive file systems treat a case-only rename as a no-op, so move through a temporary name.
        val temporary = source.resolveSibling("${UUID.randomUUID()}.tmp")
        Files.move(source, temporary)
        try {
            Files.move(temporary, target)
        } catch (error: IOException) {
            Files.move(temporary, source)
            throw error
        }
    }

    private fun String.normalizeExtension(): String = trim().removePrefix(".")
}
