package com.luisborges.limpadorandroid.utils

import android.os.Environment
import com.luisborges.limpadorandroid.model.FileItem
import com.luisborges.limpadorandroid.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileUtils {

    // Padrões de arquivos temporários/lixo
    private val TEMP_PATTERNS = listOf(".tmp", ".temp", ".bak", ".old", ".swp", ".dmp")
    private val LOG_PATTERNS = listOf(".log", ".trace", ".crash", "-log", "_log")
    private val RESIDUAL_FOLDERS = listOf(
        ".thumbnails", "lost+found", ".trash", "Trash",
        "Backup", "backup", ".nomedia_bak"
    )

    /** Varre o armazenamento e retorna listas de arquivos por categoria */
    suspend fun scanStorage(
        onProgress: (String) -> Unit = {}
    ): Map<FileType, List<FileItem>> = withContext(Dispatchers.IO) {

        val result = mutableMapOf<FileType, MutableList<FileItem>>()
        FileType.values().forEach { result[it] = mutableListOf() }

        val roots = listOf(
            Environment.getExternalStorageDirectory(),
            Environment.getDataDirectory()
        ).filter { it.exists() && it.canRead() }

        for (root in roots) {
            onProgress("Varrendo ${root.absolutePath}...")
            scanDirectory(root, result, onProgress, depth = 0)
        }

        result
    }

    private fun scanDirectory(
        dir: File,
        result: MutableMap<FileType, MutableList<FileItem>>,
        onProgress: (String) -> Unit,
        depth: Int
    ) {
        if (depth > 10) return // evita recursão infinita em symlinks
        if (!dir.canRead()) return

        val files = try {
            dir.listFiles() ?: return
        } catch (e: SecurityException) {
            return
        }

        onProgress("Verificando: ${dir.name}")

        // Pasta vazia
        if (files.isEmpty() && depth > 1 && dir.name !in listOf("Android", "data", "obb")) {
            result[FileType.EMPTY_FOLDER]?.add(
                FileItem(
                    name = dir.name,
                    path = dir.absolutePath,
                    size = 0L,
                    lastModified = dir.lastModified(),
                    type = FileType.EMPTY_FOLDER
                )
            )
            return
        }

        for (file in files) {
            if (file.isDirectory) {
                // Pastas residuais conhecidas
                if (file.name in RESIDUAL_FOLDERS) {
                    val dirSize = calculateDirSize(file)
                    result[FileType.RESIDUAL]?.add(
                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            size = dirSize,
                            lastModified = file.lastModified(),
                            type = FileType.RESIDUAL
                        )
                    )
                } else {
                    scanDirectory(file, result, onProgress, depth + 1)
                }
            } else {
                classifyFile(file)?.let { item ->
                    result[item.type]?.add(item)
                }
            }
        }
    }

    private fun classifyFile(file: File): FileItem? {
        if (!file.exists() || !file.canRead()) return null
        val name = file.name.lowercase()
        val size = file.length()
        val path = file.absolutePath
        val modified = file.lastModified()

        // APKs soltos (não são a instalação do sistema)
        if (name.endsWith(".apk") && !path.contains("/data/app/")) {
            return FileItem(file.name, path, size, modified, FileType.APK)
        }

        // Arquivos de log
        if (LOG_PATTERNS.any { name.endsWith(it) || name.contains(it) }) {
            return FileItem(file.name, path, size, modified, FileType.LOG)
        }

        // Temporários
        if (TEMP_PATTERNS.any { name.endsWith(it) }) {
            return FileItem(file.name, path, size, modified, FileType.TEMP)
        }

        // Thumbnails do sistema Android
        if (path.contains("/.thumbnails/") || path.contains("/thumbnails/") && name.endsWith(".jpg")) {
            return FileItem(file.name, path, size, modified, FileType.THUMBNAIL)
        }

        // Cache em pasta de cache
        if (path.contains("/cache/") && size > 0) {
            return FileItem(file.name, path, size, modified, FileType.CACHE)
        }

        // Vídeos grandes (>100MB)
        if ((name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") ||
                    name.endsWith(".mov") || name.endsWith(".3gp")) && size > 100 * 1024 * 1024) {
            return FileItem(file.name, path, size, modified, FileType.VIDEO)
        }

        return null
    }

    /** Exclui uma lista de arquivos e retorna quantos foram deletados e o espaço liberado */
    suspend fun deleteFiles(files: List<FileItem>): Pair<Int, Long> =
        withContext(Dispatchers.IO) {
            var deleted = 0
            var freed = 0L
            for (item in files) {
                val file = File(item.path)
                val size = if (file.isDirectory) calculateDirSize(file) else file.length()
                if (deleteRecursive(file)) {
                    deleted++
                    freed += size
                }
            }
            Pair(deleted, freed)
        }

    private fun deleteRecursive(file: File): Boolean {
        if (!file.exists()) return true
        return if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
            file.delete()
        } else {
            file.delete()
        }
    }

    fun calculateDirSize(dir: File): Long {
        var size = 0L
        try {
            val files = dir.listFiles() ?: return 0L
            for (f in files) {
                size += if (f.isDirectory) calculateDirSize(f) else f.length()
            }
        } catch (e: Exception) { /* sem permissão */ }
        return size
    }

    /** Caminho da pasta de cache do app de um pacote */
    fun getAppCachePath(packageName: String): File =
        File("/data/data/$packageName/cache")

    /** Limpa o cache de um app específico via shell (root necessário para outros apps) */
    fun clearAppCacheViaCommand(packageName: String): Boolean {
        return try {
            val process = Runtime.getRuntime()
                .exec(arrayOf("pm", "clear", packageName))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
