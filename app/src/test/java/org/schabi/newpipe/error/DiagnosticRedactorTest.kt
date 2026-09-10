/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.error

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticRedactorTest {
    @Test fun `export contains exception types and frames without request details`() {
        val trace = """
            java.io.IOException: https://example.com/watch?v=private&token=secret
            Cookie: SID=credential
                at org.example.Player.open(Player.java:30)
            Caused by: java.lang.IllegalStateException: personal search term
        """.trimIndent()
        val result = DiagnosticRedactor.stackTrace(trace)
        assertFalse(result.contains("private"))
        assertFalse(result.contains("credential"))
        assertFalse(result.contains("personal"))
        assertTrue(result.contains("java.io.IOException"))
        assertTrue(result.contains("Player.java:30"))
    }
}
