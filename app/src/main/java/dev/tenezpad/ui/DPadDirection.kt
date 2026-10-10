package dev.tenezpad.ui

import dev.tenezpad.hid.PadKey
import kotlin.math.min

/** Fractions of the pad's half-width. Both axes use the same thresholds. */
internal const val DPAD_ENTER_THRESHOLD = 0.24f
internal const val DPAD_EXIT_THRESHOLD = 0.16f

internal fun resolveDPadDirection(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    previous: Set<PadKey> = emptySet()
): Set<PadKey> {
    if (width <= 0f || height <= 0f || x < 0f || y < 0f || x >= width || y >= height) {
        return emptySet()
    }
    val radius = min(width, height) / 2f
    val dx = (x - width / 2f) / radius
    val dy = (y - height / 2f) / radius
    return buildSet {
        axisDirection(dx, PadKey.LEFT, PadKey.RIGHT, previous)?.let(::add)
        axisDirection(dy, PadKey.UP, PadKey.DOWN, previous)?.let(::add)
    }
}

private fun axisDirection(value: Float, negative: PadKey, positive: PadKey, previous: Set<PadKey>): PadKey? = when {
    value <= -DPAD_ENTER_THRESHOLD -> negative
    value >= DPAD_ENTER_THRESHOLD -> positive
    negative in previous && value <= -DPAD_EXIT_THRESHOLD -> negative
    positive in previous && value >= DPAD_EXIT_THRESHOLD -> positive
    else -> null
}

internal data class DPadChange(val released: Set<PadKey>, val pressed: Set<PadKey>)

internal fun dPadChange(from: Set<PadKey>, to: Set<PadKey>) = DPadChange(
    released = from - to,
    pressed = to - from
)
