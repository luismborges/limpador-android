package com.luisborges.limpadorandroid.model

data class AppItem(
    val packageName: String,
    val appName: String,
    val appSize: Long,        // bytes do APK instalado
    val dataSize: Long,       // dados do app
    val cacheSize: Long,      // cache do app
    val lastUsed: Long,       // timestamp última utilização
    val installTime: Long,    // timestamp instalação
    val isSystemApp: Boolean,
    var isSelected: Boolean = false
) {
    val totalSize: Long get() = appSize + dataSize + cacheSize
    val totalSizeFormatted: String get() = FileItem.formatSize(totalSize)
    val cacheSizeFormatted: String get() = FileItem.formatSize(cacheSize)
    val lastUsedFormatted: String get() {
        if (lastUsed == 0L) return "Nunca usado"
        val diff = System.currentTimeMillis() - lastUsed
        val days = diff / (1000 * 60 * 60 * 24)
        return when {
            days == 0L -> "Hoje"
            days == 1L -> "Ontem"
            days < 30  -> "Há $days dias"
            days < 365 -> "Há ${days / 30} meses"
            else       -> "Há mais de 1 ano"
        }
    }
    val isRarelyUsed: Boolean get() {
        if (lastUsed == 0L) return true
        val days = (System.currentTimeMillis() - lastUsed) / (1000L * 60 * 60 * 24)
        return days > 30
    }
}
