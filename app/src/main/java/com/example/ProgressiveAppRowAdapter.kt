package com.example

import android.content.Context
import android.graphics.Color
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter untuk Satu Baris Aplikasi Tambahan (Progressive App Row).
 * - Menampilkan maksimal 8 aplikasi per baris dalam susunan horizontal langsung di atas wallpaper.
 * - Google TV Focus Highlight: scale 1.10x, elevasi 8dp, border putih bersih netral tanpa glow neon.
 * - Navigasi remote TV:
 *     - DOWN: Memicu penambahan satu baris aplikasi berikutnya.
 *     - UP: Memicu penyembunyian satu baris aplikasi.
 *     - LEFT/RIGHT: Berpindah antarikon dalam baris.
 *     - OK: Membuka aplikasi terpilih.
 *     - MENU / Long click: Opsi jadikan favorit.
 */
class ProgressiveAppRowAdapter(
    private val context: Context,
    private val rowIndex: Int,
    private var apps: List<AppModel>,
    private val onAppClick: (AppModel) -> Unit,
    private val onAppLongClick: (AppModel) -> Unit,
    private val onDownPressed: (rowIndex: Int, columnIndex: Int) -> Unit,
    private val onUpPressed: (rowIndex: Int, columnIndex: Int) -> Unit
) : RecyclerView.Adapter<ProgressiveAppRowAdapter.AppRowViewHolder>() {

    fun updateApps(newApps: List<AppModel>) {
        this.apps = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppRowViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppRowViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppRowViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(
            app = app,
            rowIndex = rowIndex,
            columnIndex = position,
            onAppClick = onAppClick,
            onAppLongClick = onAppLongClick,
            onDownPressed = onDownPressed,
            onUpPressed = onUpPressed
        )
    }

    override fun getItemCount(): Int = apps.size

    class AppRowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardApp: FrameLayout = itemView.findViewById(R.id.cardApp)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvSlotBadge: TextView = itemView.findViewById(R.id.tvSlotBadge)

        fun bind(
            app: AppModel,
            rowIndex: Int,
            columnIndex: Int,
            onAppClick: (AppModel) -> Unit,
            onAppLongClick: (AppModel) -> Unit,
            onDownPressed: (Int, Int) -> Unit,
            onUpPressed: (Int, Int) -> Unit
        ) {
            tvSlotBadge.visibility = View.GONE
            tvAppName.text = app.appName
            tvAppName.setTextColor(Color.WHITE)
            ivAppIcon.setImageDrawable(app.icon)
            ivAppIcon.alpha = 1.0f
            cardApp.setBackgroundResource(R.drawable.bg_app_card)

            itemView.setOnClickListener {
                onAppClick(app)
            }

            itemView.setOnLongClickListener {
                onAppLongClick(app)
                true
            }

            // Remote D-pad Key Handling khusus Android TV
            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            onDownPressed(rowIndex, columnIndex)
                            return@setOnKeyListener true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            onUpPressed(rowIndex, columnIndex)
                            return@setOnKeyListener true
                        }
                        KeyEvent.KEYCODE_MENU -> {
                            onAppLongClick(app)
                            return@setOnKeyListener true
                        }
                    }
                }
                false
            }

            // Google TV Focus Highlight: scale 1.15x, elevasi 12dp, nama BOLD + background pill transparan
            itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(160)
                        .start()
                    v.elevation = 12f
                    tvAppName.isSelected = true
                    tvAppName.setTypeface(null, android.graphics.Typeface.BOLD)
                    tvAppName.setBackgroundResource(R.drawable.bg_pill_focus_title)
                    tvAppName.setTextColor(Color.WHITE)
                } else {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(160)
                        .start()
                    v.elevation = 0f
                    tvAppName.isSelected = false
                    tvAppName.setTypeface(null, android.graphics.Typeface.NORMAL)
                    tvAppName.background = null
                    tvAppName.setTextColor(Color.WHITE)
                }
            }
        }
    }
}
