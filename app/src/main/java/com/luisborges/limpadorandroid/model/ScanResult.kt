package com.luisborges.limpadorandroid.model

data class ScanResult(
    val totalScanned: Long = 0L,
    val totalJunk: Long = 0L,

    // Lixo automático (seguro deletar)
    val apkFiles: List<FileItem> = emptyList(),
    val tempFiles: List<FileItem> = emptyList(),
    val cacheFiles: List<FileItem> = emptyList(),
    val logFiles: List<FileItem> = emptyList(),
    val emptyFolders: List<FileItem> = emptyList(),
    val residualFiles: List<FileItem> = emptyList(),

    // Arquivos para revisar (ocupam espaço)
    val whatsappFiles: List<FileItem> = emptyList(),    // mídia recebida pelo WhatsApp
    val imageFiles: List<FileItem> = emptyList(),       // fotos e imagens
    val videoFiles: List<FileItem> = emptyList(),       // vídeos de todos os tamanhos
    val documentFiles: List<FileItem> = emptyList(),    // PDFs, DOCXs, planilhas, etc.
    val duplicateFiles: List<FileItem> = emptyList(),   // arquivos com conteúdo idêntico

    val unusedApps: List<AppItem> = emptyList()
) {
    val totalJunkFormatted: String get() = FileItem.formatSize(totalJunk)
    val totalScannedFormatted: String get() = FileItem.formatSize(totalScanned)

    /** Lixo automático (temp, cache, log, etc.) */
    val allJunkFiles: List<FileItem> get() =
        apkFiles + tempFiles + cacheFiles + logFiles + emptyFolders + residualFiles

    /** Arquivos que ocupam espaço e merecem atenção */
    val allReviewableFiles: List<FileItem> get() =
        whatsappFiles + imageFiles + videoFiles + documentFiles + duplicateFiles

    /** Total de espaço de arquivos revisáveis */
    val totalReviewable: Long get() = allReviewableFiles.sumOf { it.size }
    val totalReviewableFormatted: String get() = FileItem.formatSize(totalReviewable)

    /** Total de duplicatas */
    val totalDuplicatesSize: Long get() = duplicateFiles.sumOf { it.size }
    val totalDuplicatesSizeFormatted: String get() = FileItem.formatSize(totalDuplicatesSize)

    /** WhatsApp — total */
    val totalWhatsAppSize: Long get() = whatsappFiles.sumOf { it.size }
    val totalWhatsAppSizeFormatted: String get() = FileItem.formatSize(totalWhatsAppSize)
}

data class CleanResult(
    val filesDeleted: Int,
    val spaceFreed: Long,
    val errors: List<String> = emptyList()
) {
    val spaceFreedFormatted: String get() = FileItem.formatSize(spaceFreed)
}

enum class ScanState {
    IDLE, SCANNING, DONE, CLEANING, CLEANED, ERROR
}
