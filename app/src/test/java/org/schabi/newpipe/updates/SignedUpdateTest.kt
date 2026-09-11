/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.updates

import com.grack.nanojson.JsonParser
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SignedUpdateTest {
    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val payload = """{"schema":1,"kind":"release","application_id":"io.github.vejacostela.newpipet","channel":"stable","version_code":1100,"version_name":"0.30.0","apk_url":"https://github.com/vejacostela/NewPipeT/releases/download/v0.30.0/NewPipeT.apk","apk_sha256":"${"a".repeat(64)}"}"""

    private fun envelope(text: String): String {
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(keys.private)
        signer.update(text.toByteArray())
        return """{"payload":"${Base64.getEncoder().encodeToString(text.toByteArray())}","signature":"${Base64.getEncoder().encodeToString(signer.sign())}"}"""
    }

    @Test fun `accept a manifest signed by installed key`() {
        val document = SignedDocument.verify(envelope(payload), keys.public)
        assertEquals(1100, ReleaseUpdate.parse(document, false).versionCode)
    }

    @Test fun `reject a different signing key`() {
        val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        assertThrows(IllegalArgumentException::class.java) {
            SignedDocument.verify(envelope(payload), other.public)
        }
    }

    @Test fun `reject tampering after signing`() {
        val signed = envelope(payload).replace(
            Base64.getEncoder().encodeToString(payload.toByteArray()),
            Base64.getEncoder().encodeToString(payload.replace("1100", "9999").toByteArray())
        )
        assertThrows(IllegalArgumentException::class.java) { SignedDocument.verify(signed, keys.public) }
    }

    @Test fun `reject cross account and ambiguous asset URLs`() {
        listOf(
            "http://github.com/vejacostela/NewPipeT/releases/download/v1/a.apk",
            "https://github.com/other/NewPipeT/releases/download/v1/a.apk",
            "https://github.com@evil.example/vejacostela/NewPipeT/releases/download/v1/a.apk",
            "https://github.com/vejacostela/NewPipeT/releases/download/../a.apk",
            "https://github.com/vejacostela/NewPipeT/releases/download/v1/a.apk?redirect=1",
            "https://github.com/vejacostela/NewPipeT/releases/download/v1/%2e%2e.apk"
        ).forEach { assertFalse(it, ReleaseUpdate.isOwnedAsset(it)) }
        assertTrue(ReleaseUpdate.isOwnedAsset("https://github.com/vejacostela/NewPipeT/releases/download/v1/a.apk"))
    }

    @Test fun `stable excludes beta and foreign applications`() {
        for (changed in listOf(payload.replace("stable", "beta"), payload.replace("io.github.vejacostela.newpipet", "org.schabi.newpipe"))) {
            assertThrows(IllegalArgumentException::class.java) {
                ReleaseUpdate.parse(JsonParser.`object`().from(changed), false)
            }
        }
        assertEquals(1100, ReleaseUpdate.parse(JsonParser.`object`().from(payload.replace("stable", "beta")), true).versionCode)
    }

    @Test fun `policy requires expiry bounds and compatible application version`() {
        val text = """{"schema":1,"kind":"compatibility","application_id":"io.github.vejacostela.newpipet","revision":1,"issued_at":1000,"expires_at":5000,"min_version_code":1000,"max_version_code":2000,"playback":{"network_retries":2,"refresh_expired_streams":true}}"""
        assertEquals(2, PolicyDocument.parse(JsonParser.`object`().from(text), 2000, 1500).policy.networkRetries)
        assertThrows(IllegalArgumentException::class.java) { PolicyDocument.parse(JsonParser.`object`().from(text), 6000, 1500) }
        assertThrows(IllegalArgumentException::class.java) { PolicyDocument.parse(JsonParser.`object`().from(text), 2000, 999) }
        assertThrows(IllegalArgumentException::class.java) {
            PolicyDocument.parse(JsonParser.`object`().from(text.replace("\"network_retries\":2", "\"network_retries\":99")), 2000, 1500)
        }
    }
}
