/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.compatibility

import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.player.helper.PlayerDataSource
import org.schabi.newpipe.player.resolver.VideoPlaybackResolver

/** Opt-in network tests. Normal offline CI never treats a skipped probe as playback success. */
@RunWith(AndroidJUnit4::class)
class YouTubePlaybackTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val service = ServiceList.YouTube

    @Before
    fun requireLiveOptIn() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("youtubeLiveTests") == "true")
    }

    @Test(timeout = 120000)
    fun searchChannelAndPlaylistRemainUsable() {
        val search = SearchInfo.getInfo(
            service,
            service.searchQHFactory.fromQuery("Blender Open Movies", listOf("playlists"), "")
        )
        assertFalse("Search returned no results", search.relatedItems.isEmpty())
        val playlistUrl = search.relatedItems.filterIsInstance<PlaylistInfoItem>().firstOrNull()?.url
        assertNotNull("No playlist result", playlistUrl)
        val playlist = PlaylistInfo.getInfo(service, playlistUrl!!)
        assertFalse("Playlist returned no videos", playlist.relatedItems.isEmpty())
        val channel = ChannelInfo.getInfo(service, "https://www.youtube.com/@BlenderOfficial")
        assertFalse("Channel name is empty", channel.name.isBlank())
        assertFalse("Channel has no tabs", channel.tabs.isEmpty())
    }

    @Test(timeout = 120000)
    fun videoRendersAndAudioClockAdvances() {
        // Big Buck Bunny, Blender Foundation. Public control video (CC BY 3.0).
        val info = StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=aqz-KE-bpKQ")
        assertTrue("No video stream", info.videoStreams.isNotEmpty() || info.videoOnlyStreams.isNotEmpty())
        assertTrue("No audio stream", info.audioStreams.isNotEmpty() || info.videoStreams.isNotEmpty())
        val frame = AtomicBoolean(false)
        val audio = AtomicBoolean(false)
        val failure = AtomicReference<String>()
        val complete = CountDownLatch(1)
        var player: ExoPlayer? = null
        var texture: SurfaceTexture? = null
        var surface: Surface? = null
        fun maybeComplete() {
            if (frame.get() && audio.get()) complete.countDown()
        }
        try {
            instrumentation.runOnMainSync {
                val context = instrumentation.targetContext
                val resolver = VideoPlaybackResolver(
                    context,
                    PlayerDataSource(context, null),
                    object : VideoPlaybackResolver.QualityResolver {
                        override fun getDefaultResolutionIndex(sortedVideos: List<VideoStream>): Int = sortedVideos.indexOfFirst { it.resolution == "360p" }
                            .takeIf { it >= 0 } ?: sortedVideos.lastIndex

                        override fun getOverrideResolutionIndex(sortedVideos: List<VideoStream>, playbackQuality: String): Int = getDefaultResolutionIndex(sortedVideos)
                    }
                )
                val source = resolver.resolve(info)
                assertNotNull("The production resolver could not create a source", source)
                texture = SurfaceTexture(false)
                surface = Surface(texture)
                player = ExoPlayer.Builder(context).build().also { exo ->
                    exo.volume = 0f
                    exo.setVideoSurface(surface)
                    exo.addListener(object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            frame.set(true)
                            maybeComplete()
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            failure.set(error.errorCodeName)
                            complete.countDown()
                        }
                    })
                    exo.addAnalyticsListener(object : AnalyticsListener {
                        override fun onAudioPositionAdvancing(eventTime: AnalyticsListener.EventTime, playoutStartSystemTimeMs: Long) {
                            audio.set(true)
                            maybeComplete()
                        }
                    })
                    exo.setMediaSource(source!!)
                    exo.prepare()
                    exo.playWhenReady = true
                }
            }
            assertTrue("Timed out waiting for decoded audio and video", complete.await(50, TimeUnit.SECONDS))
            assertTrue("Playback failed: ${failure.get()}", failure.get() == null)
            assertTrue("No rendered video frame", frame.get())
            assertTrue("Audio output did not advance", audio.get())
        } finally {
            instrumentation.runOnMainSync {
                player?.release()
                surface?.release()
                texture?.release()
            }
        }
    }
}
