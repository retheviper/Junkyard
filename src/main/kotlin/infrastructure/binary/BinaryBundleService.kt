package infrastructure.binary

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission

class BinaryBundleService {
    fun getBinaryBundle(resourcePath: String): Path {
        this::class.java.getResource(resourcePath)
            ?: throw IllegalStateException("resource not found at $resourcePath")

        val tempFile = Files.createTempFile("bundle", null)
        tempFile.toFile().deleteOnExit()
        val input = this::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("resource stream not found at $resourcePath")
        input.use {
            Files.copy(it, tempFile, StandardCopyOption.REPLACE_EXISTING)
        }

        if (!Files.isExecutable(tempFile)) {
            makeExecutable(tempFile)
        }

        return tempFile
    }

    private fun makeExecutable(path: Path) {
        val posixView = Files.getFileAttributeView(path, PosixFileAttributeView::class.java)
        if (posixView != null) {
            val permissions = posixView.readAttributes().permissions().toMutableSet()
            permissions += PosixFilePermission.OWNER_EXECUTE
            posixView.setPermissions(permissions)
        } else {
            path.toFile().setExecutable(true, true)
        }
    }
}
