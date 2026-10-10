package dev.tenezpad.ui

import android.bluetooth.BluetoothDevice
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tenezpad.hid.PadKey
import dev.tenezpad.hid.KeyLayout

private val bg = Color(0xFF10151E)
private val face = Color(0xFF283448)
private val active = Color(0xFF4374C3)

@Composable
fun GamepadScreen(
    status: String,
    connected: Boolean,
    layout: KeyLayout,
    hosts: List<BluetoothDevice>,
    hostName: (BluetoothDevice) -> String,
    onPress: (PadKey, Boolean) -> Unit,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onDiscoverable: () -> Unit,
    onToggleLayout: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 14.dp, vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("TenezPad", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text(status, Modifier.weight(1f), color = Color(0xFFC8D6EA), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            SmallAction(if (layout == KeyLayout.PLAYER_ONE) "P1 keys" else "P2 keys", onToggleLayout)
            Spacer(Modifier.width(5.dp))
            SmallAction("Pair", onDiscoverable)
            Spacer(Modifier.width(5.dp))
            if (connected) SmallAction("Disconnect", onDisconnect)
            else SmallAction("Refresh hosts", onRefresh)
        }
        Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("iPad: Settings → Bluetooth → pair this Pixel while Pair is active. Then connect below.", color = Color.LightGray, fontSize = 11.sp)
        }
        if (!connected && hosts.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().height(34.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                Text("Paired hosts:", color = Color.LightGray, fontSize = 11.sp)
                hosts.forEach { device ->
                    Spacer(Modifier.width(7.dp))
                    SmallAction(hostName(device), { onConnect(device) })
                }
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.Center) {
                    PadButton("L", PadKey.L, Modifier.fillMaxWidth(0.72f), onPress)
                }
                DPad(Modifier.weight(1f).aspectRatio(1f), onPress)
            }
            Column(Modifier.weight(0.70f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                PadButton("Select", PadKey.SELECT, Modifier.fillMaxWidth(0.8f).height(60.dp), onPress)
                Spacer(Modifier.height(12.dp))
                PadButton("Start", PadKey.START, Modifier.fillMaxWidth(0.8f).height(60.dp), onPress)
                Spacer(Modifier.height(25.dp))
            }
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.Center) {
                    PadButton("R", PadKey.R, Modifier.fillMaxWidth(0.72f), onPress)
                }
                FaceButtons(Modifier.weight(1f).aspectRatio(1f), onPress)
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, action: () -> Unit) {
    Button(onClick = action, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), colors = ButtonDefaults.buttonColors(containerColor = face), modifier = Modifier.height(30.dp)) {
        Text(label, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun DPad(modifier: Modifier, onPress: (PadKey, Boolean) -> Unit) {
    Column(modifier) {
        Row(Modifier.weight(1f)) {
            Spacer(Modifier.weight(1f)); PadButton("↑", PadKey.UP, Modifier.weight(1f), onPress); Spacer(Modifier.weight(1f))
        }
        Row(Modifier.weight(1f)) {
            PadButton("←", PadKey.LEFT, Modifier.weight(1f), onPress)
            Spacer(Modifier.weight(1f))
            PadButton("→", PadKey.RIGHT, Modifier.weight(1f), onPress)
        }
        Row(Modifier.weight(1f)) {
            Spacer(Modifier.weight(1f)); PadButton("↓", PadKey.DOWN, Modifier.weight(1f), onPress); Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FaceButtons(modifier: Modifier, onPress: (PadKey, Boolean) -> Unit) {
    Column(modifier) {
        Row(Modifier.weight(1f)) {
            Spacer(Modifier.weight(1f)); PadButton("Y", PadKey.Y, Modifier.weight(1f), onPress); Spacer(Modifier.weight(1f))
        }
        Row(Modifier.weight(1f)) {
            PadButton("X", PadKey.X, Modifier.weight(1f), onPress)
            Spacer(Modifier.weight(1f))
            PadButton("A", PadKey.A, Modifier.weight(1f), onPress)
        }
        Row(Modifier.weight(1f)) {
            Spacer(Modifier.weight(1f)); PadButton("B", PadKey.B, Modifier.weight(1f), onPress); Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PadButton(label: String, key: PadKey, modifier: Modifier, onPress: (PadKey, Boolean) -> Unit) {
    var down by remember { mutableStateOf(false) }
    val view = LocalView.current
    Box(
        modifier.padding(3.dp).fillMaxHeight().background(if (down) active else face, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF5D728F), RoundedCornerShape(14.dp))
            .pointerInput(key) {
                try {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val nowDown = event.changes.any { change ->
                                change.pressed && change.position.x >= 0 && change.position.y >= 0 &&
                                    change.position.x < size.width && change.position.y < size.height
                            }
                            if (nowDown != down) {
                                down = nowDown
                                onPress(key, nowDown)
                                if (nowDown) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        }
                    }
                } finally {
                    if (down) {
                        down = false
                        onPress(key, false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = if (label.length > 1) 16.sp else 27.sp, fontWeight = FontWeight.Bold)
    }
}
