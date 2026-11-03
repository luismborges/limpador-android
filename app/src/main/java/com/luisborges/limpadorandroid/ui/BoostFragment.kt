package com.luisborges.limpadorandroid.ui

import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.luisborges.limpadorandroid.R
import com.luisborges.limpadorandroid.databinding.FragmentBoostBinding
import com.luisborges.limpadorandroid.model.FileItem
import com.luisborges.limpadorandroid.viewmodel.CleanerViewModel

class BoostFragment : Fragment() {

    private var _binding: FragmentBoostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CleanerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        viewModel.refreshRamInfo()

        binding.btnBoost.setOnClickListener {
            val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.boost_pulse)
            binding.btnBoost.startAnimation(anim)
            viewModel.boostPerformance()
            binding.tvBoostStatus.text = "Otimizando..."
            binding.tvBoostStatus.visibility = View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewModel.ramTotal.observe(viewLifecycleOwner) { updateRamUI() }
        viewModel.ramFree.observe(viewLifecycleOwner)  { updateRamUI() }

        viewModel.boostKilledCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.tvBoostStatus.text =
                    "✅ $count processos encerrados!\nRAM liberada com sucesso."
            }
        }
    }

    private fun updateRamUI() {
        val total = viewModel.ramTotal.value ?: return
        val free  = viewModel.ramFree.value  ?: return
        val used  = total - free
        if (total > 0) {
            val pct = (used * 100 / total).toInt()
            binding.progressRam.progress = pct
            binding.tvRamUsed.text = "${FileItem.formatSize(used)} em uso"
            binding.tvRamFree.text = "${FileItem.formatSize(free)} disponíveis"
            binding.tvRamTotal.text = "Total: ${FileItem.formatSize(total)}"
            binding.tvRamPercent.text = "$pct%"

            // Cor do progresso conforme carga
            val color = when {
                pct > 85 -> requireContext().getColor(R.color.red_warning)
                pct > 60 -> requireContext().getColor(R.color.yellow_caution)
                else     -> requireContext().getColor(R.color.green_ok)
            }
            binding.progressRam.setIndicatorColor(color)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
