package com.example

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter untuk menampilkan Folder Kategori Aplikasi di Dock Bawah Android TV.
 */
class FolderAdapter(
    private val context: Context,
    private var folderList: List<AppFolder>,
    private val onFolderClick: (AppFolder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    fun updateFolders(newFolders: List<AppFolder>) {
        this.folderList = newFolders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folderList[position]
        holder.bind(folder, onFolderClick)
    }

    override fun getItemCount(): Int = folderList.size

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardFolder: FrameLayout = itemView.findViewById(R.id.cardFolder)
        private val ivFolderIcon: ImageView = itemView.findViewById(R.id.ivFolderIcon)
        private val tvFolderName: TextView = itemView.findViewById(R.id.tvFolderName)
        private val tvFolderBadge: TextView = itemView.findViewById(R.id.tvFolderBadge)

        fun bind(folder: AppFolder, onClick: (AppFolder) -> Unit) {
            tvFolderName.text = folder.name
            tvFolderBadge.text = "${folder.packageNames.size}"

            // D-Pad Focus Handling untuk Remote Control Android TV STB
            itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
                    v.elevation = 12f
                    tvFolderName.isSelected = true
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    v.elevation = 0f
                    tvFolderName.isSelected = false
                }
            }

            itemView.setOnClickListener {
                onClick(folder)
            }
        }
    }
}
