/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.updates

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.grack.nanojson.JsonObject
import java.time.Instant
import org.schabi.newpipe.BuildConfig

data class CompatibilityPolicy(
    val networkRetries: Int = 2,
    val refreshExpiredStreams: Boolean = true
)

data class PolicyDocument(
    val revision: Long,
    val expiresAt: Long,
    val policy: CompatibilityPolicy
) {
    companion object {
        fun parse(json: JsonObject, now: Long, versionCode: Int): PolicyDocument {
            require(json.getInt("schema") == 1 && json.getString("kind") == "compatibility")
            require(json.getString("application_id") == ReleaseUpdate.APPLICATION_ID)
            val revision = json.getLong("revision")
            val issued = json.getLong("issued_at")
            val expires = json.getLong("expires_at")
            require(revision > 0 && issued > 0 && issued <= now + 300)
            require(expires > now && expires > issued && expires - issued <= 7 * 86400)
            require(versionCode in json.getInt("min_version_code")..json.getInt("max_version_code"))
            val settings = json.getObject("playback")
            val retries = settings.getInt("network_retries")
            require(retries in 0..3)
            require(settings["refresh_expired_streams"] is Boolean)
            return PolicyDocument(
                revision, expires,
                CompatibilityPolicy(retries, settings.getBoolean("refresh_expired_streams"))
            )
        }
    }
}

/** Remote settings are opt-in, bounded, signed and never contain executable code. */
object CompatibilityPolicyStore {
    const val ENABLED = "newpipet_remote_compatibility"
    private const val DOCUMENT = "newpipet_compatibility_document"
    private const val REVISION = "newpipet_compatibility_revision"

    @JvmStatic
    fun current(context: Context): CompatibilityPolicy {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (!prefs.getBoolean(ENABLED, false)) return CompatibilityPolicy()
        val document = prefs.getString(DOCUMENT, null) ?: return CompatibilityPolicy()
        return runCatching { verify(context, document).policy }.getOrDefault(CompatibilityPolicy())
    }

    @Synchronized
    fun install(context: Context, envelope: String) {
        val document = verify(context, envelope)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        require(document.revision > prefs.getLong(REVISION, 0)) { "Stale policy revision" }
        prefs.edit {
            putString(DOCUMENT, envelope)
            putLong(REVISION, document.revision)
        }
    }

    private fun verify(context: Context, envelope: String): PolicyDocument = PolicyDocument.parse(
        SignedDocument.verify(envelope, OwnedUpdateClient.installedKey(context)),
        Instant.now().epochSecond, BuildConfig.VERSION_CODE
    )
}
