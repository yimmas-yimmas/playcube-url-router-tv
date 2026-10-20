
package com.playcube.urlrouter

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class InfoActivity : Activity() {
    private val prefs by lazy {
        getSharedPreferences("router_settings", MODE_PRIVATE)
    }

    companion object {
        private const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 16; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 28, 36, 28)
        }

        val status = TextView(this).apply {
            textSize = 18f
        }

        fun browserDescription(): String {
            val choice = prefs.getString("browser_choice", "default") ?: "default"
            return when (choice) {
                "default" -> "System default"
                "ask" -> "Ask every time"
                else -> BrowserApps.installed(this).firstOrNull { it.packageName == choice }?.label
                    ?: "Unavailable (will ask)"
            }
        }

        fun refresh() {
            val selected = when (
                prefs.getString("default_player", null)
            ) {
                "vlc" -> "VLC"
                "mpv" -> "mpv-android"
                "just" -> "Just+ Player"
                else -> "Ask every time"
            }

            status.text =
                "PlayCube URL Router\n\n" +
                "Video links: VLC, mpv, Just+\n" +
                "Websites: configurable browser\n" +
                "Default player: $selected\n" +
                "Website browser: ${browserDescription()}"
        }

        refresh()
        layout.addView(status)

        layout.addView(Button(this).apply {
            text = "Choose default video player"

            setOnClickListener {
                val labels = arrayOf(
                    "Ask every time",
                    "VLC",
                    "mpv-android",
                    "Just+ Player"
                )

                val ids = arrayOf(
                    null, "vlc", "mpv", "just"
                )

                AlertDialog.Builder(this@InfoActivity)
                    .setTitle("Default video player")
                    .setItems(labels) { _, index ->
                        prefs.edit().apply {
                            if (ids[index] == null)
                                remove("default_player")
                            else
                                putString("default_player", ids[index])
                        }.apply()

                        refresh()
                    }
                    .show()
            }
        })

        layout.addView(Button(this).apply {
            text = "Choose website browser"
            setOnClickListener {
                val browsers = BrowserApps.installed(this@InfoActivity)
                val labels = (listOf("System default", "Ask every time") + browsers.map { it.label }).toTypedArray()
                val ids = listOf("default", "ask") + browsers.map { it.packageName }
                AlertDialog.Builder(this@InfoActivity)
                    .setTitle("Website browser")
                    .setItems(labels) { _, index ->
                        prefs.edit().putString("browser_choice", ids[index]).apply()
                        refresh()
                    }
                    .show()
            }
        })

        layout.addView(TextView(this).apply {
            text = "\nHTTP User-Agent"
            textSize = 18f
        })

        val uaInput = EditText(this).apply {
            setSingleLine(false)
            minLines = 2
            setText(
                prefs.getString("user_agent", DEFAULT_UA)
            )
            setSelectAllOnFocus(true)
        }

        layout.addView(uaInput)

        fun currentUa(): String =
            uaInput.text.toString().trim()

        fun copy(value: String, label: String) {
            val clipboard = getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(label, value)
            )

            Toast.makeText(
                this,
                "$label copied",
                Toast.LENGTH_SHORT
            ).show()
        }

        layout.addView(Button(this).apply {
            text = "Save User-Agent"

            setOnClickListener {
                val ua = currentUa()

                if (
                    ua.isEmpty() ||
                    ua.contains('\n') ||
                    ua.contains('\r')
                ) {
                    Toast.makeText(
                        this@InfoActivity,
                        "Enter a single-line User-Agent",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    prefs.edit()
                        .putString("user_agent", ua)
                        .apply()

                    Toast.makeText(
                        this@InfoActivity,
                        "User-Agent saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Copy User-Agent for VLC"

            setOnClickListener {
                if (currentUa().isNotEmpty()) {
                    copy(currentUa(), "User-Agent")
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Copy mpv.conf setting"

            setOnClickListener {
                if (currentUa().isNotEmpty()) {
                    copy(
                        "user-agent=${currentUa()}",
                        "mpv.conf setting"
                    )
                }
            }
        })

        layout.addView(TextView(this).apply {
            text =
                "\nVLC: Paste the User-Agent into " +
                "VLC's HTTP User-Agent setting.\n\n" +
                "mpv: Paste the mpv.conf setting " +
                "into mpv's advanced configuration.\n\n" +
                "Just+: Custom User-Agent support " +
                "has not been verified.\n\n" +
                "Copying does not automatically " +
                "configure other players."
            textSize = 15f
        })

        val scroll = ScrollView(this).apply {
            addView(layout)
        }

        setContentView(scroll)
    }
}
