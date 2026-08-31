package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun formatDuration_zeroOrNegative_returnsZeroFormat() {
        assertEquals("00:00", TimeUtils.formatDuration(0L))
        assertEquals("00:00", TimeUtils.formatDuration(-100L))
    }

    @Test
    fun formatDuration_minutesAndSeconds_returnsFormattedMinutesSeconds() {
        assertEquals("01:05", TimeUtils.formatDuration(65_000L))
        assertEquals("03:45", TimeUtils.formatDuration(225_000L))
    }

    @Test
    fun formatDuration_hoursMinutesSeconds_returnsFormattedHours() {
        assertEquals("1:01:05", TimeUtils.formatDuration(3665_000L))
    }
}
