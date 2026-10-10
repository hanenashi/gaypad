package dev.tenezpad.hid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

class HidKeyboardManager(
    private val context: Context,
    private val status: (String) -> Unit,
    private val connection: (Boolean) -> Unit
) {
    private val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private var proxy: BluetoothHidDevice? = null
    private var registered = false
    private var registrationPending = false
    private var proxyRequested = false
    private var started = false
    private var connected: BluetoothDevice? = null
    private var leds: Byte = 0
    private var layout: KeyLayout = KeyLayout.PLAYER_ONE
    private val pressed = mutableSetOf<PadKey>()

    private fun show(message: String) {
        Log.i(TAG, message)
        status(message)
    }

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private val listener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, service: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            proxyRequested = false
            if (!started) {
                adapter?.closeProfileProxy(profile, service)
                return
            }
            proxy = service as BluetoothHidDevice
            register()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            pressed.clear()
            proxyRequested = false
            proxy = null
            connected = null
            connection(false)
            registered = false
            registrationPending = false
            show("HID service disconnected")
        }
    }

    @SuppressLint("MissingPermission") // Every callback checks the revocable permission at runtime.
    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(device: BluetoothDevice?, isRegistered: Boolean) {
            if (!started) return
            registrationPending = false
            registered = isRegistered
            Log.i(TAG, "HID registration result: $isRegistered")
            if (!isRegistered) {
                pressed.clear()
                connected = null
                connection(false)
                show("HID unregistered; reopen app to retry")
            } else {
                show("Ready / waiting for host")
                // A bonded host may reconnect without an explicit app connect call.
                val existing = try { if (hasPermission()) proxy?.connectedDevices?.firstOrNull() else null }
                    catch (e: SecurityException) { Log.e(TAG, "Connected devices permission failure", e); null }
                existing?.let {
                    connected = it
                    connection(true)
                    show("Connected: ${safeName(it)}")
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.i(TAG, "Connection state=$state host=${safeName(device)}")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connected = device
                    connection(true)
                    pressed.clear()
                    if (hasPermission()) try { proxy?.sendReport(device, 0, ByteArray(8)) }
                    catch (e: SecurityException) { Log.e(TAG, "Initial report permission failure", e) }
                    show("Connected: ${safeName(device)}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    pressed.clear()
                    if (connected == device) connected = null
                    connection(false)
                    show("Disconnected / waiting for host")
                }
                BluetoothProfile.STATE_CONNECTING -> show("Connecting: ${safeName(device)}")
                BluetoothProfile.STATE_DISCONNECTING -> show("Disconnecting: ${safeName(device)}")
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            if (!hasPermission()) { Log.w(TAG, "GET_REPORT denied: no permission"); return }
            val hid = proxy ?: return
            try {
                if (id.toInt() != 0 || (type != BluetoothHidDevice.REPORT_TYPE_INPUT && type != BluetoothHidDevice.REPORT_TYPE_OUTPUT)) {
                    hid.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
                    return
                }
                val data = if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) KeyboardReport.encode(pressed, layout) else byteArrayOf(leds)
                Log.d(TAG, "GET_REPORT input, length=${data.size}, buffer=$bufferSize")
                if (!hid.replyReport(device, type, id, data)) Log.w(TAG, "replyReport failed")
            } catch (e: SecurityException) {
                Log.e(TAG, "GET_REPORT permission failure", e)
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            if (!hasPermission()) { Log.w(TAG, "SET_REPORT denied: no permission"); return }
            // The standard descriptor's single output byte is keyboard LEDs.
            try {
                if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT && id.toInt() == 0 && data.size == 1) {
                    leds = data[0]
                    proxy?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
                } else {
                    proxy?.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_PARAM)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SET_REPORT permission failure", e)
            }
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            Log.i(TAG, "Keyboard protocol=$protocol")
            releaseAll()
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            releaseAll()
            connected = null
            connection(false)
            show("Host unpaired / disconnected")
        }
    }

    fun start() {
        started = true
        when {
            adapter == null -> show("Bluetooth unavailable")
            !hasPermission() -> show("Bluetooth permission required")
            !adapter.isEnabled -> show("Bluetooth disabled")
            proxy != null -> register()
            !proxyRequested -> {
                proxyRequested = true
                show("Opening HID service")
                val requested = try {
                    adapter.getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Profile proxy permission failure", e)
                    false
                }
                if (!requested) {
                    proxyRequested = false
                    show("Error: HID device profile unavailable")
                }
            }
        }
    }

    private fun register() {
        if (!started || registered || registrationPending || !hasPermission()) return
        val hid = proxy ?: return
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "gaypad", "Keyboard controller", "gaypad",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD, KeyboardDescriptor.bytes
        )
        registrationPending = true
        show("HID registering")
        try {
            if (!hid.registerApp(sdp, null, null, context.mainExecutor, callback)) {
                registrationPending = false
                show("Error: HID registration request failed")
            }
        } catch (e: SecurityException) {
            registrationPending = false
            Log.e(TAG, "HID registration permission failure", e)
            show("Error: Bluetooth permission denied")
        }
    }

    fun bondedHosts(): List<BluetoothDevice> = try {
        if (hasPermission()) adapter?.bondedDevices?.sortedBy { safeName(it) } ?: emptyList() else emptyList()
    } catch (e: SecurityException) {
        Log.e(TAG, "Bonded device permission failure", e)
        emptyList()
    }

    fun connect(device: BluetoothDevice) {
        if (!registered || !hasPermission()) {
            show("Register HID before connecting")
            return
        }
        if (device !in bondedHosts()) {
            show("Pair host in Bluetooth settings first")
            return
        }
        Log.i(TAG, "Selected host: ${safeName(device)}")
        try {
            if (proxy?.connect(device) != true) show("Error: connect request failed")
            else show("Connecting: ${safeName(device)}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Connect permission failure", e)
            show("Error: Bluetooth permission denied")
        }
    }

    fun disconnect() {
        val device = connected ?: return
        releaseAll()
        try {
            if (proxy?.disconnect(device) != true) show("Error: disconnect request failed")
        } catch (e: SecurityException) {
            Log.e(TAG, "Disconnect permission failure", e)
            show("Error: Bluetooth permission denied")
        }
    }

    fun setPressed(key: PadKey, down: Boolean) {
        if (!registered || connected == null) return
        val changed = if (down) pressed.add(key) else pressed.remove(key)
        if (changed) send()
    }

    fun setLayout(newLayout: KeyLayout) {
        if (layout == newLayout) return
        releaseAll()
        layout = newLayout
        Log.i(TAG, "Keyboard layout: $layout")
    }

    private fun send() {
        val device = connected ?: return
        val data = KeyboardReport.encode(pressed, layout)
        if (data.size != 8) {
            Log.e(TAG, "Invalid keyboard report size: ${data.size}")
            show("Error: invalid HID report")
            return
        }
        try {
            if (proxy?.sendReport(device, 0, data) != true) {
                Log.w(TAG, "sendReport failed")
                show("Error: HID report send failed")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "sendReport permission failure", e)
            show("Error: Bluetooth permission denied")
        }
    }

    fun releaseAll() {
        if (pressed.isEmpty()) return
        pressed.clear()
        send()
    }

    fun stop() {
        releaseAll()
        started = false
        connected = null
        connection(false)
        registered = false
        registrationPending = false
        try { proxy?.unregisterApp() } catch (e: SecurityException) { Log.e(TAG, "Unregister failed", e) }
        proxy?.let { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
        proxy = null
        proxyRequested = false
    }

    private fun safeName(device: BluetoothDevice): String = try {
        if (hasPermission()) device.name ?: "Paired device" else "Paired device"
    } catch (_: SecurityException) { "Paired device" }

    companion object { const val TAG = "gaypad" }
}
