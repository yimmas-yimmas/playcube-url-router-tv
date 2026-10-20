package com.playcube.urlrouter

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.view.ViewGroup

class RouterActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("router_settings", MODE_PRIVATE) }

    private data class Player(val id: String, val label: String, val packageName: String)

    private val knownPlayers = listOf(
        Player("vlc", "VLC", "org.videolan.vlc"),
        Player("mpv", "mpv-android", "is.xyz.mpv"),
        Player("just", "Just+ Player", "pl.waskysoft.player")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data ?: run { finish(); return }
        if (uri.scheme?.lowercase() !in listOf("http", "https")) { finish(); return }
        val path = uri.path?.lowercase().orEmpty()
        if (!(path.endsWith(".m3u8") || path.endsWith("master.txt"))) {
            launchBrowser(uri)
            return
        }
        val players = availablePlayers(uri)
        val saved = prefs.getString("default_player", null)
        if (saved != null && players.any { it.id == saved } && launchPlayer(saved, uri)) {
            finish()
        } else {
            showPlayerDialog(uri, players)
        }
    }

    private fun availablePlayers(uri: Uri): List<Player> {
        val pm = packageManager
        val found = linkedMapOf<String, Player>()
        // Preserve the known, tested launch paths even when a player does not
        // advertise HLS intent filters.
        for (p in knownPlayers) {
            try {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(p.packageName, 0)
                found[p.id] = p
            } catch (_: android.content.pm.PackageManager.NameNotFoundException) { }
        }
        // Discover other installed applications that advertise HLS/video support.
        val probes = listOf("application/vnd.apple.mpegurl", "video/*")
        for (mime in probes) {
            val probe = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            @Suppress("DEPRECATION")
            val matches = pm.queryIntentActivities(probe, 0)
            for (match in matches) {
                val pkg = match.activityInfo?.packageName ?: continue
                if (pkg == packageName || pkg == "mark.via.gp" || found.values.any { it.packageName == pkg }) continue
                found[pkg] = Player(pkg, match.loadLabel(pm).toString(), pkg)
            }
        }
        return found.values.toList()
    }

    private fun showPlayerDialog(uri: Uri, players: List<Player>) {
        if (players.isEmpty()) {
            AlertDialog.Builder(this)
                .setMessage("No compatible video player found.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setOnCancelListener { finish() }
                .show()
            return
        }
        val remember = CheckBox(this).apply {
            text = "Always use selected player"
            setPadding(24, 12, 24, 12)
            isFocusable = true
        }
        val adapter = object : ArrayAdapter<Player>(this, android.R.layout.simple_list_item_1, players) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = LinearLayout(this@RouterActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(24, 12, 24, 12)
                    minimumHeight = 72
                }
                val player = players[position]
                val icon = ImageView(this@RouterActivity).apply {
                    try { setImageDrawable(packageManager.getApplicationIcon(player.packageName)) }
                    catch (_: Exception) { setImageResource(android.R.drawable.ic_media_play) }
                    layoutParams = LinearLayout.LayoutParams(48, 48).apply { rightMargin = 20 }
                }
                row.addView(icon)
                row.addView(TextView(this@RouterActivity).apply {
                    text = player.label + if (player.id == "just" && uri.path?.endsWith("master.txt") == true) " (master.txt may fail)" else ""
                    textSize = 18f
                })
                return row
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Open video with")
            .setAdapter(adapter) { dialog, which ->
                val p = players[which]
                if (launchPlayer(p.id, uri)) {
                    if (remember.isChecked) prefs.edit().putString("default_player", p.id).apply()
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
                else -> {
                    setPackage(id)
                    setDataAndType(uri, "application/vnd.apple.mpegurl")
                }
            }
        }
        return try {
            startActivity(playerIntent)
            true
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Cannot open player: ${e.javaClass.simpleName}", android.widget.Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun launchBrowser(uri: Uri) {
        val browsers = BrowserApps.installed(this)
        val choice = prefs.getString("browser_choice", "default") ?: "default"
        if (choice == "ask") {
            showBrowserDialog(uri, browsers)
            return
        }
        val selected = if (choice == "default") {
            BrowserApps.systemDefault(this, browsers)
        } else {
            browsers.firstOrNull { it.packageName == choice }
        }
        if (selected != null && openBrowser(uri, selected.packageName)) return
        // No default browser, or saved browser was uninstalled: ask rather than
        // allowing Android to route the link back into this activity.
        showBrowserDialog(uri, browsers)
    }

    private fun openBrowser(uri: Uri, browserPackage: String): Boolean {
        if (browserPackage == packageName) return false
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage(browserPackage)
            })
            finish()
            true
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot open browser", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun showBrowserDialog(uri: Uri, browsers: List<BrowserApps.Browser>) {
        if (browsers.isEmpty()) {
            AlertDialog.Builder(this).setMessage("No compatible browser found.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setOnCancelListener { finish() }.show()
            return
        }
        val remember = CheckBox(this).apply {
            text = "Always use selected browser"
            setPadding(24, 12, 24, 12)
            isFocusable = true
        }
        val adapter = object : ArrayAdapter<BrowserApps.Browser>(this, android.R.layout.simple_list_item_1, browsers) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = LinearLayout(this@RouterActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(24, 12, 24, 12)
                    minimumHeight = 72
                }
                val browser = browsers[position]
                row.addView(ImageView(this@RouterActivity).apply {
                    try { setImageDrawable(packageManager.getApplicationIcon(browser.packageName)) }
                    catch (_: Exception) { setImageResource(android.R.drawable.ic_menu_view) }
                    layoutParams = LinearLayout.LayoutParams(48, 48).apply { rightMargin = 20 }
                })
                row.addView(TextView(this@RouterActivity).apply {
                    text = browser.label
                    textSize = 18f
                })
                return row
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Open website with")
            .setAdapter(adapter) { _, which ->
                val browser = browsers[which]
                if (openBrowser(uri, browser.packageName) && remember.isChecked) {
                    prefs.edit().putString("browser_choice", browser.packageName).apply()
                }
            }
            .setView(remember)
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
