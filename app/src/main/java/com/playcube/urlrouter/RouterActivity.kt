package com.playcube.urlrouter

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import java.util.Locale

class RouterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        route(intent)
        finish()
    }

    private fun route(incoming: Intent?) {
        val uri: Uri = incoming?.data ?: run {
            Toast.makeText(this, "No URL received", Toast.LENGTH_SHORT).show()
            return
        }
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("https", "http")) return
        val path = (uri.encodedPath ?: "").lowercase(Locale.ROOT)
        val isHls = path.endsWith(".m3u8") || path.endsWith("/master.txt")
        if (isHls) {
            val player = Intent(Intent.ACTION_VIEW).apply {
                setClassName("pl.waskysoft.player", "com.brouken.player.PlayerActivity")
                setDataAndType(uri, "application/vnd.apple.mpegurl")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(player)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Just+ Player is not installed or cannot open URL", Toast.LENGTH_LONG).show()
            } catch (e: SecurityException) {
                Toast.makeText(this, "Just+ Player refused the intent", Toast.LENGTH_LONG).show()
            }
        } else {
            val browser = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("mark.via.gp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(browser)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Via Browser is not installed", Toast.LENGTH_LONG).show()
            } catch (e: SecurityException) {
                Toast.makeText(this, "Via Browser refused the intent", Toast.LENGTH_LONG).show()
            }
        }
    }
}
