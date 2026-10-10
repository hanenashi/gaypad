package dev.tenezpad.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tenezpad.hid.PadKey

private val baseColor = Color(0xFF314159)
private val activeColor = Color(0xFF5285D4)
private val centerColor = Color(0xFF233247)

@Composable
internal fun EightWayDPad(modifier: Modifier = Modifier, onPress: (PadKey, Boolean) -> Unit) {
    var activeDirections by remember { mutableStateOf(emptySet<PadKey>()) }
    val currentOnPress by rememberUpdatedState(onPress)
    val view = LocalView.current

    Box(
        modifier.pointerInput(Unit) {
            var owner: PointerId? = null
            var directions = emptySet<PadKey>()
            var hapticSent = false

            fun update(next: Set<PadKey>) {
                if (next == directions) return
                val change = dPadChange(directions, next)
                change.released.forEach { currentOnPress(it, false) }
                change.pressed.forEach { currentOnPress(it, true) }
                directions = next
                activeDirections = next
                if (!hapticSent && next.isNotEmpty()) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    hapticSent = true
                }
            }

            try {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (owner == null) {
                            val down = event.changes.firstOrNull {
                                !it.previousPressed && it.pressed &&
                                    it.position.x >= 0 && it.position.y >= 0 &&
                                    it.position.x < size.width && it.position.y < size.height
                            }
                            if (down != null) {
                                owner = down.id
                                hapticSent = false
                            }
                        }
                        val owned = event.changes.firstOrNull { it.id == owner }
                        if (owned == null || !owned.pressed) {
                            if (owner != null) update(emptySet())
                            owner = null
                        } else {
                            update(resolveDPadDirection(
                                owned.position.x, owned.position.y,
                                size.width.toFloat(), size.height.toFloat(), directions
                            ))
                            owned.consume()
                        }
                    }
                }
            } finally {
                update(emptySet())
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val arm = size.minDimension / 3f
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawRoundRect(Color(0xFF172333), cornerRadius = CornerRadius(24f, 24f))
            drawRoundRect(Color(0xFF506988), cornerRadius = CornerRadius(24f, 24f), style = Stroke(width = 3f))
            drawRect(baseColor, Offset(cx - arm / 2f, 0f), Size(arm, size.height))
            drawRect(baseColor, Offset(0f, cy - arm / 2f), Size(size.width, arm))
            if (PadKey.UP in activeDirections) drawArm(Offset(cx - arm / 2f, 0f), Size(arm, cy - arm / 2f))
            if (PadKey.DOWN in activeDirections) drawArm(Offset(cx - arm / 2f, cy + arm / 2f), Size(arm, cy - arm / 2f))
            if (PadKey.LEFT in activeDirections) drawArm(Offset(0f, cy - arm / 2f), Size(cx - arm / 2f, arm))
            if (PadKey.RIGHT in activeDirections) drawArm(Offset(cx + arm / 2f, cy - arm / 2f), Size(cx - arm / 2f, arm))
            drawCircle(centerColor, radius = arm * 0.38f, center = Offset(cx, cy))
            drawCircle(Color(0xFF6680A1), radius = arm * 0.38f, center = Offset(cx, cy), style = Stroke(width = 2f))
        }
        Text("↑", Modifier.align(Alignment.TopCenter).offset(y = 8.dp), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("↓", Modifier.align(Alignment.BottomCenter).offset(y = (-8).dp), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("←", Modifier.align(Alignment.CenterStart).offset(x = 8.dp), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("→", Modifier.align(Alignment.CenterEnd).offset(x = (-8).dp), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
    }
}

private fun DrawScope.drawArm(topLeft: Offset, size: Size) {
    drawRect(activeColor, topLeft, size)
}
