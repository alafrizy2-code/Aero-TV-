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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter untuk 8 Slot Aplikasi Favorit:
 * - Selalu menampilkan tepat 8 slot favorit dalam satu baris langsung di atas wallpaper (tanpa background).
 * - Mendukung slot terisi dan slot kosong (+).
 * - Google TV Focus Highlight: scale 1.10x, elevation 8dp, border netral putih bersih tanpa neon.
 * - Navigasi remote TV:
 *     - DOWN: Memicu pemunculan baris aplikasi tambahan pertama tepat di atas dock favorit.
 *     - LEFT/RIGHT: Berpindah antar slot favorit.
 *     - OK: Membuka aplikasi (atau memilih aplikasi jika slot kosong).
 *     - MENU / Long click: Opsi pengaturan slot (ganti, hapus, tukar urutan).
 */
class FavoritesAdapter(
    private val context: Context,
    private var slots: List<FavoriteSlot>,
    private val onAppClick: (AppModel) -> Unit,
    private val onEmptySlotClick: (Int) -> Unit,
    private val onSlotManage: (FavoriteSlot) -> Unit,
    private val onDownPressed: (slotIndex: Int) -> Unit,
    private val onUpPressed: ((slotIndex: Int) -> Boolean)? = null
) : RecyclerView.Adapter<FavoritesAdapter.SlotViewHolder>() {

    fun updateSlots(newSlots: List<FavoriteSlot>) {
        this.slots = newSlots
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]
        holder.bind(
            slot = slot,
            onAppClick = onAppClick,
            onEmptySlotClick = onEmptySlotClick,
            onSlotManage = onSlotManage,
            onDownPressed = onDownPressed,
            onUpPressed = onUpPressed
        )
    }

    override fun getItemCount(): Int = slots.size

    class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardApp: FrameLayout = itemView.findViewById(R.id.cardApp)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvSlotBadge: TextView = itemView.findViewById(R.id.tvSlotBadge)

        fun bind(
            slot: FavoriteSlot,
            onAppClick: (AppModel) -> Unit,
            onEmptySlotClick: (Int) -> Unit,
            onSlotManage: (FavoriteSlot) -> Unit,
            onDownPressed: (Int) -> Unit,
            onUpPressed: ((Int) -> Boolean)?
        ) {
            val app = slot.appModel

            tvSlotBadge.visibility = View.VISIBLE
            tvSlotBadge.text = "#${slot.index + 1}"

            if (app != null) {
                // Slot Terisi Aplikasi
                tvAppName.text = app.appName
                tvAppName.setTextColor(Color.WHITE)
                ivAppIcon.setImageDrawable(app.icon)
                ivAppIcon.alpha = 1.0f
                cardApp.setBackgroundResource(R.drawable.bg_app_card)

                itemView.setOnClickListener {
                    onAppClick(app)
                }
            } else {
                // Slot Kosong (+)
                tvAppName.text = "+ Tambah"
                tvAppName.setTextColor(Color.parseColor("#CBD5E1"))
                val addDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_add)
                ivAppIcon.setImageDrawable(addDrawable)
                ivAppIcon.alpha = 0.75f
                cardApp.setBackgroundResource(R.drawable.bg_app_card_empty)

                itemView.setOnClickListener {
                    onEmptySlotClick(slot.index)
                }
            }

            // Long Click / Menu: Pengelolaan Slot Favorit
            itemView.setOnLongClickListener {
                onSlotManage(slot)
                true
            }

            // Remote D-pad Key Handling
            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            onDownPressed(slot.index)
                            return@setOnKeyListener true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (onUpPressed != null && onUpPressed.invoke(slot.index)) {
                                return@setOnKeyListener true
                            }
                        }
                        KeyEvent.KEYCODE_MENU -> {
                            onSlotManage(slot)
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
                    tvAppName.setTextColor(if (slot.appModel != null) Color.WHITE else Color.parseColor("#CBD5E1"))
                }
            }
        }
    }
}
