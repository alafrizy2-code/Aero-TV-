package com.example.theme

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.AppModel
import com.example.TvHorizontalRecyclerView
import kotlin.math.roundToInt

/**
 * Adapter terpadu untuk Home Screen dan App Drawer menggunakan ThemeRenderer yang sama
 * dan satu sumber data AppModel.
 */
class ThemedAppAdapter(
    context: Context,
    private var appList: List<AppModel>,
    theme: LauncherThemeDefinition,
    private val onAppClick: (AppModel) -> Unit
) : RecyclerView.Adapter<ThemeRenderer.ThemedAppViewHolder>() {

    val renderer: ThemeRenderer = ThemeRenderer(context, theme)

    fun updateApps(newApps: List<AppModel>) {
        this.appList = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeRenderer.ThemedAppViewHolder {
        return renderer.createViewHolder(parent)
    }

    override fun onBindViewHolder(holder: ThemeRenderer.ThemedAppViewHolder, position: Int) {
        renderer.bindViewHolder(holder, appList[position], onAppClick)
    }

    override fun getItemCount(): Int = appList.size
}

/**
 * ThemeLayoutManager:
 * Mengatur konfigurasi layout Home Screen (1-baris horizontal vs 2-baris grid, ukuran, jarak kartu,
 * padding tepi, header baris tema, dan ekstra layout prefetch untuk navigasi D-Pad yang mulus).
 */
object ThemeLayoutManager {

    private var activeSpacingDecoration: RecyclerView.ItemDecoration? = null

    fun applyHomeLayout(
        context: Context,
        recyclerView: TvHorizontalRecyclerView,
        headerTextView: TextView?,
        apps: List<AppModel>,
        theme: LauncherThemeDefinition,
        onAppClick: (AppModel) -> Unit,
        onRequestAppDrawer: () -> Unit
    ): ThemedAppAdapter {
        val adapter = ThemedAppAdapter(context, apps, theme, onAppClick)
        val renderer = adapter.renderer
        val density = context.resources.displayMetrics.density

        // Header baris tema (tersembunyi di Default Theme agar tampilan lama utuh)
        if (headerTextView != null) {
            val title = theme.layout.headerTitle
            if (title.isNullOrBlank()) {
                headerTextView.visibility = View.GONE
            } else {
                headerTextView.text = title
                headerTextView.visibility = View.VISIBLE
            }
        }

        // Atur bottom margin RecyclerView sesuai tema
        (recyclerView.layoutParams as? RelativeLayout.LayoutParams)?.let { lp ->
            lp.bottomMargin = (theme.layout.bottomMarginDp * density).roundToInt()
            recyclerView.layoutParams = lp
        }

        val extraPrefetchPx = (480f * renderer.scale).roundToInt().coerceAtLeast(240)
        val span = theme.layout.spanCount.coerceAtLeast(1)

        recyclerView.layoutManager = if (theme.layout.layoutType == ThemeLayoutType.HORIZONTAL_TWO_ROW_GRID && span > 1) {
            object : GridLayoutManager(context, span, RecyclerView.HORIZONTAL, false) {
                @Deprecated("Deprecated in Java")
                override fun getExtraLayoutSpace(state: RecyclerView.State): Int = extraPrefetchPx

                override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
                    extraLayoutSpace[0] = extraPrefetchPx
                    extraLayoutSpace[1] = extraPrefetchPx
                }

                override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                    if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT || direction == View.FOCUS_DOWN) {
                        val container = recyclerView.findContainingItemView(focused)
                        val pos = container?.let { getPosition(it) } ?: RecyclerView.NO_POSITION
                        if (pos != RecyclerView.NO_POSITION) {
                            when (direction) {
                                View.FOCUS_RIGHT -> if (pos + span < itemCount) findViewByPosition(pos + span)?.let { return it }
                                View.FOCUS_LEFT -> if (pos - span >= 0) findViewByPosition(pos - span)?.let { return it }
                                View.FOCUS_DOWN -> if ((pos % span) < span - 1 && pos + 1 < itemCount) findViewByPosition(pos + 1)?.let { return it }
                            }
                        }
                        return focused
                    }
                    return super.onInterceptFocusSearch(focused, direction)
                }
            }
        } else {
            object : LinearLayoutManager(context, RecyclerView.HORIZONTAL, false) {
                @Deprecated("Deprecated in Java")
                override fun getExtraLayoutSpace(state: RecyclerView.State): Int = extraPrefetchPx

                override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
                    extraLayoutSpace[0] = extraPrefetchPx
                    extraLayoutSpace[1] = extraPrefetchPx
                }

                override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                    if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT || direction == View.FOCUS_DOWN) {
                        val container = recyclerView.findContainingItemView(focused)
                        val pos = container?.let { getPosition(it) } ?: RecyclerView.NO_POSITION
                        if (pos != RecyclerView.NO_POSITION) {
                            if (direction == View.FOCUS_RIGHT && pos < itemCount - 1) {
                                findViewByPosition(pos + 1)?.let { return it }
                            } else if (direction == View.FOCUS_LEFT && pos > 0) {
                                findViewByPosition(pos - 1)?.let { return it }
                            }
                        }
                        return focused
                    }
                    return super.onInterceptFocusSearch(focused, direction)
                }
            }
        }

        activeSpacingDecoration?.let { recyclerView.removeItemDecoration(it) }
        val halfH = (renderer.gapHorizontalPx / 2).coerceAtLeast(3)
        val halfV = (renderer.gapVerticalPx / 2).coerceAtLeast(3)

        val decoration = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val pos = parent.getChildAdapterPosition(view)
                val count = parent.adapter?.itemCount ?: 0
                if (span > 1) {
                    val row = pos % span
                    val isFirstCol = pos < span
                    val left = if (isFirstCol) 0 else halfH
                    val right = halfH
                    val top = if (row == 0) 0 else halfV
                    val bottom = if (row == span - 1) 0 else halfV
                    outRect.set(left, top, right, bottom)
                } else {
                    val left = if (pos == 0) 0 else halfH
                    val right = if (pos == count - 1) 0 else halfH
                    outRect.set(left, 0, right, 0)
                }
            }
        }
        activeSpacingDecoration = decoration
        recyclerView.addItemDecoration(decoration)

        recyclerView.setPadding(
            renderer.edgePaddingHorizontalPx,
            renderer.edgePaddingVerticalPx,
            renderer.edgePaddingHorizontalPx,
            renderer.edgePaddingVerticalPx
        )
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(24)
        recyclerView.itemAnimator = null
        recyclerView.onDownAtBottomListener = if (theme.layout.supportsDownAppDrawer) {
            onRequestAppDrawer
        } else {
            null
        }
        recyclerView.adapter = adapter
        recyclerView.resetFocusMemory(0)

        return adapter
    }
}
