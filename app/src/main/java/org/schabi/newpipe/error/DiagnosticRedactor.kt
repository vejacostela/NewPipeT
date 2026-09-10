/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.error

/** Only exception types and stack frames leave the device in exported crash reports. */
object DiagnosticRedactor {
    fun stackTrace(trace: String): String = trace.lineSequence().mapNotNull { line ->
        val trimmed = line.trim()
        when {
            trimmed.startsWith("at ") -> trimmed.substringBefore("http")
            trimmed.startsWith("... ") -> trimmed
            else -> {
                val name = trimmed.removePrefix("Caused by: ").removePrefix("Suppressed: ")
                    .substringBefore(':')
                name.takeIf { it.matches(Regex("[A-Za-z0-9_.$]+(?:Exception|Error)")) }
            }
        }
    }.joinToString("\n")
}
