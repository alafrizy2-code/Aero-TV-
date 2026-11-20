package com.example

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom Horizontal RecyclerView untuk Android TV STB (ZTE B866F).
 * Mendukung 1 baris (LinearLayoutManager) maupun 2 baris (GridLayoutManager HORIZONTAL)
 * untuk sistem tema launcher, sekaligus menjamin fokus D-Pad Kiri/Kanan/Bawah terkunci
 * 100% agar tidak pernah melompat ke ikon bar di pojok kanan atas saat tombol arah ditahan.
 */
class TvHorizontalRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var lastKnownFocusedPos: Int = 0
    private var pendingFocusRunnable: Runnable? = null

    /**
     * Opsional: dipanggil saat user menekan DPAD_DOWN di baris paling bawah (aktif di tema non-default
     * untuk membuka App Drawer bertema; null pada Default Theme agar perilaku tetap terkunci).
     */
    var onDownAtBottomListener: (() -> Unit)? = null

    init {
        isFocusable = false
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        setItemViewCacheSize(24)
        setHasFixedSize(true)
    }

    fun resetFocusMemory(pos: Int = 0) {
        lastKnownFocusedPos = pos.coerceAtLeast(0)
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        super.requestChildFocus(child, focused)
        if (child != null) {
            val pos = getChildAdapterPosition(child)
            if (pos != NO_POSITION) {
                lastKnownFocusedPos = pos
            }
        }
    }

    override fun onRequestFocusInDescendants(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        val total = adapter?.itemCount ?: 0
        if (total > 0) {
            val lm = layoutManager as? LinearLayoutManager
            val clampedPos = lastKnownFocusedPos.coerceIn(0, total - 1)
            val savedView = lm?.findViewByPosition(clampedPos)
            if (savedView != null && savedView.isFocusable && savedView.requestFocus()) {
                return true
            }
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect)
    }

    private fun getHorizontalSpanCount(): Int {
        val lm = layoutManager
        return if (lm is GridLayoutManager && lm.orientation == HORIZONTAL) {
            lm.spanCount.coerceAtLeast(1)
        } else {
            1
        }
    }

    override fun focusSearch(focused: View?, direction: Int): View? {
        val lm = layoutManager as? LinearLayoutManager
        val total = adapter?.itemCount ?: 0
        val span = getHorizontalSpanCount()

        if (direction == View.FOCUS_RIGHT || direction == View.FOCUS_LEFT || direction == View.FOCUS_DOWN) {
            if (lm != null && total > 0) {
                val containerView = focused?.let { findContainingItemView(it) }
                val rawPos = containerView?.let { getChildAdapterPosition(it) } ?: NO_POSITION
                val currentPos = if (rawPos != NO_POSITION) rawPos else lastKnownFocusedPos.coerceIn(0, total - 1)

                val candidatePos = when (direction) {
                    View.FOCUS_RIGHT -> if (currentPos + span < total) currentPos + span else null
                    View.FOCUS_LEFT -> if (currentPos - span >= 0) currentPos - span else null
                    View.FOCUS_DOWN -> if (span > 1 && (currentPos % span) < span - 1 && currentPos + 1 < total) currentPos + 1 else null
                    else -> null
                }
                if (candidatePos != null) {
                    lm.findViewByPosition(candidatePos)?.let { if (it.isFocusable) return it }
                }

                val fallbackPos = currentPos.coerceIn(0, total - 1)
                return focused
                    ?: lm.findViewByPosition(fallbackPos)
                    ?: getChildAt(0)
            }
            return focused ?: getChildAt(0)
        }

        // Untuk FOCUS_UP di grid 2 baris, pindah dari baris 1 ke baris 0 lebih dulu
        if (direction == View.FOCUS_UP && span > 1 && lm != null && total > 0) {
            val containerView = focused?.let { findContainingItemView(it) }
            val rawPos = containerView?.let { getChildAdapterPosition(it) } ?: NO_POSITION
            val currentPos = if (rawPos != NO_POSITION) rawPos else lastKnownFocusedPos.coerceIn(0, total - 1)
            if ((currentPos % span) > 0 && currentPos - 1 >= 0) {
                lm.findViewByPosition(currentPos - 1)?.let { if (it.isFocusable) return it }
            }
        }

        return super.focusSearch(focused, direction)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val span = getHorizontalSpanCount()

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN
        ) {
            if (event.action != KeyEvent.ACTION_DOWN) {
                return true
            }

            val total = adapter?.itemCount ?: 0
            val lm = layoutManager as? LinearLayoutManager ?: return true
            if (total <= 0) return true

            val currentFocusView = focusedChild ?: findFocus()?.let { findContainingItemView(it) }
            val rawPos = currentFocusView?.let { getChildAdapterPosition(it) } ?: NO_POSITION
            val currentPos = if (rawPos != NO_POSITION) rawPos else lastKnownFocusedPos.coerceIn(0, total - 1)

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                val rowIdx = currentPos % span
                if (span > 1 && rowIdx < span - 1 && currentPos + 1 < total) {
                    moveFocusToPosition(lm, currentPos + 1, isMovingRight = false, isRepeat = false)
                } else {
                    onDownAtBottomListener?.invoke()
                }
                return true
            }

            val targetPos = if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (currentPos + span < total) currentPos + span else currentPos
            } else {
                if (currentPos - span >= 0) currentPos - span else currentPos
            }

            if (targetPos == currentPos) {
                val edgeView = lm.findViewByPosition(currentPos)
                if (edgeView != null && !edgeView.isFocused) {
                    edgeView.requestFocus()
                }
                return true
            }

            moveFocusToPosition(
                lm = lm,
                targetPos = targetPos,
                isMovingRight = (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT),
                isRepeat = (event.repeatCount > 0)
            )
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && span > 1 && event.action == KeyEvent.ACTION_DOWN) {
            val total = adapter?.itemCount ?: 0
            val lm = layoutManager as? LinearLayoutManager
            if (lm != null && total > 0) {
                val currentFocusView = focusedChild ?: findFocus()?.let { findContainingItemView(it) }
                val rawPos = currentFocusView?.let { getChildAdapterPosition(it) } ?: NO_POSITION
                val currentPos = if (rawPos != NO_POSITION) rawPos else lastKnownFocusedPos.coerceIn(0, total - 1)
                if ((currentPos % span) > 0 && currentPos - 1 >= 0) {
                    moveFocusToPosition(lm, currentPos - 1, isMovingRight = false, isRepeat = false)
                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    private fun moveFocusToPosition(
        lm: LinearLayoutManager,
        targetPos: Int,
        isMovingRight: Boolean,
        isRepeat: Boolean
    ) {
        lastKnownFocusedPos = targetPos
        pendingFocusRunnable?.let { removeCallbacks(it) }

        val cardApproxStep = (getChildAt(0)?.width ?: (width / 7)).coerceAtLeast(120)
        var targetView = lm.findViewByPosition(targetPos)

        if (targetView == null) {
            stopScroll()
            scrollBy(if (isMovingRight) cardApproxStep else -cardApproxStep, 0)
            targetView = lm.findViewByPosition(targetPos)
        }

        if (targetView != null && targetView.isAttachedToWindow) {
            targetView.requestFocus()

            val leftBound = paddingLeft
            val rightBound = width - paddingRight
            val peekOffset = (targetView.width / 3).coerceAtLeast(40)

            val scrollDelta = when {
                isMovingRight && targetView.right > rightBound ->
                    (targetView.right - rightBound) + peekOffset
                !isMovingRight && targetView.left < leftBound ->
                    (targetView.left - leftBound) - peekOffset
                else -> 0
            }

            if (scrollDelta != 0) {
                if (isRepeat) {
                    stopScroll()
                    scrollBy(scrollDelta, 0)
                } else {
                    smoothScrollBy(scrollDelta, 0)
                }
            }
            return
        }

        val peekMargin = if (isMovingRight) {
            (width - paddingRight - cardApproxStep - (cardApproxStep / 3)).coerceAtLeast(paddingLeft)
        } else {
            paddingLeft
        }
        lm.scrollToPositionWithOffset(targetPos, peekMargin)

        val runnable = object : Runnable {
            var attempts = 0
            override fun run() {
                val view = lm.findViewByPosition(targetPos)
                if (view != null && view.isAttachedToWindow) {
                    view.requestFocus()
                } else if (attempts < 4) {
                    attempts++
                    postDelayed(this, 16)
                }
            }
        }
        pendingFocusRunnable = runnable
        post(runnable)
    }
}
