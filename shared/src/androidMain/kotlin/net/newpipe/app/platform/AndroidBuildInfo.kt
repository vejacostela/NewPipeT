/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.koin.core.annotation.Singleton

@Singleton(binds = [BuildInfo::class])
class AndroidBuildInfo(private val context: Context) : BuildInfo {

    override val isDebug: Boolean =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    override val isReleaseApk: Boolean by lazy {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getApplicationInfo(
            context.packageName, PackageManager.GET_META_DATA
        )
        !isDebug && info.metaData?.getBoolean("io.github.vejacostela.newpipet.release", false) == true
    }
}
