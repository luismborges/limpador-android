package com.luisborges.limpadorandroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.luisborges.limpadorandroid.databinding.FragmentHomeBinding
import com.luisborges.limpadorandroid.model.FileItem
import com.luisborges.limpadorandroid.model.ScanState
import com.luisborges.limpadorandroid.utils.StorageUtils
import com.luisborges.limpadorandroid.viewmodel.CleanerViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CleanerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
        viewModel.refreshStorageInfo()
        viewModel.refreshRamInfo()
    }

    private fun setupClickListeners() {
        binding.btnScan.setOnClickListener {
            if (viewModel.scanState.value == ScanState.IDLE ||
                viewModel.scanState.value == ScanState.CLEANED ||
                viewModel.scanState.value == ScanState.ERROR) {
                viewModel.startScan()
            }
        }

        binding.btnClean.setOnClickListener {
            viewModel.cleanAllJunk()
        }

        binding.btnScanAgain.setOnClickListener {
            viewModel.resetScan()
            viewModel.startScan()
        }
    }

    private fun observeViewModel() {
        viewModel.scanState.observe(viewLifecycleOwner) { state ->
            updateUIForState(state)
        }

        viewModel.scanProgress.observe(viewLifecycleOwner) { msg ->
            binding.tvProgress.text = msg
        }

        viewModel.scanResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            binding.tvJunkSize.text = result.totalJunkFormatted
            binding.tvJunkFiles.text = "${result.allJunkFiles.size} arquivos desnecessários"

            val breakdown = buildString {
                if (result.apkFiles.isNotEmpty())
                    appendLine("APKs: ${result.apkFiles.size} (${FileItem.formatSize(result.apkFiles.sumOf { it.size })})")
                if (result.cacheFiles.isNotEmpty())
                    appendLine("Cache: ${result.cacheFiles.size} (${FileItem.formatSize(result.cacheFiles.sumOf { it.size })})")
                if (result.tempFiles.isNotEmpty())
                    appendLine("Temp: ${result.tempFiles.size} (${FileItem.formatSize(result.tempFiles.sumOf { it.size })})")
                if (result.logFiles.isNotEmpty())
                    appendLine("Logs: ${result.logFiles.size} (${FileItem.formatSize(result.logFiles.sumOf { it.size })})")
                if (result.emptyFolders.isNotEmpty())
                    appendLine("Pastas vazias: ${result.emptyFolders.size}")
                if (result.residualFiles.isNotEmpty())
                    appendLine("Residuais: ${result.residualFiles.size} (${FileItem.formatSize(result.residualFiles.sumOf { it.size })})")
            }
            binding.tvBreakdown.text = breakdown.trim()
        }

        viewModel.cleanResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            binding.tvCleanResult.text =
                "✅ ${result.filesDeleted} arquivos deletados • ${result.spaceFreedFormatted} liberados"
            binding.tvCleanResult.visibility = View.VISIBLE
        }

        viewModel.storageTotal.observe(viewLifecycleOwner) { updateStorageBar() }
        viewModel.storageFree.observe(viewLifecycleOwner) { updateStorageBar() }

        viewModel.ramTotal.observe(viewLifecycleOwner) { updateRamBar() }
        viewModel.ramFree.observe(viewLifecycleOwner) { updateRamBar() }
    }

    private fun updateStorageBar() {
        val total = viewModel.storageTotal.value ?: return
        val free = viewModel.storageFree.value ?: return
        val used = total - free
        if (total > 0) {
            val percent = (used * 100 / total).toInt()
            binding.progressStorage.progress = percent
            binding.tvStorageLabel.text =
                "Armazenamento: ${FileItem.formatSize(used)} / ${FileItem.formatSize(total)} ($percent% usado)"
            binding.tvStorageFree.text = "${FileItem.formatSize(free)} disponíveis"
        }
    }

    private fun updateRamBar() {
        val total = viewModel.ramTotal.value ?: return
        val free = viewModel.ramFree.value ?: return
        val used = total - free
        if (total > 0) {
            val percent = (used * 100 / total).toInt()
            binding.progressRam.progress = percent
            binding.tvRamLabel.text =
                "RAM: ${FileItem.formatSize(used)} / ${FileItem.formatSize(total)} ($percent% em uso)"
        }
    }

    private fun updateUIForState(state: ScanState) {
        binding.tvCleanResult.visibility = View.GONE
        when (state) {
            ScanState.IDLE -> {
                binding.btnScan.visibility = View.VISIBLE
                binding.btnScan.text = "Varrer Agora"
                binding.btnClean.visibility = View.GONE
                binding.btnScanAgain.visibility = View.GONE
                binding.layoutResult.visibility = View.GONE
                binding.layoutScanning.visibility = View.GONE
            }
            ScanState.SCANNING -> {
                binding.btnScan.visibility = View.GONE
                binding.btnClean.visibility = View.GONE
                binding.btnScanAgain.visibility = View.GONE
                binding.layoutResult.visibility = View.GONE
                binding.layoutScanning.visibility = View.VISIBLE
            }
            ScanState.DONE -> {
                binding.btnScan.visibility = View.GONE
                binding.btnClean.visibility = View.VISIBLE
                binding.btnScanAgain.visibility = View.GONE
                binding.layoutResult.visibility = View.VISIBLE
                binding.layoutScanning.visibility = View.GONE
            }
            ScanState.CLEANING -> {
                binding.btnClean.isEnabled = false
                binding.btnClean.text = "Limpando..."
            }
            ScanState.CLEANED -> {
                binding.btnClean.visibility = View.GONE
                binding.btnScanAgain.visibility = View.VISIBLE
                binding.tvCleanResult.visibility = View.VISIBLE
            }
            ScanState.ERROR -> {
                binding.btnScan.visibility = View.VISIBLE
                binding.btnScan.text = "Tentar novamente"
                binding.layoutScanning.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
