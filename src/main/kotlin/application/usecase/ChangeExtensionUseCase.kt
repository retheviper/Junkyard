package application.usecase

import application.processing.ProcessingContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class ChangeExtensionUseCase {
    suspend fun execute(basePath: Path, toExtension: String, context: ProcessingContext) {
        val targets = Files.walk(basePath).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .filter { it != basePath }
                .toList()
        }

        context.setTotal(targets.size)

        targets.forEach {
            context.checkpoint()
            val newFileName = "${it.nameWithoutExtension}.$toExtension"
            context.processWithCount {
                context.updateCurrentFile(it)
                Files.move(it, it.resolveSibling(newFileName))
            }
        }
    }
}
