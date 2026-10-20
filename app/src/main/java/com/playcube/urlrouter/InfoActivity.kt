package com.playcube.urlrouter

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class InfoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this).apply {
            text = "PlayCube URL Router\n\nShare an HTTP/HTTPS URL to this app.\n\n.m3u8 and /master.txt → Just+ Player\nOther links → Via Browser\n\nChoose PlayCube URL Router in Android's Open with dialog.\n\nPress Back to exit."
            textSize = 20f
            setPadding(48, 48, 48, 48)
        }
        setContentView(view)
    }
}
