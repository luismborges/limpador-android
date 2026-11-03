package com.luisborges.limpadorandroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.luisborges.limpadorandroid.model.*
import com.luisborges.limpadorandroid.utils.AppUtils
import com.luisborges.limpadorandroid.utils.FileUtils
import com.luisborges.limpadorandroid.utils.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx = application.applicationContext

    // --- Estado do scan ---
    private val _scanState = MutableLiveData(ScanState.IDLE)
    val scanState: LiveData<ScanState> = _scanState

    private val _scanProgress = MutableLiveData("")
    val scanProgress: LiveData<String> = _scanProgress

    private val _scanResult = MutableLiveData<ScanResult>()
    val scanResult: LiveData<ScanResult> = _scanResult

    // --- Estado da limpeza ---
    private val _cleanResult = MutableLiveData<CleanResult>()
    val cleanResult: LiveData<CleanResult> = _cleanResult

    // --- Armazenamento ---
    private val _storageTotal = MutableLiveData(0L)
    val storageTotal: LiveData<Long> = _storageTotal

    private val _storageFree = MutableLiveData(0L)
    val storageFree: LiveData<Long> = _storageFree

    private val _storageUsed = MutableLiveData(0L)
    val storageUsed: LiveData<Long> = _storageUsed

    // --- RAM ---
    private val _ramTotal = MutableLiveData(0L)
    val ramTotal: LiveData<Long> = _ramTotal

    private val _ramFree = MutableLiveData(0L)
    val ramFree: LiveData<Long> = _ramFree

    // --- Lista de apps ---
    private val _appList = MutableLiveData<List<AppItem>>()
    val appList: LiveData<List<AppItem>> = _appList

    private val _rarelyUsedApps = MutableLiveData<List<AppItem>>()
    val rarelyUsedApps: LiveData<List<AppItem>> = _rarelyUsedApps

    private val _boostKilledCount = MutableLiveData(0)
    val boostKilledCount: LiveData<Int> = _boostKilledCount

    init {
        refreshStorageInfo()
        refreshRamInfo()
    }

    /** Atualiza informações de armazenamento */
    fun refreshStorageInfo() {
        viewModelScope.launch {
            val total = StorageUtils.getTotalStorage(ctx)
            val free = StorageUtils.getFreeStorage(ctx)
            _storageTotal.value = total
            _storageFree.value = free
            _storageUsed.value = total - free
        }
    }

    /** Atualiza informações de RAM */
    fun refreshRamInfo() {
        viewModelScope.launch {
            _ramTotal.value = AppUtils.getTotalRam(ctx)
            _ramFree.value = AppUtils.getAvailableRam(ctx)
        }
    }

    /** Varre o armazenamento completo */
    fun startScan() {
        if (_scanState.value == ScanState.SCANNING) return
        _scanState.value = ScanState.SCANNING
        _scanProgress.value = "Iniciando varredura..."

        viewModelScope.launch {
            try {
                val filesMap = FileUtils.scanStorage { msg ->
                    _scanProgress.postValue(msg)
                }

                val apkFiles     = filesMap[FileType.APK] ?: emptyList()
                val tempFiles    = filesMap[FileType.TEMP] ?: emptyList()
                val cacheFiles   = filesMap[FileType.CACHE] ?: emptyList()
                val logFiles     = filesMap[FileType.LOG] ?: emptyList()
                val emptyFolders = filesMap[FileType.EMPTY_FOLDER] ?: emptyList()
                val residual     = filesMap[FileType.RESIDUAL] ?: emptyList()
                val largeFiles   = filesMap[FileType.VIDEO] ?: emptyList()

                val allJunk = apkFiles + tempFiles + cacheFiles + logFiles + emptyFolders + residual
                val totalJunk = allJunk.sumOf { it.size }
                val totalScanned = withContext(Dispatchers.IO) {
                    StorageUtils.getUsedStorage(ctx)
                }

                val unusedApps = withContext(Dispatchers.IO) {
                    AppUtils.getRarelyUsedApps(ctx)
                }

                val result = ScanResult(
                    totalScanned = totalScanned,
                    totalJunk = totalJunk,
                    apkFiles = apkFiles,
                    tempFiles = tempFiles,
                    cacheFiles = cacheFiles,
                    logFiles = logFiles,
                    emptyFolders = emptyFolders,
                    residualFiles = residual,
                    largeFiles = largeFiles,
                    unusedApps = unusedApps
                )

                _scanResult.postValue(result)
                _scanState.postValue(ScanState.DONE)
            } catch (e: Exception) {
                _scanProgress.postValue("Erro: ${e.message}")
                _scanState.postValue(ScanState.ERROR)
            }
        }
    }

    /** Limpa todos os arquivos lixo detectados */
    fun cleanAllJunk() {
        val result = _scanResult.value ?: return
        _scanState.value = ScanState.CLEANING

        viewModelScope.launch {
            val allFiles = result.allJunkFiles
            val (deleted, freed) = FileUtils.deleteFiles(allFiles)

            _cleanResult.postValue(
                CleanResult(filesDeleted = deleted, spaceFreed = freed)
            )
            _scanState.postValue(ScanState.CLEANED)
            refreshStorageInfo()
        }
    }

    /** Limpa apenas a seleção de arquivos */
    fun cleanSelectedFiles(files: List<FileItem>) {
        viewModelScope.launch {
            val (deleted, freed) = FileUtils.deleteFiles(files)
            _cleanResult.postValue(CleanResult(filesDeleted = deleted, spaceFreed = freed))
            refreshStorageInfo()
        }
    }

    /** Carrega lista de apps instalados */
    fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                AppUtils.getInstalledApps(ctx)
            }
            _appList.postValue(apps)

            val rarely = apps.filter { it.isRarelyUsed && !it.isSystemApp }
            _rarelyUsedApps.postValue(rarely)
        }
    }

    /** Boost: encerra processos em background */
    fun boostPerformance() {
        viewModelScope.launch {
            val killed = withContext(Dispatchers.IO) {
                AppUtils.killBackgroundProcesses(ctx)
            }
            _boostKilledCount.postValue(killed)
            refreshRamInfo()
        }
    }

    /** Desinstala um app */
    fun uninstallApp(packageName: String) {
        AppUtils.openUninstallScreen(ctx, packageName)
    }

    /** Abre configurações de um app */
    fun openAppSettings(packageName: String) {
        AppUtils.openAppSettings(ctx, packageName)
    }

    /** Reseta o estado para nova varredura */
    fun resetScan() {
        _scanState.value = ScanState.IDLE
        _scanResult.value = null
    }
}
