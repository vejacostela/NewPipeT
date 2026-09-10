/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.schabi.newpipe.updates

import com.grack.nanojson.JsonObject
import java.net.URI

data class ReleaseUpdate(val versionCode: Int, val versionName: String, val apkUrl: String) {
    companion object {
        const val REPOSITORY = "vejacostela/NewPipeT"
        const val APPLICATION_ID = "io.github.vejacostela.newpipet"

        fun isOwnedAsset(url: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme == "https" && uri.host == "github.com" && uri.port == -1 &&
                uri.userInfo == null && uri.query == null && uri.fragment == null &&
                uri.rawPath.startsWith("/$REPOSITORY/releases/download/") &&
                !uri.rawPath.contains("..") && !uri.rawPath.contains('%') &&
                uri.rawPath.removePrefix("/$REPOSITORY/releases/download/")
                    .matches(Regex("[A-Za-z0-9._-]+/[A-Za-z0-9._-]+"))
        }.getOrDefault(false)

        fun parse(json: JsonObject, allowBeta: Boolean): ReleaseUpdate {
            require(json.getInt("schema") == 1 && json.getString("kind") == "release")
            require(json.getString("application_id") == APPLICATION_ID)
            val channel = json.getString("channel")
            require(channel == "stable" || (allowBeta && channel == "beta"))
            val code = json.getInt("version_code")
            require(code in 1..2100000000)
            val name = json.getString("version_name")
            require(name.isNotBlank() && name.length <= 80)
            val apk = json.getString("apk_url")
            require(isOwnedAsset(apk) && apk.endsWith(".apk"))
            require(json.getString("apk_sha256").matches(Regex("[0-9a-f]{64}")))
            return ReleaseUpdate(code, name, apk)
        }
    }
}
