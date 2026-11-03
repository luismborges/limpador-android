package com.luisborges.limpadorandroid.utils

import android.os.Environment
import com.luisborges.limpadorandroid.model.FileItem
import com.luisborges.limpadorandroid.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object FileUtils {

    // ── Padrões de lixo automático ─────────────────────────────────────────
    private val TEMP_PATTERNS   = listOf(".tmp", ".temp", ".bak", ".old", ".swp", ".dmp")
    private val LOG_PATTERNS    = listOf(".log", ".trace", ".crash", "-log", "_log")
    private val RESIDUAL_FOLDERS = setOf(
        ".thumbnails", "lost+found", ".trash", "Trash",
        "Backup", "backup", ".nomedia_bak"
    )

    // ── Extensões por categoria ────────────────────────────────────────────
    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "tiff", "raw"
    )
    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "3gp", "wmv", "flv", "m4v", "ts", "webm", "3g2"
    )
    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt",
        "txt", "odt", "ods", "odp", "csv", "rtf"
    )

    // ── Caminhos do WhatsApp ───────────────────────────────────────────────
    // Android antigo: /sdcard/WhatsApp/Media/
    // Android 10+:    /sdcard/Android/media/com.whatsapp/WhatsApp/Media/
    private val WHATSAPP_RELATIVE_PATHS = listOf(
        "WhatsApp/Media",
        "Android/media/com.whatsapp/WhatsApp/Media",
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"  // WhatsApp Business
    )

    // Subpastas do WhatsApp que nos interessam
    private val WHATSAPP_MEDIA_SUBFOLDERS = setOf(
        "WhatsApp Images",
        "WhatsApp Video",
        "WhatsApp Audio",
        "WhatsApp Documents",
        "WhatsApp Animated Gifs",
        "WhatsApp Stickers",
        "WhatsApp Voice Notes",
        "WhatsApp Images/Sent",
        "WhatsApp Video/Sent"
    )

    // ── Varredura principal ────────────────────────────────────────────────

    /** Varre o armazenamento e retorna listas de arquivos por categoria */
    suspend fun scanStorage(
        onProgress: (String) -> Unit = {}
    ): Map<FileType, List<FileItem>> = withContext(Dispatchers.IO) {

        val result = mutableMapOf<FileType, MutableList<FileItem>>()
        FileType.values().forEach { result[it] = mutableListOf() }

        val externalStorage = Environment.getExternalStorageDirectory()
        val dataDir = Environment.getDataDirectory()

        // 1. Escanear mídia do WhatsApp primeiro (específico e prioritário)
        onProgress("Procurando mídia do WhatsApp...")
        for (relPath in WHATSAPP_RELATIVE_PATHS) {
            val waDir = File(externalStorage, relPath)
            if (waDir.exists() && waDir.canRead()) {
                scanWhatsAppMedia(waDir, result, onProgress)
            }
        }

        // 2. Escanear armazenamento geral
        val roots = listOf(externalStorage, dataDir).filter { it.exists() && it.canRead() }
        for (root in roots) {
            onProgress("Varrendo ${root.absolutePath}...")
            scanDirectory(root, result, onProgress, depth = 0)
        }

        // 3. Detectar arquivos duplicados
        onProgress("Procurando arquivos duplicados...")
        val candidatos = listOf(
            result[FileType.IMAGE],
            result[FileType.VIDEO],
            result[FileType.DOCUMENT],
            result[FileType.WHATSAPP_MEDIA]
        ).filterNotNull().flatten()

        val duplicates = findDuplicates(candidatos, onProgress)
        result[FileType.DUPLICATE]?.addAll(duplicates)

        result
    }

    // ── WhatsApp ──────────────────────────────────────────────────────────

    /** Escaneia as subpastas de mídia do WhatsApp */
    private fun scanWhatsAppMedia(
        waMediaDir: File,
        result: MutableMap<FileType, MutableList<FileItem>>,
        onProgress: (String) -> Unit
    ) {
        val dirs = waMediaDir.listFiles()?.filter { it.isDirectory } ?: return
        for (subDir in dirs) {
            onProgress("WhatsApp › ${subDir.name}")
            // Inclui a pasta principal + /Sent dentro dela
            addFilesFromDir(subDir, result)
            val sentDir = File(subDir, "Sent")
            if (sentDir.exists() && sentDir.canRead()) {
                addFilesFromDir(sentDir, result)
            }
        }
    }

    private fun addFilesFromDir(dir: File, result: MutableMap<FileType, MutableList<FileItem>>) {
        val files = try { dir.listFiles() } catch (e: Exception) { null } ?: return
        for (file in files) {
            if (!file.isFile || !file.canRead()) continue
            if (file.name.startsWith(".")) continue  // arquivos ocultos (.nomedia etc.)
            result[FileType.WHATSAPP_MEDIA]?.add(
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    type = FileType.WHATSAPP_MEDIA
                )
            )
        }
    }

    // ── Varredura geral ───────────────────────────────────────────────────

    private fun scanDirectory(
        dir: File,
        result: MutableMap<FileType, MutableList<FileItem>>,
        onProgress: (String) -> Unit,
        depth: Int
    ) {
        if (depth > 12) return  // evita recursão infinita em symlinks
        if (!dir.canRead()) return

        // Pula pastas do WhatsApp (já escaneadas acima)
        val absPath = dir.absolutePath
        if (WHATSAPP_RELATIVE_PATHS.any { absPath.contains(it) }) return

        val files = try {
            dir.listFiles() ?: return
        } catch (e: SecurityException) {
            return
        }

        onProgress("Verificando: ${dir.name}")

        // Pasta vazia (não raiz)
        if (files.isEmpty() && depth > 1 && dir.name !in listOf("Android", "data", "obb")) {
            result[FileType.EMPTY_FOLDER]?.add(
                FileItem(dir.name, absPath, 0L, dir.lastModified(), FileType.EMPTY_FOLDER)
            )
            return
        }

        for (file in files) {
            if (file.isDirectory) {
                if (file.name in RESIDUAL_FOLDERS) {
                    val dirSize = calculateDirSize(file)
                    result[FileType.RESIDUAL]?.add(
                        FileItem(file.name, file.absolutePath, dirSize, file.lastModified(), FileType.RESIDUAL)
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
        val ext  = name.substringAfterLast('.', "")
        val size = file.length()
        val path = file.absolutePath
        val mod  = file.lastModified()

        // APKs soltos (não instalação do sistema)
        if (ext == "apk" && !path.contains("/data/app/")) {
            return FileItem(file.name, path, size, mod, FileType.APK)
        }

        // Logs
        if (LOG_PATTERNS.any { name.endsWith(it) || name.contains(it) }) {
            return FileItem(file.name, path, size, mod, FileType.LOG)
        }

        // Temporários
        if (TEMP_PATTERNS.any { name.endsWith(it) }) {
            return FileItem(file.name, path, size, mod, FileType.TEMP)
        }

        // Thumbnails do Android
        if (path.contains("/.thumbnails/") || (path.contains("/thumbnails/") && ext == "jpg")) {
            return FileItem(file.name, path, size, mod, FileType.THUMBNAIL)
        }

        // Cache de apps
        if (path.contains("/cache/") && size > 0) {
            return FileItem(file.name, path, size, mod, FileType.CACHE)
        }

        // Imagens (todos os tamanhos)
        if (ext in IMAGE_EXTENSIONS) {
            return FileItem(file.name, path, size, mod, FileType.IMAGE)
        }

        // Vídeos (todos os tamanhos — sem filtro de 100 MB)
        if (ext in VIDEO_EXTENSIONS) {
            return FileItem(file.name, path, size, mod, FileType.VIDEO)
        }

        // Documentos (PDFs, DOCXs, planilhas recebidos no celular)
        if (ext in DOCUMENT_EXTENSIONS) {
            return FileItem(file.name, path, size, mod, FileType.DOCUMENT)
        }

        return null
    }

    // ── Detecção de duplicatas ────────────────────────────────────────────

    /**
     * Encontra arquivos com conteúdo idêntico.
     * Estratégia: agrupar por tamanho (rápido) → calcular MD5 apenas nos grupos com 2+
     * arquivos do mesmo tamanho. Mantém o arquivo mais antigo como "original".
     */
    private fun findDuplicates(
        files: List<FileItem>,
        onProgress: (String) -> Unit
    ): List<FileItem> {
        // Filtra arquivos muito pequenos (< 10 KB não vale o custo)
        val candidates = files.filter { it.size >= 10 * 1024 }

        // Agrupar por tamanho
        val bySize = candidates
            .groupBy { it.size }
            .filter { it.value.size > 1 }

        if (bySize.isEmpty()) return emptyList()

        val duplicates = mutableListOf<FileItem>()
        var checked = 0
        val total = bySize.values.sumOf { it.size }

        for ((_, sameSize) in bySize) {
            val byHash = mutableMapOf<String, MutableList<FileItem>>()

            for (item in sameSize) {
                checked++
                if (checked % 5 == 0) {
                    onProgress("Comparando arquivos: $checked/$total")
                }
                val hash = md5(File(item.path)) ?: continue
                byHash.getOrPut(hash) { mutableListOf() }.add(item)
            }

            // Grupos com 2+ arquivos idênticos → todos menos o mais antigo são duplicatas
            for ((_, dups) in byHash.filter { it.value.size > 1 }) {
                val sorted = dups.sortedBy { it.lastModified }  // mais antigo = original
                duplicates.addAll(sorted.drop(1))               // os demais são duplicatas
            }
        }

        return duplicates
    }

    /** Calcula MD5 de um arquivo para comparação de conteúdo */
    private fun md5(file: File): String? {
        return try {
            if (!file.exists() || !file.canRead() || file.length() == 0L) return null
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().buffered(65536).use { stream ->
                val buffer = ByteArray(65536)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    // ── Deleção ───────────────────────────────────────────────────────────

    /** Exclui uma lista de arquivos/pastas e retorna (quantidade, espaço liberado) */
    suspend fun deleteFiles(files: List<FileItem>): Pair<Int, Long> =
        withContext(Dispatchers.IO) {
            var deleted = 0
            var freed   = 0L
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

    // ── Utilidades ────────────────────────────────────────────────────────

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

    fun getAppCachePath(packageName: String): File =
        File("/data/data/$packageName/cache")

    fun clearAppCacheViaCommand(packageName: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("pm", "clear", packageName))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
