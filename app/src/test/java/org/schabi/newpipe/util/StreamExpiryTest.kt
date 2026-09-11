/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamExpiryTest {
    @Test fun `only a known expired Googlevideo URL qualifies for refresh`() {
        assertTrue(StreamExpiry.isExpired("https://rr1.googlevideo.com/videoplayback?expire=1000", 1000001))
        assertFalse(StreamExpiry.isExpired("https://rr1.googlevideo.com/videoplayback?expire=1000", 999999))
        assertFalse(StreamExpiry.isExpired("https://rr1.googlevideo.com/videoplayback", Long.MAX_VALUE))
        assertFalse(StreamExpiry.isExpired("https://googlevideo.com.evil.example/?expire=1", 10000))
    }

    @Test fun `malformed duplicated and overflowing expiry values are ignored`() {
        for (query in listOf("expire=-1", "expire=oops", "expire=9223372036854775807", "expire=1&expire=2")) {
            assertNull(StreamExpiry.expiresAt("https://rr1.googlevideo.com/videoplayback?$query"))
        }
    }
}
