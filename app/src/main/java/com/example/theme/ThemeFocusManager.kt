package com.example.theme

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView

/**
 * ThemeFocusManager:
 * Mengatur seluruh efek fokus remote D-Pad (skala, durasi animasi, interpolator, elevasi,
 * warna border fokus, visibilitas teks fokus) secara terpisah dan halus tanpa lag.
 */
object ThemeFocusManager {

    private val decelerate = DecelerateInterpolator()
    private val overshoot = OvershootInterpolator(1.15f)

    fun applyFocusState(
        rootView: View,
        cardBox: FrameLayout,
        focusBorderView: View,
        tvInsideLabel: TextView,
        tvBelowLabel: TextView,
        normalBg: GradientDrawable?,
        focusedBg: GradientDrawable?,
        theme: LauncherThemeDefinition,
        hasFocus: Boolean,
        animate: Boolean
    ) {
        val focusCfg = theme.focusConfig
        val cardCfg = theme.cardStyle
        val isFullBleedVisual = (
            cardCfg.shapeType == CardShapeType.FULL_CIRCLE_TILE ||
            cardCfg.shapeType == CardShapeType.BANNER_16_9_FULL_BLEED ||
            cardCfg.shapeType == CardShapeType.WOLF_BANNER_TILE
        )

        rootView.animate().cancel()
        tvBelowLabel.animate().cancel()

        if (hasFocus) {
            if (!isFullBleedVisual) {
                cardBox.background = focusedBg
            } else {
                cardBox.background = null
            }
            focusBorderView.visibility = View.VISIBLE
            rootView.alpha = 1.0f
            rootView.elevation = focusCfg.elevationFocused

            when (cardCfg.labelPlacement) {
                CardLabelPlacement.INSIDE_RIGHT,
                CardLabelPlacement.INSIDE_BOTTOM_STACK -> {
                    tvInsideLabel.setTextColor(cardCfg.labelColorFocused)
                }
                CardLabelPlacement.BELOW_CARD_ALWAYS -> {
                    tvBelowLabel.setTextColor(cardCfg.labelColorFocused)
                    tvBelowLabel.alpha = 1.0f
                }
                CardLabelPlacement.BELOW_CARD_FOCUS_ONLY -> {
                    tvBelowLabel.visibility = View.VISIBLE
                    tvBelowLabel.setTextColor(cardCfg.labelColorFocused)
                    if (animate) {
                        tvBelowLabel.animate().alpha(1.0f).setDuration(focusCfg.animDurationMs).start()
                    } else {
                        tvBelowLabel.alpha = 1.0f
                    }
                }
                CardLabelPlacement.BANNER_INTEGRATED -> {
                    // Judul sudah menyatu di dalam banner full-bleed
                }
            }

            if (animate) {
                val interp = if (focusCfg.effectStyle == FocusEffectStyle.NEXFY_NEON_DUAL_BORDER) {
                    overshoot
                } else {
                    decelerate
                }
                rootView.animate()
                    .scaleX(focusCfg.scaleFocused)
                    .scaleY(focusCfg.scaleFocused)
                    .setDuration(focusCfg.animDurationMs)
                    .setInterpolator(interp)
                    .start()
            } else {
                rootView.scaleX = focusCfg.scaleFocused
                rootView.scaleY = focusCfg.scaleFocused
            }
        } else {
            if (!isFullBleedVisual) {
                cardBox.background = normalBg
            } else {
                cardBox.background = null
            }
            focusBorderView.visibility = View.GONE
            rootView.alpha = focusCfg.unfocusedCardAlpha
            rootView.elevation = focusCfg.elevationNormal

            when (cardCfg.labelPlacement) {
                CardLabelPlacement.INSIDE_RIGHT,
                CardLabelPlacement.INSIDE_BOTTOM_STACK -> {
                    tvInsideLabel.setTextColor(cardCfg.labelColorNormal)
                }
                CardLabelPlacement.BELOW_CARD_ALWAYS -> {
                    tvBelowLabel.setTextColor(cardCfg.labelColorNormal)
                    tvBelowLabel.alpha = 0.88f
                }
                CardLabelPlacement.BELOW_CARD_FOCUS_ONLY -> {
                    tvBelowLabel.alpha = 0f
                    tvBelowLabel.visibility = View.INVISIBLE
                }
                CardLabelPlacement.BANNER_INTEGRATED -> {
                    // Judul sudah menyatu di dalam banner
                }
            }

            if (animate) {
                rootView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(focusCfg.animDurationMs)
                    .setInterpolator(decelerate)
                    .start()
            } else {
                rootView.scaleX = 1.0f
                rootView.scaleY = 1.0f
            }
        }
    }
}
