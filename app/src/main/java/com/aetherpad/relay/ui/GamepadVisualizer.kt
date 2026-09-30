package com.aetherpad.relay.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aetherpad.relay.GamepadInputState

val NeonCyan = Color(0xFF00FFFF)
val NeonMagenta = Color(0xFFFF00FF)
val HudDarkCard = Color(0xFF0D121F)
val HudGridBorder = Color(0xFF1B2838)

@Composable
fun GamepadVisualizer(
    state: GamepadInputState,
    isBatterySaver: Boolean,
    modifier: Modifier = Modifier
) {
    if (isBatterySaver) return

    val infiniteTransition = rememberInfiniteTransition(label = "hudGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // 1. Controller Silhouette Path
        val bodyPath = Path().apply {
            moveTo(center.x - 220f, center.y - 100f)
            cubicTo(center.x - 120f, center.y - 120f, center.x + 120f, center.y - 120f, center.x + 220f, center.y - 100f)
            cubicTo(center.x + 300f, center.y - 40f, center.x + 320f, center.y + 120f, center.x + 240f, center.y + 180f)
            cubicTo(center.x + 180f, center.y + 130f, center.x + 140f, center.y + 40f, center.x, center.y + 60f)
            cubicTo(center.x - 140f, center.y + 40f, center.x - 180f, center.y + 130f, center.x - 240f, center.y + 180f)
            cubicTo(center.x - 320f, center.y + 120f, center.x - 300f, center.y - 40f, center.x - 220f, center.y - 100f)
            close()
        }

        drawPath(
            path = bodyPath,
            brush = Brush.radialGradient(
                colors = listOf(HudDarkCard, Color(0xFF070B14)),
                center = center,
                radius = 320f
            )
        )
        drawPath(
            path = bodyPath,
            color = NeonCyan.copy(alpha = 0.4f),
            style = Stroke(width = 2f)
        )

        // 2. Left Stick with trail
        val leftStickBase = Offset(center.x - 120f, center.y - 20f)
        val leftStickOffset = Offset(
            leftStickBase.x + state.leftStickX * 36f,
            leftStickBase.y + state.leftStickY * 36f
        )
        drawCircle(color = HudGridBorder, radius = 48f, center = leftStickBase, style = Stroke(width = 2f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (state.buttonL3) listOf(NeonMagenta, Color(0xFF3B003B)) else listOf(NeonCyan, Color(0xFF003838)),
                center = leftStickOffset,
                radius = 28f
            ),
            radius = 28f,
            center = leftStickOffset
        )

        // 3. Right Stick with trail
        val rightStickBase = Offset(center.x + 70f, center.y + 45f)
        val rightStickOffset = Offset(
            rightStickBase.x + state.rightStickX * 36f,
            rightStickBase.y + state.rightStickY * 36f
        )
        drawCircle(color = HudGridBorder, radius = 48f, center = rightStickBase, style = Stroke(width = 2f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (state.buttonR3) listOf(NeonMagenta, Color(0xFF3B003B)) else listOf(NeonCyan, Color(0xFF003838)),
                center = rightStickOffset,
                radius = 28f
            ),
            radius = 28f,
            center = rightStickOffset
        )

        // 4. Action Buttons A, B, X, Y
        val abxyCenter = Offset(center.x + 140f, center.y - 25f)
        val btnRad = 14f

        fun drawBtn(off: Offset, pressed: Boolean, c: Color) {
            drawCircle(color = if (pressed) c else HudDarkCard, radius = btnRad, center = off)
            drawCircle(color = c, radius = btnRad, center = off, style = Stroke(width = if (pressed) 3f else 1.5f))
        }

        drawBtn(Offset(abxyCenter.x, abxyCenter.y + 24f), state.buttonA, Color(0xFF10B981))
        drawBtn(Offset(abxyCenter.x + 24f, abxyCenter.y), state.buttonB, Color(0xFFEF4444))
        drawBtn(Offset(abxyCenter.x - 24f, abxyCenter.y), state.buttonX, Color(0xFF3B82F6))
        drawBtn(Offset(abxyCenter.x, abxyCenter.y - 24f), state.buttonY, Color(0xFFF59E0B))

        // 5. Triggers Fill Bars
        val ltWidth = 80f * state.lTrigger.coerceIn(0f, 1f)
        val rtWidth = 80f * state.rTrigger.coerceIn(0f, 1f)
        // Left trigger gauge
        drawRect(color = Color(0xFF1E293B), topLeft = Offset(center.x - 170f, center.y - 140f), size = androidx.compose.ui.geometry.Size(80f, 12f))
        drawRect(color = NeonCyan, topLeft = Offset(center.x - 170f, center.y - 140f), size = androidx.compose.ui.geometry.Size(ltWidth, 12f))

        // Right trigger gauge
        drawRect(color = Color(0xFF1E293B), topLeft = Offset(center.x + 90f, center.y - 140f), size = androidx.compose.ui.geometry.Size(80f, 12f))
        drawRect(color = NeonMagenta, topLeft = Offset(center.x + 90f, center.y - 140f), size = androidx.compose.ui.geometry.Size(rtWidth, 12f))
    }
}
