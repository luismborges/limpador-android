package com.luisborges.limpadorandroid.model

data class ScanResult(
    val totalScanned: Long = 0L,
    val totalJunk: Long = 0L,
    val apkFiles: List<FileItem> = emptyList(),
    val tempFiles: List<FileItem> = emptyList(),
    val cacheFiles: List<FileItem> = emptyList(),
    val logFiles: List<FileItem> = emptyList(),
    val emptyFolders: List<FileItem> = emptyList(),
    val residualFiles: List<FileItem> = emptyList(),
    val largeFiles: List<FileItem> = emptyList(),
    val unusedApps: List<AppItem> = emptyList()
) {
    val totalJunkFormatted: String get() = FileItem.formatSize(totalJunk)
    val totalScannedFormatted: String get() = FileItem.formatSize(totalScanned)

    val allJunkFiles: List<FileItem> get() =
        apkFiles + tempFiles + cacheFiles + logFiles + emptyFolders + residualFiles
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
