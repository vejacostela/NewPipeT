/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.util

import io.reactivex.rxjava3.core.Single
import java.util.function.Consumer
import java.util.function.Supplier

/** Shares in-flight extraction and prevents an older response replacing a forced refresh. */
class SingleFlight<T : Any> {
    private data class Flight<T : Any>(val token: Any, val forced: Boolean, val result: Single<T>)
    private val flights = mutableMapOf<String, Flight<T>>()

    fun load(
        key: String,
        force: Boolean,
        cached: Supplier<T?>,
        invalidate: Runnable,
        network: Supplier<Single<T>>,
        save: Consumer<T>
    ): Single<T> = Single.defer {
        synchronized(this) {
            val active = flights[key]
            if (active != null && (!force || active.forced)) return@synchronized active.result
            if (!force) cached.get()?.let { return@synchronized Single.just(it) }
            if (force) invalidate.run()
            val token = Any()
            val result = Single.defer { network.get() }
                .doOnSuccess { value ->
                    synchronized(this) {
                        if (flights[key]?.token === token) save.accept(value)
                    }
                }
                .doFinally {
                    synchronized(this) {
                        if (flights[key]?.token === token) flights.remove(key)
                    }
                }
                .cache()
            flights[key] = Flight(token, force, result)
            result
        }
    }
}
