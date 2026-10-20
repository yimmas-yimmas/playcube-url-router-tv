package com.playcube.urlrouter

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast

class RouterActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("router_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data ?: run { finish(); return }
        if (uri.scheme?.lowercase() !in listOf("http", "https")) { finish(); return }
        val path = uri.path?.lowercase().orEmpty()
        val isHls = path.endsWith(".m3u8") || path.endsWith("master.txt")
        if (!isHls) {
            launchBrowser(uri)
            finish()
            return
        }
        val saved = prefs.getString("default_player", null)
        if (saved != null && launchPlayer(saved, uri)) {
            finish()
        } else {
            showPlayerDialog(uri)
        }
    }

    private fun showPlayerDialog(uri: Uri) {
        val ids = arrayOf("vlc", "mpv", "just")
        val names = arrayOf("VLC", "mpv-android", "Just+ Player (may not play master.txt)")
        val remember = CheckBox(this).apply {
            text = "Always use selected player"
            setPadding(24, 12, 24, 12)
            isFocusable = true
        }
        AlertDialog.Builder(this)
            .setTitle("Open video with")
            .setSingleChoiceItems(names, -1) { dialog, which ->
                val id = ids[which]
                if (launchPlayer(id, uri)) {
                    if (remember.isChecked) prefs.edit().putString("default_player", id).apply()
                    dialog.dismiss()
                    finish()
                }
            }
            .setView(remember)
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun launchPlayer(id: String, uri: Uri): Boolean {
        val playerIntent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            when (id) {
                "vlc" -> {
                    setClassName("org.videolan.vlc", "org.videolan.vlc.StartActivity")
                    setDataAndType(uri, "application/vnd.apple.mpegurl")
                }
                "mpv" -> {
                    setPackage("is.xyz.mpv")
                    setDataAndType(uri, "application/vnd.apple.mpegurl")
                }
                "just" -> {
                    setClassName("pl.waskysoft.player", "com.brouken.player.PlayerActivity")
                    setDataAndType(uri, "application/vnd.apple.mpegurl")
                }
                else -> return false
            }
        }
        return try {
            startActivity(playerIntent)
            true
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open $id: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun launchBrowser(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("mark.via.gp"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Via Browser not installed", Toast.LENGTH_LONG).show()
        }
    }
}
