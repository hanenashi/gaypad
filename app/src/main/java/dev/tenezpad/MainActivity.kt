package dev.tenezpad

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.tenezpad.hid.HidKeyboardManager
import dev.tenezpad.hid.KeyLayout
import dev.tenezpad.ui.GamepadScreen

class MainActivity : ComponentActivity() {
    private var status by mutableStateOf("Opening Bluetooth")
    private var hosts by mutableStateOf<List<BluetoothDevice>>(emptyList())
    private var connected by mutableStateOf(false)
    private var layout by mutableStateOf(KeyLayout.PLAYER_ONE)
    private var inputEpoch by mutableStateOf(0)
    private lateinit var hid: HidKeyboardManager

    private val connectPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            hid.start()
            refreshHosts()
        } else status = "Bluetooth permission denied"
    }
    private val advertisePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) requestDiscoverable() else status = "Nearby devices permission needed for pairing"
    }
    private val discoverable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        status = if (result.resultCode > 0) "Discoverable for ${result.resultCode}s; pair from iPad Settings"
        else "Discoverability cancelled"
        hid.start()
        refreshHosts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 5894 or 1024 or 512
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hid = HidKeyboardManager(this, { status = it }, { connected = it })
        layout = runCatching { KeyLayout.valueOf(getPreferences(MODE_PRIVATE).getString("layout", KeyLayout.PLAYER_ONE.name)!!) }
            .getOrDefault(KeyLayout.PLAYER_ONE)
        hid.setLayout(layout)
        setContent {
            key(inputEpoch) { GamepadScreen(
                status = status,
                connected = connected,
                layout = layout,
                hosts = hosts,
                hostName = { try { it.name ?: "Paired device" } catch (_: SecurityException) { "Paired device" } },
                onPress = hid::setPressed,
                onConnect = hid::connect,
                onDisconnect = hid::disconnect,
                onDiscoverable = ::beginPairing,
                onToggleLayout = ::toggleLayout,
                onRefresh = ::refreshHosts
            ) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            connectPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        else {
            hid.start()
            refreshHosts()
        }
    }

    override fun onPause() {
        hid.releaseAll()
        inputEpoch++
        super.onPause()
    }

    override fun onStop() {
        hid.stop()
        super.onStop()
    }

    override fun onDestroy() {
        hid.stop()
        super.onDestroy()
    }

    private fun refreshHosts() { hosts = hid.bondedHosts() }

    private fun beginPairing() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
            advertisePermission.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
        else requestDiscoverable()
    }

    private fun toggleLayout() {
        val next = if (layout == KeyLayout.PLAYER_ONE) KeyLayout.PLAYER_TWO else KeyLayout.PLAYER_ONE
        hid.setLayout(next)
        layout = next
        getPreferences(MODE_PRIVATE).edit().putString("layout", next.name).apply()
    }

    private fun requestDiscoverable() {
        discoverable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120))
    }
}
