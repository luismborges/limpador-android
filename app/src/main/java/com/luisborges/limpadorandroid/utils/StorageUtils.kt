package com.luisborges.limpadorandroid.utils

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.luisborges.limpadorandroid.model.FileItem

object StorageUtils {

    /** Espaço total do armazenamento interno em bytes */
    fun getTotalStorage(context: Context): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val storageManager = context.getSystemService(StorageManager::class.java)
            val storageVolumes = storageManager.storageVolumes
            val primaryVolume = storageVolumes.firstOrNull { it.isPrimary }
            try {
                val storageStatsManager =
                    context.getSystemService(StorageStatsManager::class.java)
                val uuid = primaryVolume?.uuid?.let {
                    java.util.UUID.fromString(it)
                } ?: StorageManager.UUID_DEFAULT
                storageStatsManager.getTotalBytes(uuid)
            } catch (e: Exception) {
                StatFs(Environment.getDataDirectory().path).totalBytes
            }
        } else {
            StatFs(Environment.getDataDirectory().path).totalBytes
        }
    }

    /** Espaço disponível em bytes */
    fun getFreeStorage(context: Context): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val storageManager = context.getSystemService(StorageManager::class.java)
            try {
                storageManager.getAllocatableBytes(StorageManager.UUID_DEFAULT)
            } catch (e: Exception) {
                StatFs(Environment.getDataDirectory().path).availableBytes
            }
        } else {
            StatFs(Environment.getDataDirectory().path).availableBytes
        }
    }

    /** Espaço usado em bytes */
    fun getUsedStorage(context: Context): Long =
        getTotalStorage(context) - getFreeStorage(context)

    /** Porcentagem usada (0.0 a 1.0) */
    fun getUsedPercent(context: Context): Float {
        val total = getTotalStorage(context)
        if (total == 0L) return 0f
        return getUsedStorage(context).toFloat() / total.toFloat()
    }

    /** Caminho do armazenamento externo (cartão SD), se disponível */
    fun getExternalStoragePath(context: Context): String? {
        val externalDirs = context.getExternalFilesDirs(null)
        return if (externalDirs.size > 1 && externalDirs[1] != null) {
            externalDirs[1].absolutePath
        } else null
    }

    /** Tamanho total de uma lista de arquivos */
    fun sumFileSize(files: List<FileItem>): Long =
        files.sumOf { it.size }
}
