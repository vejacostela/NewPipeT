/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.updates

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

class OwnedUpdateClient(context: Context) {
    private val key = installedKey(context)
    private val client = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Only this repository's signed release assets are considered. */
    fun latest(allowBeta: Boolean): ReleaseUpdate? {
        val releases = JsonParser.array().from(
            getText("https://api.github.com/repos/${ReleaseUpdate.REPOSITORY}/releases?per_page=10")
                ?: return null
        )
        val candidates = releases.filterIsInstance<JsonObject>()
            .filter { !it.getBoolean("draft") && (allowBeta || !it.getBoolean("prerelease")) }
            .take(5)
        val updates = candidates.mapNotNull { release ->
            val asset = release.getArray("assets").filterIsInstance<JsonObject>()
                .firstOrNull { it.getString("name") == "newpipet-update.json" }
                ?: return@mapNotNull null
            val url = asset.getString("browser_download_url")
            require(ReleaseUpdate.isOwnedAsset(url))
            val envelope = getText(url) ?: return@mapNotNull null
            ReleaseUpdate.parse(SignedDocument.verify(envelope, key), allowBeta)
        }
        return updates.maxByOrNull { it.versionCode }
    }

    fun compatibilityDocument(): String? = getText(
        "https://github.com/${ReleaseUpdate.REPOSITORY}/releases/download/compatibility/compatibility.json"
    )?.also { SignedDocument.verify(it, key) }

    private fun getText(url: String): String? {
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) throw IOException("Update server HTTP ${response.code}")
            val source = response.body.source()
            if (source.request(SignedDocument.MAX_BYTES.toLong() + 1)) {
                throw IOException("Update document too large")
            }
            return source.readUtf8()
        }
    }

    companion object {
        @Suppress("DEPRECATION")
        fun installedKey(context: Context): PublicKey {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo!!.apkContentsSigners
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures!!
            }
            require(signatures.size == 1) { "A single release signing key is required" }
            return CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(signatures.single().toByteArray()))
                .publicKey
        }
    }
}
