package com.luisborges.limpadorandroid.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.luisborges.limpadorandroid.adapter.AppAdapter
import com.luisborges.limpadorandroid.databinding.FragmentAppsBinding
import com.luisborges.limpadorandroid.viewmodel.CleanerViewModel

class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CleanerViewModel by activityViewModels()
    private lateinit var adapter: AppAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupChips()
        observeViewModel()
        viewModel.loadApps()
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(
            onUninstall = { app -> viewModel.uninstallApp(app.packageName) },
            onSettings  = { app -> viewModel.openAppSettings(app.packageName) }
        )
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.adapter = adapter
    }

    private fun setupChips() {
        binding.chipAll.setOnClickListener {
            val all = viewModel.appList.value ?: return@setOnClickListener
            adapter.submitList(all.filter { !it.isSystemApp })
        }
        binding.chipRarely.setOnClickListener {
            adapter.submitList(viewModel.rarelyUsedApps.value ?: emptyList())
        }
        binding.chipLarge.setOnClickListener {
            val large = viewModel.appList.value
                ?.filter { !it.isSystemApp && it.appSize > 50 * 1024 * 1024L }
                ?: emptyList()
            adapter.submitList(large)
        }
    }

    private fun observeViewModel() {
        viewModel.appList.observe(viewLifecycleOwner) { apps ->
            val userApps = apps.filter { !it.isSystemApp }
            adapter.submitList(userApps)
            binding.tvAppCount.text = "${userApps.size} apps instalados"
            binding.progressLoading.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
