package com.luisborges.limpadorandroid.utils

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.luisborges.limpadorandroid.model.AppItem

object AppUtils {

    /** Lista todos os apps instalados pelo usuário (não-sistema) com métricas */
    fun getInstalledApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val usageMap = getUsageStats(context)

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }

        return packages.mapNotNull { appInfo ->
            try {
                val packageName = appInfo.packageName
                // Exclui o próprio app
                if (packageName == context.packageName) return@mapNotNull null

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, 0)
                }

                val lastUsed = usageMap[packageName] ?: 0L
                val appLabel = pm.getApplicationLabel(appInfo).toString()

                // Tamanho do APK
                val apkSize = try {
                    java.io.File(appInfo.sourceDir).length()
                } catch (e: Exception) { 0L }

                AppItem(
                    packageName = packageName,
                    appName = appLabel,
                    appSize = apkSize,
                    dataSize = 0L, // Requer acesso root para calcular exato
                    cacheSize = getCacheSize(packageName),
                    lastUsed = lastUsed,
                    installTime = packageInfo.firstInstallTime,
                    isSystemApp = isSystem
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.appSize }
    }

    /** Retorna os apps raramente usados (>30 dias sem abrir) */
    fun getRarelyUsedApps(context: Context): List<AppItem> {
        return getInstalledApps(context).filter {
            !it.isSystemApp && it.isRarelyUsed
        }
    }

    /** Retorna apps grandes (>50MB) */
    fun getLargeApps(context: Context): List<AppItem> {
        return getInstalledApps(context).filter {
            it.appSize > 50 * 1024 * 1024L
        }
    }

    /** Mapa de packageName -> último uso (ms) via UsageStats */
    private fun getUsageStats(context: Context): Map<String, Long> {
        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000L * 60 * 60 * 24 * 365 // último ano
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, startTime, endTime
            )
            stats?.associate { it.packageName to it.lastTimeUsed } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** Estima o tamanho do cache de um app */
    private fun getCacheSize(packageName: String): Long {
        return try {
            val cacheDir = java.io.File("/data/data/$packageName/cache")
            if (cacheDir.exists()) FileUtils.calculateDirSize(cacheDir) else 0L
        } catch (e: Exception) {
            0L
        }
    }

    /** Abre a tela de desinstalação do sistema para um app */
    fun openUninstallScreen(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** Abre as informações do app nas Configurações do sistema */
    fun openAppSettings(context: Context, packageName: String) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** Encerra processos em background (não root — efeito moderado) */
    fun killBackgroundProcesses(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return 0
        var killed = 0
        for (process in runningApps) {
            if (process.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                try {
                    am.killBackgroundProcesses(process.processName)
                    killed++
                } catch (e: Exception) { /* ignora */ }
            }
        }
        return killed
    }

    /** Memória RAM disponível em bytes */
    fun getAvailableRam(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem
    }

    /** Memória RAM total em bytes */
    fun getTotalRam(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem
    }
}
