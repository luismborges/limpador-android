package com.luisborges.limpadorandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.luisborges.limpadorandroid.databinding.ItemFileBinding
import com.luisborges.limpadorandroid.model.FileItem

class FileAdapter(
    private val onItemClick: (FileItem) -> Unit
) : ListAdapter<FileItem, FileAdapter.FileViewHolder>(DIFF) {

    inner class FileViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FileItem) {
            binding.tvFileName.text = item.name
            binding.tvFilePath.text = item.path
            binding.tvFileSize.text = item.sizeFormatted
            binding.tvFileType.text = item.type.name

            binding.checkbox.isChecked = item.isSelected
            binding.root.isSelected = item.isSelected

            val bg = if (item.isSelected)
                binding.root.context.getColor(android.R.color.holo_blue_light)
            else
                android.graphics.Color.TRANSPARENT
            binding.root.setBackgroundColor(bg)

            binding.root.setOnClickListener {
                onItemClick(item)
                notifyItemChanged(adapterPosition)
            }

            // Ícone por tipo
            val iconRes = when (item.type.name) {
                "APK"          -> android.R.drawable.ic_menu_upload
                "CACHE"        -> android.R.drawable.ic_delete
                "TEMP"         -> android.R.drawable.ic_menu_recent_history
                "LOG"          -> android.R.drawable.ic_menu_edit
                "EMPTY_FOLDER" -> android.R.drawable.ic_menu_more
                "RESIDUAL"     -> android.R.drawable.ic_menu_delete
                "VIDEO"        -> android.R.drawable.ic_media_play
                else           -> android.R.drawable.ic_menu_help
            }
            binding.ivFileIcon.setImageResource(iconRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FileItem>() {
            override fun areItemsTheSame(a: FileItem, b: FileItem) = a.path == b.path
            override fun areContentsTheSame(a: FileItem, b: FileItem) =
                a == b && a.isSelected == b.isSelected
        }
    }
}
