# TenezPad v0.2

Pixel 10a as a Bluetooth HID **keyboard** for an iPad running RetroArch. No network or account. This is a keyboard proof of concept, not a gamepad HID device.

## Pair and use

1. Install `app/build/outputs/apk/debug/app-debug.apk` on the Pixel and turn Bluetooth on for both devices.
2. Open TenezPad, grant Nearby devices access, and wait for **Ready / waiting for host**.
3. Tap **Pair** and accept Android's discoverability prompt. On the iPad open **Settings → Bluetooth**, choose the Pixel, and confirm the pairing code on both devices. Keep TenezPad visible after accepting the prompt. If the iPad does not find it, open the Pixel's Bluetooth Settings to make the phone discoverable, return to TenezPad, and try again. Android may unregister the HID service while TenezPad is in the background; it re-registers on return.
4. Tap **Refresh hosts** and tap the iPad's paired-device name to request an HID connection. The iPad may also connect on its own. Wait for **Connected: …**.
5. Open an iPad text field to check letters, arrows, Enter, and Shift. Then bind the keys in RetroArch.

The mapping is in `KeyMappings` in `KeyboardReport.kt`. **P1 keys** retain the original D-pad arrows, B=Z, A=X, Y=A, X=S, L=Q, R=W, Start=Enter, Select=Right Shift. **P2 keys** use a separate set: Up=8, Down=2, Left=4, Right=6, B=1, A=3, Y=7, X=9, L=5, R=0, Start=minus, Select=equals. Tap the `P1 keys` / `P2 keys` control to switch; the choice is saved. Buttons support simultaneous touches.

## Two players in RetroArch

The Pixel remains one Bluetooth keyboard, separate from the Xbox controller. RetroArch's default keyboard binds match **P1 keys**, so both inputs control Player 1 until Port 2 is configured. A Bluetooth keyboard will not appear as a second game controller in iPadOS Settings.

1. Connect the Xbox controller and TenezPad. In TenezPad select **P2 keys**.
2. In RetroArch set **Settings → Input → Maximum Users** to at least 2.
3. Open **Settings → Input → RetroPad Binds → Port 1 Controls** and set **Device Index** to the Xbox controller. Keep its gamepad bindings.
4. Open **Port 2 Controls** and bind its D-pad, A/B/X/Y, shoulders, Start, and Select using the Pixel buttons. The new keys do not overlap the default Port 1 keyboard binds. If **Set All Controls** accepts keyboard presses, use it; otherwise bind each control individually.
5. In a two-player arcade game, check **Quick Menu → Controls → Port 2 Controls** for a Joypad/RetroPad device type and save a core or game remap if needed. Test that the Xbox moves Player 1 and TenezPad moves Player 2 independently.

Port 2 keyboard handling was physically confirmed on Daniel iPad: TenezPad and an Xbox controller played separate players in **The Combatribes** through RetroArch.

The app only requests Nearby devices permissions for Bluetooth connection and temporary discoverability. It does not scan, use location, or access the network. Android unregisters a HID device app when it leaves the foreground, so keep the app visible while playing.

## Build

Use JDK 21 and Android SDK 37, then run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.

## Physical validation

Keyboard HID pairing, text-field key delivery, and two-player gameplay with an Xbox controller were confirmed with Daniel iPad. Extended tests of Down+Right, Right+B, L+A, Select+A, and quick app switching remain useful for v0.2. A key must stop when released or when TenezPad leaves the foreground. Check Logcat with `adb logcat -s TenezPad` for registration, connection, and report failures.
