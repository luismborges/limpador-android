package com.luisborges.limpadorandroid.model

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,           // bytes
    val lastModified: Long,   // timestamp millis
    val type: FileType,
    var isSelected: Boolean = false
) {
    val sizeFormatted: String get() = formatSize(size)

    companion object {
        fun formatSize(bytes: Long): String {
            return when {
                bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
                bytes >= 1_048_576L    -> "%.1f MB".format(bytes / 1_048_576.0)
                bytes >= 1_024L        -> "%.0f KB".format(bytes / 1_024.0)
                else                   -> "$bytes B"
            }
        }
    }
}

enum class FileType {
    APK,            // APKs baixados
    CACHE,          // Caches de apps
    TEMP,           // Arquivos temporários
    THUMBNAIL,      // Miniaturas antigas
    EMPTY_FOLDER,   // Pastas vazias
    DUPLICATE,      // Arquivos duplicados (mesmo conteúdo)
    LOG,            // Arquivos de log
    RESIDUAL,       // Dados residuais de apps desinstalados
    VIDEO,          // Vídeos (todos os tamanhos)
    IMAGE,          // Imagens (jpg, png, gif, webp…)
    WHATSAPP_MEDIA, // Mídias recebidas pelo WhatsApp
    DOCUMENT,       // Documentos (pdf, docx, xlsx…)
    OTHER
}
