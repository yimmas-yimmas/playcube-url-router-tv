package com.playcube.urlrouter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager

/** Browser discovery is refreshed each time settings or a web link is opened. */
object BrowserApps {
    data class Browser(val packageName: String, val label: String)

    private fun probe(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }

    fun installed(context: Context): List<Browser> {
        val pm = context.packageManager
        val found = linkedMapOf<String, Browser>()
        for (url in listOf("https://www.example.com/", "http://www.example.com/")) {
            @Suppress("DEPRECATION")
            val matches = pm.queryIntentActivities(probe(url), PackageManager.MATCH_DEFAULT_ONLY)
            for (match in matches) {
                val pkg = match.activityInfo?.packageName ?: continue
                if (pkg == context.packageName || pkg == "android") continue
                // Skip Android's ResolverActivity/ChooserActivity and non-exported activities.
                if (match.activityInfo?.exported != true) continue
                if (match.activityInfo?.name?.contains("ResolverActivity") == true) continue
                if (match.activityInfo?.name?.contains("ChooserActivity") == true) continue
                found.putIfAbsent(pkg, Browser(pkg, match.loadLabel(pm).toString()))
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }

    fun systemDefault(context: Context, installed: List<Browser>): Browser? {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val resolved = pm.resolveActivity(probe("https://www.example.com/"), PackageManager.MATCH_DEFAULT_ONLY)
        val pkg = resolved?.activityInfo?.packageName ?: return null
        // Android may return the ResolverActivity if no system default is configured.
        return installed.firstOrNull { it.packageName == pkg }
    }
}
