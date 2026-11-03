package com.luisborges.limpadorandroid.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.luisborges.limpadorandroid.adapter.FileAdapter
import com.luisborges.limpadorandroid.databinding.FragmentFilesBinding
import com.luisborges.limpadorandroid.model.FileItem
import com.luisborges.limpadorandroid.model.FileType
import com.luisborges.limpadorandroid.model.ScanResult
import com.luisborges.limpadorandroid.viewmodel.CleanerViewModel

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CleanerViewModel by activityViewModels()

    private lateinit var adapter: FileAdapter
    private var currentFilter = FileType.WHATSAPP_MEDIA

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupChips()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = FileAdapter { item ->
            item.isSelected = !item.isSelected
            updateDeleteButton()
        }
        binding.rvFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFiles.adapter = adapter
    }

    private fun setupChips() {
        // Lixo automático
        binding.chipApk.setOnClickListener      { setFilter(FileType.APK) }
        binding.chipCache.setOnClickListener    { setFilter(FileType.CACHE) }
        binding.chipTemp.setOnClickListener     { setFilter(FileType.TEMP) }
        binding.chipLog.setOnClickListener      { setFilter(FileType.LOG) }
        binding.chipResidual.setOnClickListener { setFilter(FileType.RESIDUAL) }
        binding.chipEmpty.setOnClickListener    { setFilter(FileType.EMPTY_FOLDER) }

        // Novos: arquivos que ocupam espaço
        binding.chipWhatsapp.setOnClickListener   { setFilter(FileType.WHATSAPP_MEDIA) }
        binding.chipImages.setOnClickListener     { setFilter(FileType.IMAGE) }
        binding.chipVideos.setOnClickListener     { setFilter(FileType.VIDEO) }
        binding.chipDocuments.setOnClickListener  { setFilter(FileType.DOCUMENT) }
        binding.chipDuplicates.setOnClickListener { setFilter(FileType.DUPLICATE) }

        binding.btnDeleteSelected.setOnClickListener {
            val selected = adapter.currentList.filter { it.isSelected }
            viewModel.cleanSelectedFiles(selected)
        }
    }

    private fun setFilter(type: FileType) {
        currentFilter = type
        refreshList()
    }

    private fun getFilesForFilter(result: ScanResult, filter: FileType): List<FileItem> {
        return when (filter) {
            FileType.APK            -> result.apkFiles
            FileType.CACHE          -> result.cacheFiles
            FileType.TEMP           -> result.tempFiles
            FileType.LOG            -> result.logFiles
            FileType.RESIDUAL       -> result.residualFiles
            FileType.EMPTY_FOLDER   -> result.emptyFolders
            FileType.WHATSAPP_MEDIA -> result.whatsappFiles
                .sortedByDescending { it.size }
            FileType.IMAGE          -> result.imageFiles
                .sortedByDescending { it.size }
            FileType.VIDEO          -> result.videoFiles
                .sortedByDescending { it.size }
            FileType.DOCUMENT       -> result.documentFiles
                .sortedByDescending { it.size }
            FileType.DUPLICATE      -> result.duplicateFiles
                .sortedByDescending { it.size }
            else                    -> emptyList()
        }
    }

    private fun refreshList() {
        val result = viewModel.scanResult.value ?: return
        val files = getFilesForFilter(result, currentFilter)

        binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        adapter.submitList(files.toMutableList())
        updateDeleteButton()
    }

    private fun updateDeleteButton() {
        val selected = adapter.currentList.filter { it.isSelected }
        binding.btnDeleteSelected.isEnabled = selected.isNotEmpty()
        if (selected.isNotEmpty()) {
            val size = selected.sumOf { it.size }
            binding.btnDeleteSelected.text =
                "Excluir ${selected.size} selecionados (${FileItem.formatSize(size)})"
        } else {
            binding.btnDeleteSelected.text = "Selecione arquivos para excluir"
        }
    }

    private fun observeViewModel() {
        viewModel.scanResult.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.tvScanFirst.visibility = View.GONE
                refreshList()
            } else {
                binding.tvScanFirst.visibility = View.VISIBLE
                adapter.submitList(emptyList())
            }
        }
        viewModel.cleanResult.observe(viewLifecycleOwner) {
            refreshList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
