/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.updates

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

/** A signature is checked before any remote value can affect the app. */
object SignedDocument {
    const val MAX_BYTES = 131072

    fun verify(envelope: String, key: PublicKey): JsonObject {
        require(envelope.length <= MAX_BYTES) { "Document too large" }
        require(key.algorithm == "RSA") { "Unsupported signing key" }
        val json = JsonParser.`object`().from(envelope)
        val payload = Base64.getDecoder().decode(json.getString("payload"))
        val signature = Base64.getDecoder().decode(json.getString("signature"))
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(key)
        verifier.update(payload)
        require(verifier.verify(signature)) { "Invalid document signature" }
        return JsonParser.`object`().from(payload.toString(Charsets.UTF_8))
    }
}
