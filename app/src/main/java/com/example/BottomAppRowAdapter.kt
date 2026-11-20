package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * Adapter baris aplikasi horizontal di bagian bawah Home Screen sesuai foto referensi:
 * - Kartu persegi panjang horizontal 224 x 96 px (rasio ~2.33:1) pada referensi 1920 x 1080.
 * - Latar kaca gelap transparan (#880B101B) dengan border tipis (#40FFFFFF) dan sudut membulat 4 px.
 * - Ikon aplikasi di kiri (46 px) dan nama aplikasi putih di kanan (maks 2 baris).
 * - Saat fokus: sedikit membesar (1.12x), latar kaca sedikit lebih terang, dan outline cyan (#2DD4BF) terang.
 * - Penskalaan responsif penuh terhadap lebar layar TV.
 */
class BottomAppRowAdapter(
    context: Context,
    private var appList: List<AppModel>,
    private val onAppClick: (AppModel) -> Unit
) : RecyclerView.Adapter<BottomAppRowAdapter.BottomAppViewHolder>() {

    val scale: Float = (context.resources.displayMetrics.widthPixels / 1920f).coerceAtLeast(0.5f)
    val cardWidthPx: Int = (224f * scale).roundToInt().coerceAtLeast(110)
    val cardHeightPx: Int = (96f * scale).roundToInt().coerceAtLeast(48)
    val gapPx: Int = (16f * scale).roundToInt().coerceAtLeast(8)
    val edgePaddingHorizontalPx: Int = (76f * scale).roundToInt().coerceAtLeast(32)
    val edgePaddingVerticalPx: Int = (16f * scale).roundToInt().coerceAtLeast(10)

    private val iconSizePx: Int = (46f * scale).roundToInt().coerceAtLeast(24)
    private val paddingStartPx: Int = (16f * scale).roundToInt().coerceAtLeast(8)
    private val paddingEndPx: Int = (14f * scale).roundToInt().coerceAtLeast(8)
    private val paddingVerticalPx: Int = (10f * scale).roundToInt().coerceAtLeast(5)
    private val textMarginStartPx: Int = (12f * scale).roundToInt().coerceAtLeast(6)
    private val textSizePx: Float = (20f * scale).coerceAtLeast(12f)
    private val cornerRadiusPx: Float = (4f * scale).coerceAtLeast(3f)

    fun updateApps(newApps: List<AppModel>) {
        this.appList = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bottom_app_card, parent, false)
        view.layoutParams = RecyclerView.LayoutParams(cardWidthPx, cardHeightPx)
        return BottomAppViewHolder(
            view,
            iconSizePx,
            paddingStartPx,
            paddingEndPx,
            paddingVerticalPx,
            textMarginStartPx,
            textSizePx,
            cornerRadiusPx,
            scale
        )
    }

    override fun onBindViewHolder(holder: BottomAppViewHolder, position: Int) {
        holder.bind(appList[position], onAppClick)
    }

    override fun getItemCount(): Int = appList.size

    class BottomAppViewHolder(
        itemView: View,
        iconSizePx: Int,
        paddingStartPx: Int,
        paddingEndPx: Int,
        paddingVerticalPx: Int,
        textMarginStartPx: Int,
        textSizePx: Float,
        cornerRadiusPx: Float,
        scale: Float
    ) : RecyclerView.ViewHolder(itemView) {

        private val layoutCardContent: LinearLayout = itemView.findViewById(R.id.layoutBottomCardContent)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivBottomAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvBottomAppName)
        private val viewFocusBorder: View = itemView.findViewById(R.id.viewBottomFocusBorder)

        private val normalBg: GradientDrawable
        private val focusedBg: GradientDrawable

        init {
            layoutCardContent.setPadding(
                paddingStartPx,
                paddingVerticalPx,
                paddingEndPx,
                paddingVerticalPx
            )

            ivAppIcon.layoutParams = ivAppIcon.layoutParams.apply {
                width = iconSizePx
                height = iconSizePx
            }

            (tvAppName.layoutParams as? LinearLayout.LayoutParams)?.apply {
                marginStart = textMarginStartPx
                tvAppName.layoutParams = this
            }
            tvAppName.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)

            val normalStrokePx = (1.5f * scale).roundToInt().coerceAtLeast(1)
            normalBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setColor(Color.parseColor("#880B101B"))
                setStroke(normalStrokePx, Color.parseColor("#44FFFFFF"))
            }

            focusedBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setColor(Color.parseColor("#8858687E"))
            }

            val focusStrokePx = (4.5f * scale).roundToInt().coerceAtLeast(3)
            viewFocusBorder.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setColor(Color.TRANSPARENT)
                setStroke(focusStrokePx, Color.parseColor("#2DD4BF"))
            }

            layoutCardContent.background = normalBg
        }

        fun bind(app: AppModel, onClick: (AppModel) -> Unit) {
            tvAppName.text = app.appName
            ivAppIcon.setImageDrawable(app.icon)

            val currentlyFocused = itemView.isFocused
            applyFocusVisuals(itemView, currentlyFocused, animate = false)

            itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                applyFocusVisuals(v, hasFocus, animate = true)
            }

            itemView.setOnClickListener {
                onClick(app)
            }

            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    onClick(app)
                    return@setOnKeyListener true
                }
                false
            }
        }

        private fun applyFocusVisuals(v: View, hasFocus: Boolean, animate: Boolean) {
            if (hasFocus) {
                layoutCardContent.background = focusedBg
                viewFocusBorder.visibility = View.VISIBLE
                v.elevation = 16f
                if (animate) {
                    v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(110).start()
                } else {
                    v.scaleX = 1.12f
                    v.scaleY = 1.12f
                }
            } else {
                layoutCardContent.background = normalBg
                viewFocusBorder.visibility = View.GONE
                v.elevation = 2f
                if (animate) {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(110).start()
                } else {
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                }
            }
        }
    }
}
