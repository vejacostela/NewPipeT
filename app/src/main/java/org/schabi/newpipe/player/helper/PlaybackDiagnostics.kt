/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.player.helper

import org.schabi.newpipe.BuildConfig

/** In-memory counters only. No URLs, searches, cookies, identifiers, or automatic uploads. */
object PlaybackDiagnostics {
    private var starts = 0
    private var frames = 0
    private var retries = 0
    private var failures = 0
    private var lastError = "none"
    private var lastStartupMs = -1L

    @JvmStatic
    @Synchronized
    fun started() { starts++ }
    @JvmStatic
    @Synchronized
    fun firstFrame(elapsedMs: Long) {
        frames++
        lastStartupMs = elapsedMs.coerceAtLeast(0)
    }
    @JvmStatic
    @Synchronized
    fun retry() { retries++ }
    @JvmStatic
    @Synchronized
    fun failed(errorCode: Int) {
        failures++
        lastError = "player_$errorCode"
    }
    @JvmStatic
    @Synchronized
    fun report(): String =
        "NewPipeT ${BuildConfig.VERSION_NAME}\nExtractor ${BuildConfig.EXTRACTOR_VERSION}\n" +
            "starts=$starts\nfirst_frames=$frames\nretries=$retries\nerrors=$failures\n" +
            "last_error=$lastError\nlast_first_frame_ms=$lastStartupMs"

    @JvmStatic
    @Synchronized
    fun clear() {
        starts = 0
        frames = 0
        retries = 0
        failures = 0
        lastError = "none"
        lastStartupMs = -1
    }
}
