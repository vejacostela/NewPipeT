/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.util

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.SingleSubject
import org.junit.Assert.assertEquals
import org.junit.Test

class SingleFlightTest {
    private val flights = SingleFlight<String>()
    private var cached: String? = null
    private var calls = 0
    private var network: Single<String> = SingleSubject.create()

    private fun load(force: Boolean = false): Single<String> = flights.load(
        "video",
        force,
        { cached },
        { cached = null },
        {
            calls++
            network
        },
        { cached = it }
    )

    @Test fun `share concurrent extraction and reuse completed cache`() {
        val response = SingleSubject.create<String>()
        network = response
        val one = load().test()
        val two = load().test()
        assertEquals(1, calls)
        response.onSuccess("fresh")
        one.assertValue("fresh")
        two.assertValue("fresh")
        load().test().assertValue("fresh")
        assertEquals(1, calls)
    }

    @Test fun `superseded response cannot overwrite forced refresh`() {
        val old = SingleSubject.create<String>()
        network = old
        load().test()
        val fresh = SingleSubject.create<String>()
        network = fresh
        load(true).test()
        fresh.onSuccess("new")
        old.onSuccess("old")
        assertEquals("new", cached)
    }

    @Test fun `failed extraction does not poison future requests`() {
        network = Single.error(IllegalStateException())
        load().test().assertError(IllegalStateException::class.java)
        network = Single.just("recovered")
        load().test().assertValue("recovered")
        assertEquals(2, calls)
    }

    @Test fun `concurrent force calls share the fresh request`() {
        cached = "old"
        val response = SingleSubject.create<String>()
        network = response
        val first = load(true).test()
        val second = load(true).test()
        response.onSuccess("fresh")
        first.assertValue("fresh")
        second.assertValue("fresh")
        assertEquals(1, calls)
    }
}
