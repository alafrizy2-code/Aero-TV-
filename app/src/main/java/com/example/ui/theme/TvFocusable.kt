package com.example.ui.theme

import android.view.KeyEvent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Android TV Remote Focus Highlight Palette (Purple Glow Design System)
val TvFocusPurplePrimary = Color(0xFFA855F7) // Vibrant TV Purple (#A855F7)
val TvFocusPurpleLight = Color(0xFFC084FC)   // Crisp High-Contrast TV Border (#C084FC)
val TvFocusPurpleGlow = Color(0x999333EA)    // Soft Ambient Outer Glow
val TvFocusPurpleHalo = Color(0x45A855F7)    // Far Outer Diffuse Aura

/**
 * Android TV Remote Focus Highlight Modifier
 * - Smooth spring scale on focus (1.0f -> 1.08f)
 * - Purple border with multi-layer soft glow effect
 * - Smooth transition when losing focus
 * - High visibility from TV couch distance
 * - Full D-pad navigation support (Up, Down, Left, Right, OK)
 * - Consistent focused and selected state support
 */
@Composable
fun Modifier.tvFocusable(
    accentColor: Color = TvFocusPurpleLight,
    glowColor: Color = TvFocusPurplePrimary,
    shape: Shape = RoundedCornerShape(18.dp),
    scaleOnFocus: Float = 1.08f,
    elevationOnFocus: Dp = 12.dp,
    focusRequester: FocusRequester? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
): Modifier {
    var isFocused by remember { mutableStateOf(false) }

    // Smooth TV spring physics for focus scale
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else if (isSelected) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tv_focus_scale"
    )

    // Animated glow alpha for smooth entrance/exit transitions
    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else if (isSelected) 0.65f else 0.0f,
        animationSpec = tween(durationMillis = 200),
        label = "tv_focus_glow"
    )

    // Animated border width (tebal dan kontras tinggi di TV)
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 3.8.dp else if (isSelected) 2.8.dp else 1.dp,
        animationSpec = tween(durationMillis = 180),
        label = "tv_focus_border_width"
    )

    return this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { state -> isFocused = state.isFocused }
        .focusable()
        .scale(scale)
        .onKeyEvent { keyEvent ->
            val code = keyEvent.nativeKeyEvent.keyCode
            if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                code == KeyEvent.KEYCODE_ENTER ||
                code == KeyEvent.KEYCODE_NUMPAD_ENTER
            ) {
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                    onClick()
                }
                true
            } else {
                // Pass D-pad UP, DOWN, LEFT, RIGHT and BACK to TV system navigation
                false
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
        // Multi-layer diffuse purple glow behind the card for TV viewing distance
        .drawBehind {
            if (glowAlpha > 0f) {
                // Outer diffuse halo
                drawRoundRect(
                    color = TvFocusPurpleHalo.copy(alpha = 0.40f * glowAlpha),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    size = Size(size.width + 14.dp.toPx(), size.height + 14.dp.toPx()),
                    topLeft = Offset(-7.dp.toPx(), -7.dp.toPx())
                )
                // Mid ambient purple glow
                drawRoundRect(
                    color = TvFocusPurpleGlow.copy(alpha = 0.65f * glowAlpha),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    size = Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                    topLeft = Offset(-3.dp.toPx(), -3.dp.toPx())
                )
            }
        }
        .then(
            if (isFocused || isSelected) {
                Modifier
                    .shadow(
                        elevation = elevationOnFocus,
                        shape = shape,
                        spotColor = glowColor,
                        ambientColor = TvFocusPurpleGlow
                    )
                    .border(
                        BorderStroke(borderWidth, accentColor),
                        shape
                    )
                    .drawWithContent {
                        drawContent()
                        // Subtle frosted liquid specular highlight on focused card
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x33A855F7), Color(0x08FFFFFF), Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.45f
                            )
                        )
                    }
            } else {
                Modifier.border(BorderStroke(1.dp, Color(0x22FFFFFF)), shape)
            }
        )
}
