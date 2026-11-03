package com.luisborges.limpadorandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.luisborges.limpadorandroid.databinding.ItemAppBinding
import com.luisborges.limpadorandroid.model.AppItem

class AppAdapter(
    private val onUninstall: (AppItem) -> Unit,
    private val onSettings:  (AppItem) -> Unit
) : ListAdapter<AppItem, AppAdapter.AppViewHolder>(DIFF) {

    inner class AppViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppItem) {
            binding.tvAppName.text = app.appName
            binding.tvAppSize.text = "Tamanho: ${app.totalSizeFormatted}"
            binding.tvLastUsed.text = "Último uso: ${app.lastUsedFormatted}"
            binding.tvPackage.text = app.packageName

            // Carrega ícone do app
            try {
                val icon = binding.root.context.packageManager
                    .getApplicationIcon(app.packageName)
                binding.ivAppIcon.setImageDrawable(icon)
            } catch (e: Exception) {
                binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            // Tag de raramente usado
            binding.tvRareTag.visibility =
                if (app.isRarelyUsed) android.view.View.VISIBLE else android.view.View.GONE

            binding.btnUninstall.setOnClickListener { onUninstall(app) }
            binding.btnSettings.setOnClickListener  { onSettings(app) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppItem>() {
            override fun areItemsTheSame(a: AppItem, b: AppItem) =
                a.packageName == b.packageName
            override fun areContentsTheSame(a: AppItem, b: AppItem) = a == b
        }
    }
}
