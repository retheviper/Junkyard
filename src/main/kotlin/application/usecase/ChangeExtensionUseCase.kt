package application.usecase

import application.processing.ProcessingContext
import java.nio.file.Files
import java.nio.file.Path
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
                    Files.move(it, targetPath)
                }
            }
        }
    }

    private fun String.normalizeExtension(): String = trim().removePrefix(".")
}
