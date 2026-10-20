package com.playcube.urlrouter

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button

class InfoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("router_settings", MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
        }
        val status = TextView(this).apply { textSize = 20f }
        fun refresh() {
            status.text = "PlayCube URL Router\n\nVideo (.m3u8 / master.txt): VLC, mpv or Just+\nOther links: Via Browser\n\nDefault player: ${prefs.getString("default_player", null) ?: "Ask every time"}\n\nOpen a video link to choose a player."
        }
        refresh()
        val change = Button(this).apply {
            text = "Choose default video player"
            setOnClickListener {
                val labels = arrayOf("Ask every time", "VLC", "mpv-android", "Just+ Player")
                val ids = arrayOf(null, "vlc", "mpv", "just")
                AlertDialog.Builder(this@InfoActivity).setTitle("Default video player")
                    .setItems(labels) { _, index ->
                        prefs.edit().apply {
                            if (ids[index] == null) remove("default_player")
                            else putString("default_player", ids[index])
                        }.apply()
                        refresh()
                    }.show()
            }
        }
        layout.addView(status)
        layout.addView(change)
        setContentView(layout)
    }
}
