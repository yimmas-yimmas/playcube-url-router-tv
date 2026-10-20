package com.playcube.urlrouter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class RouterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri: Uri = intent?.data ?: run {
            finish()
            return
        }

        val scheme = uri.scheme?.lowercase()

        if (scheme != "http" && scheme != "https") {
            finish()
            return
        }

        val path = uri.path?.lowercase() ?: ""

        val isHls =
            path.endsWith(".m3u8") ||
            path.endsWith("/master.txt") ||
            path.endsWith("master.txt")

        if (isHls) {

            // Send HLS URL directly to Just+ Player.
            // This reproduces:
            // adb shell am start -a android.intent.action.VIEW \
            // -d "URL" -p pl.waskysoft.player

            val playerIntent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                setPackage("pl.waskysoft.player")
            }

            try {
                startActivity(playerIntent)
            } catch (e: Exception) {
                // Just+ Player not installed or unable to handle URL
            }

            finish()
            return
        }

        // Normal website -> Via Browser
        val browserIntent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            setPackage("mark.via.gp")
        }

        try {
            startActivity(browserIntent)
        } catch (e: Exception) {
            // Via Browser not installed
        }

        finish()
    }
}
