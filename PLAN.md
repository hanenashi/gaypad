# gaypad v0.1 plan

1. Build one landscape Android activity (API 28+, compile/target API 37) with a small Compose touch surface and status area.
2. Register a keyboard HID descriptor through the public `BluetoothHidDevice` profile. Use one eight-byte, six-key rollover input report with no report ID; keep mapping and report encoding separate from UI.
3. Request `BLUETOOTH_CONNECT` at runtime. Request `BLUETOOTH_ADVERTISE` only when opening Android's temporary discoverability prompt for pairing. Pair in iPad Bluetooth Settings; use `connect()` only for a selected bonded host and `disconnect()` for the active host. Do not scan or request location.
4. Treat active pointers independently, send reports for every key state change, and release keys on pause, disconnect, registration loss, and teardown.
5. Compile, run checks, build debug APK, and review HID sizes, lifecycle, permissions, and simultaneous touch. Verify phone/iPad behavior physically.

Android reference: https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice and https://developer.android.com/develop/connectivity/bluetooth/bt-permissions . The API requires `BLUETOOTH_CONNECT` and unregisters a HID app when it is no longer foreground.

## v0.2: two-player arcade test

The v0.1 keys exactly match RetroArch's default Player 1 keyboard binds. Add a saved Port 2 key layout using unused number/symbol keys and bind them under RetroArch Port 2 while the Xbox controller remains Port 1. Verify the two-player result on Daniel iPad before considering gamepad HID.

Result: v0.2 Port 2 keyboard bindings and the Xbox controller controlled separate players in The Combatribes on Daniel iPad.

## v0.3: gaypad identity

Use the supplied arcade artwork as the README hero, derive a launcher icon from it, and rename launcher/UI/Bluetooth SDP text to `gaypad`. Keep the application ID for in-place upgrades. Confirmed v0.3 upgrades and reconnects to Daniel iPad.

## v0.3: continuous eight-way D-pad

Replace the four D-pad buttons with one square pointer surface. A pure Kotlin resolver uses independent horizontal and vertical thresholds, a center dead zone, and mild hysteresis; positions outside the surface resolve to neutral. Keep one owning pointer until it lifts, release removed directions before pressing added ones, and clear directions on cancellation. Draw one cross-shaped pad with active arms while leaving the other buttons and Bluetooth HID code unchanged. Unit test all directions, dead-zone boundaries, transitions, and outside release; then run tests, lint, and a debug build. Verify thumb slides and simultaneous buttons on the Pixel and iPad.

## v0.4: controller visuals and Settings popup

Keep the proven touch controls and HID behavior. Give the controller a unified shell, a more physical D-pad face, and distinct round face buttons. Move pairing, host connection, and layout switching into a scrollable Settings popup while keeping connection status visible during play. Show the Gradle app version in Settings. Release active controls when opening the popup and verify the layout and connection flow on the Pixel and iPad.
