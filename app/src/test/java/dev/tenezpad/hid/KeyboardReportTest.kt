package dev.tenezpad.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class KeyboardReportTest {
    @Test fun arrowsAndLettersCanBeHeldTogether() {
        assertArrayEquals(
            byteArrayOf(0, 0, 0x1D, 0x4F, 0x51, 0, 0, 0),
            KeyboardReport.encode(setOf(PadKey.RIGHT, PadKey.DOWN, PadKey.B))
        )
    }

    @Test fun rightShiftUsesModifierByte() {
        assertArrayEquals(
            byteArrayOf(0x20, 0, 0x1B, 0, 0, 0, 0, 0),
            KeyboardReport.encode(setOf(PadKey.SELECT, PadKey.A))
        )
    }

    @Test fun emptySetReleasesEveryKey() {
        assertArrayEquals(ByteArray(8), KeyboardReport.encode(emptySet()))
    }

    @Test fun overSixOrdinaryKeysReturnsErrorRollover() {
        assertArrayEquals(
            byteArrayOf(0, 0, 1, 1, 1, 1, 1, 1),
            KeyboardReport.encode(setOf(PadKey.UP, PadKey.DOWN, PadKey.LEFT, PadKey.RIGHT, PadKey.A, PadKey.B, PadKey.X))
        )
    }

    @Test fun portTwoUsesDistinctKeysAndNoModifier() {
        val p1 = KeyMappings.usages.getValue(KeyLayout.PLAYER_ONE).values.toSet()
        val p2 = KeyMappings.usages.getValue(KeyLayout.PLAYER_TWO).values.toSet()
        org.junit.Assert.assertTrue(p1.intersect(p2).isEmpty())
        org.junit.Assert.assertEquals(12, p2.size)
        assertArrayEquals(
            byteArrayOf(0, 0, 0x1E, 0x23, 0, 0, 0, 0),
            KeyboardReport.encode(setOf(PadKey.RIGHT, PadKey.B), KeyLayout.PLAYER_TWO)
        )
    }
}
