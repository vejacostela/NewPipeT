/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.player

import org.junit.Assert.assertEquals
import org.junit.Test
import org.schabi.newpipe.player.helper.RecoveryBudget

class RecoveryBudgetTest {
    @Test fun `network retries back off then stop`() {
        val budget = RecoveryBudget()
        assertEquals(1000L, budget.nextDelay(false, true, 2, true))
        assertEquals(2000L, budget.nextDelay(false, true, 2, true))
        repeat(10) { assertEquals(-1L, budget.nextDelay(false, true, 2, true)) }
        budget.reset()
        assertEquals(1000L, budget.nextDelay(false, true, 2, true))
    }

    @Test fun `expired URL refresh is allowed only once`() {
        val budget = RecoveryBudget()
        assertEquals(0L, budget.nextDelay(true, false, 2, true))
        assertEquals(-1L, budget.nextDelay(true, false, 2, true))
    }

    @Test fun `permanent errors and disabled recovery are not retried`() {
        val budget = RecoveryBudget()
        assertEquals(-1L, budget.nextDelay(false, false, 3, true))
        assertEquals(-1L, budget.nextDelay(false, true, 0, true))
        assertEquals(-1L, budget.nextDelay(true, false, 2, false))
    }
}
