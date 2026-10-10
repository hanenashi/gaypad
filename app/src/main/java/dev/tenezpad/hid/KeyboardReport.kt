package dev.tenezpad.hid

enum class PadKey { UP, DOWN, LEFT, RIGHT, B, A, Y, X, L, R, START, SELECT }

enum class KeyLayout { PLAYER_ONE, PLAYER_TWO }

/** HID keyboard usages. Port 2 avoids every RetroArch default Port 1 key. */
object KeyMappings {
    val usages: Map<KeyLayout, Map<PadKey, Int>> = mapOf(
        KeyLayout.PLAYER_ONE to mapOf(
            PadKey.UP to 0x52, PadKey.DOWN to 0x51, PadKey.LEFT to 0x50, PadKey.RIGHT to 0x4F,
            PadKey.B to 0x1D, PadKey.A to 0x1B, PadKey.Y to 0x04, PadKey.X to 0x16,
            PadKey.L to 0x14, PadKey.R to 0x1A, PadKey.START to 0x28, PadKey.SELECT to 0xE5
        ),
        KeyLayout.PLAYER_TWO to mapOf(
            PadKey.UP to 0x25, PadKey.DOWN to 0x1F, PadKey.LEFT to 0x21, PadKey.RIGHT to 0x23,
            PadKey.B to 0x1E, PadKey.A to 0x20, PadKey.Y to 0x24, PadKey.X to 0x26,
            PadKey.L to 0x22, PadKey.R to 0x27, PadKey.START to 0x2D, PadKey.SELECT to 0x2E
        )
    )
}

object KeyboardReport {
    fun encode(pressed: Set<PadKey>, layout: KeyLayout = KeyLayout.PLAYER_ONE): ByteArray {
        val result = ByteArray(8)
        val mapping = requireNotNull(KeyMappings.usages[layout])
        val usages = pressed.map { requireNotNull(mapping[it]) }
        val ordinary = usages.filter { it < 0xE0 }.sorted()
        val modifiers = usages.filter { it in 0xE0..0xE7 }.fold(0) { bits, usage -> bits or (1 shl (usage - 0xE0)) }
        result[0] = modifiers.toByte()
        if (ordinary.size > 6) {
            // HID ErrorRollOver: never silently drop a key.
            for (i in 2..7) result[i] = 0x01
            return result
        }
        ordinary.forEachIndexed { index, usage -> result[index + 2] = usage.toByte() }
        return result
    }
}
