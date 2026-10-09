package application.usecase

import application.processing.ProcessingContext
import com.github.junrar.Junrar
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class RarToZipUseCase {
    private val rarExtension = "rar"

    suspend fun execute(basePath: Path, context: ProcessingContext) {
        val targets = Files.walk(basePath).use { stream ->
            stream.filter { Files.isRegularFile(it) && it != basePath }
                .filter { it.extension.equals(rarExtension, ignoreCase = true) }
                .toList()
        }

        context.setTotal(targets.size)

        targets.forEach {
            context.checkpoint()
            context.updateCurrentFile(it)
            context.processWithCount { convert(it) }
        }
    }

    private fun convert(rarFile: Path) {
        // A unique folder never collides with leftovers or user folders that share the archive name.
        val unarchivedFolder = Files.createTempDirectory(
            rarFile.toAbsolutePath().parent,
            "unrar_${rarFile.nameWithoutExtension}_"
        )
        try {
            Junrar.extract(rarFile.toFile(), unarchivedFolder.toFile())
            zipFiles(unarchivedFolder, rarFile.resolveSibling("${rarFile.nameWithoutExtension}.zip"))
        } finally {
            unarchivedFolder.toFile().deleteRecursively()
        }
    }
}
