package com.example

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

enum class AppCategory(val label: String) {
    ALL("Semua"),
    MEDIA_VIDEO("Video & TV"),
    MUSIC_AUDIO("Musik & Audio"),
    GAMES("Game"),
    TOOLS_SYSTEM("Alat & Sistem")
}

data class AppModel(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val launchIntent: Intent?,
    val banner: Drawable? = null,
    val category: AppCategory = AppCategory.TOOLS_SYSTEM,
    val isTvApp: Boolean = false
)

class AppAdapter(
    private val context: Context,
    private var appList: List<AppModel>,
    private val onItemClick: (AppModel) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    fun updateApps(newApps: List<AppModel>) {
        this.appList = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        holder.bind(app, onItemClick)
    }

    override fun getItemCount(): Int = appList.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardApp: FrameLayout = itemView.findViewById(R.id.cardApp)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)

        fun bind(app: AppModel, onClick: (AppModel) -> Unit) {
            tvAppName.text = app.appName
            ivAppIcon.setImageDrawable(app.icon)

            // D-Pad Focus Handling untuk Remote Control B866F Android TV
            itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
                    v.elevation = 12f
                    tvAppName.isSelected = true
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    v.elevation = 0f
                    tvAppName.isSelected = false
                }
            }

            itemView.setOnClickListener {
                onClick(app)
            }
        }
    }
}
