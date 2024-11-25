package service

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

class BinaryBundleService {
    fun getBinaryBundle(resourcePath: String): Path {
        val url = this::class.java.getResource(resourcePath)
        if (url == null) {
            throw IllegalStateException("resource not found at $resourcePath")
        }

        val tempFile = Files.createTempFile("bundle", null)
        tempFile.toFile().deleteOnExit()
        this::class.java.getResourceAsStream(resourcePath)?.use { input ->
            Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }

        if (!Files.isExecutable(tempFile)) {
            Files.setPosixFilePermissions(tempFile, setOf(PosixFilePermission.OWNER_EXECUTE))
        }

        return tempFile
    }
}