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
            convert(it, context)
        }
    }

    private fun convert(rarFile: Path, context: ProcessingContext) {
        val unarchivedFolder = rarFile.resolveSibling("unrar_${rarFile.nameWithoutExtension}")
        Files.createDirectory(unarchivedFolder)
        context.incrementCurrent()

        runCatching { Junrar.extract(rarFile.toFile(), unarchivedFolder.toFile()) }
            .onFailure {
                unarchivedFolder.toFile().deleteRecursively()
                context.incrementFailed()
                return
            }

        val zipFilePath = rarFile.resolveSibling("${rarFile.nameWithoutExtension}.zip")
        zipFiles(unarchivedFolder, zipFilePath)

        unarchivedFolder.toFile().deleteRecursively()
        context.incrementProcessed()
    }
}
