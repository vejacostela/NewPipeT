/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.error

/** Only exception types and stack frames leave the device in exported crash reports. */
object DiagnosticRedactor {
    private val frame = Regex("at [A-Za-z_$][A-Za-z0-9_.$]*\\((?:[A-Za-z0-9_.$-]+(?::[0-9]+)?|Native Method|Unknown Source)\\)")
    private val omitted = Regex("\\.\\.\\. [0-9]+ more")

    fun stackTrace(trace: String): String = trace.lineSequence().mapNotNull { line ->
        val trimmed = line.trim()
        when {
            frame.matches(trimmed) -> trimmed

            omitted.matches(trimmed) -> trimmed

            else -> {
                val name = trimmed.removePrefix("Caused by: ").removePrefix("Suppressed: ")
                    .substringBefore(':')
                name.takeIf { it.matches(Regex("[A-Za-z0-9_.$]+(?:Exception|Error)")) }
            }
        }
    }.joinToString("\n")
}
