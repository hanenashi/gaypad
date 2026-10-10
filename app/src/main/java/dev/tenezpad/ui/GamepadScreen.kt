package dev.tenezpad.ui

import android.bluetooth.BluetoothDevice
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tenezpad.hid.PadKey
import dev.tenezpad.hid.KeyLayout

private val bg = Color(0xFF10151E)
private val shell = Color(0xFF202C3E)
private val face = Color(0xFF34445C)
private val active = Color(0xFF5285D4)
private val trim = Color(0xFF617897)

@Composable
fun GamepadScreen(
    status: String,
    version: String,
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
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var controlsEpoch by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("gaypad", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(12.dp))
            Text(if (layout == KeyLayout.PLAYER_ONE) "P1" else "P2", color = Color(0xFF9DC6FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text(status, Modifier.weight(1f), color = if (connected) Color(0xFF94E5BC) else Color(0xFFC8D6EA), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            SmallAction("⚙ Settings", { controlsEpoch++; settingsOpen = true })
        }
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 4.dp),
            shape = RoundedCornerShape(42.dp),
            color = shell,
            border = androidx.compose.foundation.BorderStroke(2.dp, trim)
        ) {
            key(controlsEpoch) { ControllerControls(onPress) }
        }
    }
    if (settingsOpen) {
        SettingsPopup(
            version, status, connected, layout, hosts, hostName,
            onConnect, onDisconnect, onDiscoverable, onToggleLayout, onRefresh,
            onClose = { settingsOpen = false }
        )
    }
}

@Composable
private fun ControllerControls(onPress: (PadKey, Boolean) -> Unit) {
    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Row(Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.Center) {
                    PadButton("L", PadKey.L, Modifier.fillMaxWidth(0.72f), onPress)
                }
                EightWayDPad(Modifier.weight(1f).aspectRatio(1f), onPress)
            }
            Column(Modifier.weight(0.70f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                PadButton("Select", PadKey.SELECT, Modifier.fillMaxWidth(0.8f).height(58.dp), onPress)
                Spacer(Modifier.height(12.dp))
                PadButton("Start", PadKey.START, Modifier.fillMaxWidth(0.8f).height(58.dp), onPress)
                Spacer(Modifier.height(25.dp))
            }
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Row(Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.Center) {
                    PadButton("R", PadKey.R, Modifier.fillMaxWidth(0.72f), onPress)
                }
                FaceButtons(Modifier.weight(1f).aspectRatio(1f), onPress)
            }
    }
}

@Composable
private fun SettingsPopup(
    version: String,
    status: String,
    connected: Boolean,
    layout: KeyLayout,
    hosts: List<BluetoothDevice>,
    hostName: (BluetoothDevice) -> String,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onDiscoverable: () -> Unit,
    onToggleLayout: () -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = shell,
            border = androidx.compose.foundation.BorderStroke(1.dp, trim)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Settings", Modifier.weight(1f), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("v$version", color = Color(0xFFB7C9E0), fontSize = 13.sp)
                }
                Spacer(Modifier.height(5.dp))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(status, color = if (connected) Color(0xFF94E5BC) else Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("KEY LAYOUT", color = Color(0xFF9DC6FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    SmallAction(if (layout == KeyLayout.PLAYER_ONE) "P1 keys  ·  switch to P2" else "P2 keys  ·  switch to P1", onToggleLayout)
                    Spacer(Modifier.height(6.dp))
                    Text("BLUETOOTH", color = Color(0xFF9DC6FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Pair in iPad Settings → Bluetooth while this phone is discoverable.", color = Color(0xFFD7E2F1), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction("Make discoverable", onDiscoverable)
                        SmallAction("Refresh hosts", onRefresh)
                        if (connected) SmallAction("Disconnect", onDisconnect)
                    }
                    if (!connected) {
                        Spacer(Modifier.height(4.dp))
                        Text("Paired hosts", color = Color(0xFFD7E2F1), fontSize = 13.sp)
                        if (hosts.isEmpty()) Text("None yet — pair from iPad Bluetooth settings, then refresh.", color = Color(0xFF9CAFC6), fontSize = 12.sp)
                        hosts.forEach { device ->
                            Spacer(Modifier.height(5.dp))
                            SmallAction("Connect: ${hostName(device)}", { onConnect(device) })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SmallAction("Close", onClose)
                }
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
    val faceAccent = when (key) {
        PadKey.A -> Color(0xFFF47E83)
        PadKey.B -> Color(0xFFF6C36A)
        PadKey.X -> Color(0xFF7EB6F4)
        PadKey.Y -> Color(0xFF8DD7A3)
        else -> trim
    }
    val shape = if (key == PadKey.A || key == PadKey.B || key == PadKey.X || key == PadKey.Y) CircleShape else RoundedCornerShape(18.dp)
    Box(
        modifier.padding(3.dp).fillMaxHeight().background(if (down) active else face, shape)
            .border(2.dp, faceAccent, shape)
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
        Text(label, color = if (down) Color.White else if (key == PadKey.A || key == PadKey.B || key == PadKey.X || key == PadKey.Y) faceAccent else Color(0xFFE1ECF7), fontSize = if (label.length > 1) 16.sp else 27.sp, fontWeight = FontWeight.Bold)
    }
}
