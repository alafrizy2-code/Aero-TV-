package com.example

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter untuk menampilkan aplikasi di dalam folder serta kartu "+" untuk menambah aplikasi baru.
 */
class FolderAppsAdapter(
    private val context: Context,
    private var appsInFolder: List<AppModel>,
    private val onAddClick: () -> Unit,
    private val onAppClick: (AppModel) -> Unit,
    private val onAppLongClick: (AppModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ADD = 0
        private const val TYPE_APP = 1
    }

    fun updateApps(newApps: List<AppModel>) {
        this.appsInFolder = newApps
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_ADD else TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_app, parent, false)
        return if (viewType == TYPE_ADD) {
            AddViewHolder(view)
        } else {
            AppItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AddViewHolder) {
            holder.bind(onAddClick)
        } else if (holder is AppItemViewHolder) {
            val app = appsInFolder[position - 1]
            holder.bind(app, onAppClick, onAppLongClick)
        }
    }

    override fun getItemCount(): Int = appsInFolder.size + 1

    class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardApp: FrameLayout = itemView.findViewById(R.id.cardApp)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)

        fun bind(onAdd: () -> Unit) {
            tvAppName.text = "Tambah App"
            tvAppName.setTextColor(Color.parseColor("#C084FC"))
            ivAppIcon.setImageResource(R.drawable.ic_add)

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
                onAdd()
            }
        }
    }

    class AppItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardApp: FrameLayout = itemView.findViewById(R.id.cardApp)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)

        fun bind(
            app: AppModel,
            onClick: (AppModel) -> Unit,
            onLongClick: (AppModel) -> Unit
        ) {
            tvAppName.text = app.appName
            tvAppName.setTextColor(Color.WHITE)
            ivAppIcon.setImageDrawable(app.icon)

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

            itemView.setOnLongClickListener {
                onLongClick(app)
                true
            }
        }
    }
}
