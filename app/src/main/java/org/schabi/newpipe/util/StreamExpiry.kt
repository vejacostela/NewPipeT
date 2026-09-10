/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.util

import java.net.URI
import org.schabi.newpipe.extractor.stream.StreamInfo

object StreamExpiry {
    @JvmStatic
    fun expiresAt(url: String): Long? = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" ||
            !(uri.host == "googlevideo.com" || uri.host?.endsWith(".googlevideo.com") == true)
        ) {
            return@runCatching null
        }
        val seconds = uri.rawQuery?.split('&')
            ?.singleOrNull { it.startsWith("expire=") }
            ?.removePrefix("expire=")?.toLongOrNull() ?: return@runCatching null
        if (seconds <= 0 || seconds > Long.MAX_VALUE / 1000) return@runCatching null
        seconds * 1000
    }.getOrNull()

    @JvmStatic
    fun isExpired(url: String, nowMillis: Long): Boolean = expiresAt(url)?.let { it <= nowMillis } ?: false

    @JvmStatic
    fun cacheLifetime(info: StreamInfo, defaultMillis: Long, nowMillis: Long): Long {
        val streams = info.videoStreams + info.videoOnlyStreams + info.audioStreams
        val urls = streams.filter { it.isUrl }.map { it.content } +
            listOfNotNull(info.hlsUrl, info.dashMpdUrl)
        val earliest = urls.mapNotNull(::expiresAt).minOrNull() ?: return defaultMillis
        // Refresh before the signed URL expires; do not cache already-expired streams.
        return (earliest - nowMillis - 30000).coerceIn(0, defaultMillis)
    }
}
