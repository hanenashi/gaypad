package dev.tenezpad.ui

import dev.tenezpad.hid.PadKey
import org.junit.Assert.assertEquals
import org.junit.Test

class DPadDirectionTest {
    private fun direction(x: Float, y: Float, previous: Set<PadKey> = emptySet()) =
        resolveDPadDirection(x, y, 100f, 100f, previous)

    @Test fun centerAndDeadZone() {
        assertEquals(emptySet<PadKey>(), direction(50f, 50f))
        assertEquals(emptySet<PadKey>(), direction(61.9f, 50f))
        assertEquals(setOf(PadKey.RIGHT), direction(62.1f, 50f))
        assertEquals(emptySet<PadKey>(), direction(38.1f, 50f))
        assertEquals(setOf(PadKey.LEFT), direction(37.9f, 50f))
    }

    @Test fun cardinalDirections() {
        assertEquals(setOf(PadKey.UP), direction(50f, 0f))
        assertEquals(setOf(PadKey.DOWN), direction(50f, 99f))
        assertEquals(setOf(PadKey.LEFT), direction(0f, 50f))
        assertEquals(setOf(PadKey.RIGHT), direction(99f, 50f))
    }

    @Test fun diagonalDirections() {
        assertEquals(setOf(PadKey.UP, PadKey.LEFT), direction(0f, 0f))
        assertEquals(setOf(PadKey.UP, PadKey.RIGHT), direction(99f, 0f))
        assertEquals(setOf(PadKey.DOWN, PadKey.LEFT), direction(0f, 99f))
        assertEquals(setOf(PadKey.DOWN, PadKey.RIGHT), direction(99f, 99f))
    }

    @Test fun hysteresisAndSlideTransitions() {
        val right = direction(85f, 50f)
        assertEquals(right, direction(59f, 50f, right))
        val diagonal = direction(75f, 75f, right)
        assertEquals(setOf(PadKey.RIGHT, PadKey.DOWN), diagonal)
        val down = direction(50f, 85f, diagonal)
        assertEquals(setOf(PadKey.DOWN), down)
        assertEquals(DPadChange(setOf(PadKey.RIGHT), emptySet()), dPadChange(diagonal, down))
        assertEquals(DPadChange(emptySet(), setOf(PadKey.DOWN)), dPadChange(right, diagonal))
        assertEquals(DPadChange(setOf(PadKey.DOWN), setOf(PadKey.LEFT)), dPadChange(down, setOf(PadKey.LEFT)))
    }

    @Test fun oneThumbCanSweepAcrossFiveDirectionsAndNeutral() {
        val points = listOf(
            85f to 50f,
            75f to 75f,
            50f to 85f,
            25f to 75f,
            15f to 50f,
            50f to 50f
        )
        val expected = listOf(
            setOf(PadKey.RIGHT),
            setOf(PadKey.RIGHT, PadKey.DOWN),
            setOf(PadKey.DOWN),
            setOf(PadKey.LEFT, PadKey.DOWN),
            setOf(PadKey.LEFT),
            emptySet()
        )
        var previous = emptySet<PadKey>()
        points.zip(expected).forEach { (point, keys) ->
            val next = direction(point.first, point.second, previous)
            assertEquals(keys, next)
            previous = next
        }
        assertEquals(emptySet<PadKey>(), previous)
    }

    @Test fun outsideAndRelease() {
        val right = setOf(PadKey.RIGHT)
        assertEquals(emptySet<PadKey>(), direction(-0.1f, 50f, right))
        assertEquals(emptySet<PadKey>(), direction(100f, 50f, right))
        assertEquals(emptySet<PadKey>(), direction(50f, 100f, right))
        assertEquals(DPadChange(right, emptySet()), dPadChange(right, emptySet()))
    }
}
