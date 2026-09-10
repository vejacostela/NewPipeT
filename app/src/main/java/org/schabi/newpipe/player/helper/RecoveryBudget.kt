/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.player.helper

/** Budgets reset on another queue item/session, never on another error or reload. */
class RecoveryBudget {
    private var networkAttempts = 0
    private var expiredRefreshUsed = false

    fun nextDelay(expired: Boolean, transient: Boolean, maxNetworkRetries: Int, allowRefresh: Boolean): Long {
        if (expired && allowRefresh && !expiredRefreshUsed) {
            expiredRefreshUsed = true
            return 0
        }
        if (transient && networkAttempts < maxNetworkRetries.coerceIn(0, 3)) {
            return 1000L * (1L shl networkAttempts++)
        }
        return -1
    }

    fun reset() {
        networkAttempts = 0
        expiredRefreshUsed = false
    }
}
