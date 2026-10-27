package com.playcube.urlrouter

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.net.NetworkInterface

class InfoActivity : Activity() {
    private var returningFromOverlaySettings = false
    private val prefs by lazy { getSharedPreferences("router_settings", MODE_PRIVATE) }

    companion object {
        private const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 16; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Default is ON. Launching the app also restores the receiver if Android
        // previously terminated its process. A manual OFF selection is respected.
        LinkReceiverService.startIfEnabled(this)

        val pendingUrl = prefs.getString("pending_received_url", null)
        if (!pendingUrl.isNullOrBlank()) {
            prefs.edit().remove("pending_received_url").commit()
            startActivity(Intent(this, RouterActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(pendingUrl)
            })
            finish()
            return
        }

        setContentView(buildScreen())
    }

    override fun onResume() {
        super.onResume()
        if (returningFromOverlaySettings) {
            returningFromOverlaySettings = false
            recreate()
        }
    }

    private fun buildScreen(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(7, 16, 30))
            isFillViewport = true
        }

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(18))
        }
        scroll.addView(outer)

        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(8, 20, 39), Color.rgb(9, 24, 49), Color.rgb(7, 18, 34))
            ).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Color.argb(120, 79, 110, 160))
            }
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }
        outer.addView(shell, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        shell.addView(buildHeader())
        shell.addView(space(18))

        val contentRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        shell.addView(contentRow)

        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, 0, 0)
        }

        contentRow.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        contentRow.addView(right, LinearLayout.LayoutParams(dp(330), ViewGroup.LayoutParams.WRAP_CONTENT))

        // Keep remote DPAD navigation in a predictable top-to-bottom order.
        // Focus styling is applied to the entire card, not just its text.
        val remoteRows = listOf(
            buildReceiverCard(),
            buildActionCard(
                "▣",
                "Background launch permission",
                "Allow VLC/browser to open while Receiver is in background",
                if (Settings.canDrawOverlays(this)) "Allowed" else "Not granted"
            ) { openBackgroundLaunchSettings() },
            buildActionCard("◎", "Open in", "Choose default app for links", defaultPlayerLabel()) { chooseDefaultPlayer() },
            buildActionCard("◈", "Access Token", "Used to authenticate incoming links", LinkReceiverService.token(this)) { tokenActions() },
            buildActionCard("↻", "Refresh QR / IP address", "Update local IP and regenerate QR code", "") { recreate() },
            buildActionCard("◷", "Open Last Received Link", "Launch the most recent URL", "") { openLastReceived() },
            buildActionCard("⚙", "Advanced Settings", "Notifications, browser and User-Agent", "") { advancedSettings() }
        )
        remoteRows.forEachIndexed { index, view ->
            view.id = View.generateViewId()
            if (index > 0) left.addView(space(10))
            left.addView(view)
        }
        remoteRows.forEachIndexed { index, view ->
            if (index > 0) view.nextFocusUpId = remoteRows[index - 1].id
            if (index < remoteRows.lastIndex) view.nextFocusDownId = remoteRows[index + 1].id
        }

        val ip = localIp() ?: "Not connected"
        val token = LinkReceiverService.token(this)
        right.addView(buildQrPanel(ip, token))
        right.addView(space(14))
        right.addView(buildInfoPanel(ip, token))
        right.addView(space(12))
        right.addView(bodyText("Make sure your phone and tvBOX are on the same Wi-Fi network.", 13f))

        // Start with a visibly selected item on the television.
        scroll.post { if (!isFinishing) remoteRows.first().requestFocus() }
        return scroll
    }

    private fun buildHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.router_icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(68), dp(68)))

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, 0, 0)
        }
        textCol.addView(titleText("tvBOX Receiver", 30f, true))
        textCol.addView(bodyText("Wi-Fi URL receiver for VLC, mpv and browser", 16f))
        row.addView(textCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val version = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) { "1.0.0" }
        row.addView(bodyText("Version $version", 14f, Color.rgb(196, 209, 235)))
        return row
    }

    private fun buildReceiverCard(): View {
        val card = baseCard()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val leftIcon = iconText("◔")
        row.addView(leftIcon)
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(12), 0)
        }
        textCol.addView(titleText("Wi-Fi Receiver", 17f, true))
        textCol.addView(bodyText("Receive URLs from Mobile Phone", 13f))
        row.addView(textCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val toggle = Switch(this).apply {
            isChecked = LinkReceiverService.isEnabled(this@InfoActivity)
            // The remote should focus the whole highlighted row, not this tiny switch.
            isFocusable = false
            isClickable = false
            setOnCheckedChangeListener { _, checked -> toggleReceiver(checked) }
        }
        row.addView(toggle)
        card.addView(row)
        enableRemoteFocus(card)
        card.setOnClickListener { toggle.isChecked = !toggle.isChecked }
        return card
    }

    private fun buildActionCard(symbol: String, title: String, subtitle: String, trailing: String, onClick: () -> Unit): View {
        val card = baseCard()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(iconText(symbol))

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(12), 0)
        }
        textCol.addView(titleText(title, 17f, true))
        textCol.addView(bodyText(subtitle, 13f))
        row.addView(textCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val trailCol = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (trailing.isNotEmpty()) {
            trailCol.addView(bodyText(trailing, 16f, Color.rgb(218, 228, 245), false, Gravity.END))
            trailCol.addView(spaceW(10))
        }
        trailCol.addView(bodyText("›", 24f, Color.rgb(141, 167, 210)))
        row.addView(trailCol)

        card.addView(row)
        enableRemoteFocus(card)
        card.setOnClickListener { onClick() }
        return card
    }

    private fun buildQrPanel(ip: String, token: String): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        panel.addView(titleText("Scan QR with your phone", 18f, true, gravity = Gravity.CENTER_HORIZONTAL))
        panel.addView(space(10))

        val qrHolder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(14).toFloat()
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        val qr = ImageView(this).apply {
            val size = dp(180)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setImageBitmap(buildQrBitmap("http://$ip:${LinkReceiverService.PORT}/?token=$token", 420))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        qrHolder.addView(qr)
        panel.addView(qrHolder)
        panel.addView(space(10))
        panel.addView(bodyText("Opens URL sender with IP and token automatically filled in.", 13f, gravity = Gravity.CENTER_HORIZONTAL))
        return panel
    }

    private fun buildInfoPanel(ip: String, token: String): View {
        val panel = baseCard()
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(infoRow("IP Address", ip) { copy(ip, "IP address") })
        col.addView(separator())
        col.addView(infoRow("Access Token", token) { copy(token, "Access token") })
        panel.addView(col)
        return panel
    }

    private fun infoRow(label: String, value: String, onCopy: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(bodyText(label, 13f, Color.rgb(183, 198, 224)))
        col.addView(titleText(value, 18f, true))
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val copyBtn = Button(this).apply {
            text = "Copy"
            isAllCaps = false
            textSize = 12f
            setTextColor(Color.WHITE)
            backgroundTintList = null
            background = focusableCopyButtonBackground()
            isFocusable = true
            setOnClickListener { onCopy() }
        }
        row.addView(copyBtn, LinearLayout.LayoutParams(dp(74), dp(38)))
        return row
    }

    private fun baseCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = remoteCardBackground()
        setPadding(dp(18), dp(16), dp(18), dp(16))
    }

    // Native state drawable responds immediately to DPAD focus changes.
    // The cyan outline and brighter blue surface make the selected row obvious.
    private fun remoteCardBackground(): StateListDrawable {
        fun shape(selected: Boolean): GradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            if (selected) intArrayOf(Color.rgb(35, 83, 153), Color.rgb(24, 64, 125))
            else intArrayOf(Color.rgb(24, 38, 61), Color.rgb(18, 31, 50))
        ).apply {
            cornerRadius = dp(16).toFloat()
            setStroke(
                dp(if (selected) 3 else 1),
                if (selected) Color.rgb(70, 212, 255) else Color.argb(90, 115, 145, 190)
            )
        }
        val highlighted = shape(true)
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), highlighted)
            addState(intArrayOf(android.R.attr.state_pressed), highlighted)
            addState(intArrayOf(), shape(false))
        }
    }

    private fun focusableCopyButtonBackground(): StateListDrawable {
        fun shape(selected: Boolean) = GradientDrawable().apply {
            setColor(if (selected) Color.rgb(34, 118, 217) else Color.rgb(39, 59, 90))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(if (selected) 3 else 1), if (selected) Color.rgb(82, 220, 255) else Color.rgb(74, 100, 150))
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), shape(true))
            addState(intArrayOf(android.R.attr.state_pressed), shape(true))
            addState(intArrayOf(), shape(false))
        }
    }

    private fun enableRemoteFocus(view: View) {
        view.isFocusable = true
        view.isClickable = true
        view.isFocusableInTouchMode = true
        view.setOnFocusChangeListener { selectedView, focused ->
            selectedView.animate().scaleX(if (focused) 1.015f else 1f)
                .scaleY(if (focused) 1.015f else 1f).setDuration(110).start()
            selectedView.elevation = if (focused) dp(10).toFloat() else 0f
        }
    }

    private fun toggleReceiver(checked: Boolean) {
        // Persist the user's choice; it survives app updates and reboots.
        prefs.edit().putBoolean(LinkReceiverService.PREF_ENABLED, checked).apply()
        if (checked) {
            LinkReceiverService.startIfEnabled(this)
            Toast.makeText(this, "Receiver enabled on port ${LinkReceiverService.PORT}", Toast.LENGTH_SHORT).show()
        } else {
            stopService(Intent(this, LinkReceiverService::class.java))
            Toast.makeText(this, "Receiver disabled", Toast.LENGTH_SHORT).show()
        }
        recreate()
    }

    private fun openLastReceived() {
        val url = prefs.getString("last_received_url", null)
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "No received link yet", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, RouterActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
        })
    }

    private fun defaultPlayerLabel(): String = when (prefs.getString("default_player", null)) {
        "vlc" -> "VLC"
        "mpv" -> "mpv"
        "just" -> "Just+"
        else -> "Ask every time"
    }

    private fun chooseDefaultPlayer() {
        val labels = arrayOf("Ask every time", "VLC", "mpv-android", "Just+ Player")
        val ids = arrayOf<String?>(null, "vlc", "mpv", "just")
        AlertDialog.Builder(this)
            .setTitle("Default video player")
            .setItems(labels) { _, index ->
                prefs.edit().apply {
                    if (ids[index] == null) remove("default_player") else putString("default_player", ids[index])
                }.apply()
                recreate()
            }
            .show()
    }

    private fun tokenActions() {
        val token = LinkReceiverService.token(this)
        AlertDialog.Builder(this)
            .setTitle("Access token")
            .setMessage(token)
            .setNeutralButton("Copy") { _, _ -> copy(token, "Access token") }
            .setNegativeButton("Close", null)
            .setPositiveButton("Regenerate") { _, _ ->
                prefs.edit().remove("receiver_token").apply()
                Toast.makeText(this, "Access token regenerated", Toast.LENGTH_SHORT).show()
                recreate()
            }
            .show()
    }

    private fun openBackgroundLaunchSettings() {
        if (Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Background launch permission")
                .setMessage("Display over other apps permission is already granted. Android TV may still impose additional restrictions.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Allow background launching")
            .setMessage(
                "Android 14 is currently blocking tvBOX Receiver from opening VLC or a browser while in the background. " +
                "Grant 'Display over other apps' in system settings to test Android's permitted background-launch exception. " +
                "This is a powerful permission: grant it only because you trust this app."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open settings") { _, _ ->
                returningFromOverlaySettings = true
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    })
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                    } catch (_: Exception) {
                        try {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            })
                        } catch (_: Exception) {
                            returningFromOverlaySettings = false
                            Toast.makeText(this, "System permission screen is unavailable. Use ADB to grant it.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun advancedSettings() {
        val items = arrayOf(
            "Choose website browser",
            "Edit HTTP User-Agent",
            "Copy User-Agent for VLC",
            "Copy mpv.conf setting",
            "Start Wi-Fi receiver",
            "Stop Wi-Fi receiver",
            "Background launch permission"
        )
        AlertDialog.Builder(this)
            .setTitle("Advanced settings")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> chooseBrowser()
                    1 -> editUserAgent()
                    2 -> copy(currentUa(), "User-Agent")
                    3 -> copy("user-agent=${currentUa()}", "mpv.conf setting")
                    4 -> toggleReceiver(true)
                    5 -> toggleReceiver(false)
                    6 -> openBackgroundLaunchSettings()
                }
            }
            .show()
    }

    private fun chooseBrowser() {
        val browsers = BrowserApps.installed(this)
        val labels = (listOf("System default", "Ask every time") + browsers.map { it.label }).toTypedArray()
        val ids = listOf("default", "ask") + browsers.map { it.packageName }
        AlertDialog.Builder(this)
            .setTitle("Website browser")
            .setItems(labels) { _, index ->
                prefs.edit().putString("browser_choice", ids[index]).apply()
                recreate()
            }
            .show()
    }

    private fun editUserAgent() {
        val input = EditText(this).apply {
            setText(currentUa())
            setSingleLine(false)
            minLines = 3
            setSelectAllOnFocus(true)
        }
        AlertDialog.Builder(this)
            .setTitle("HTTP User-Agent")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val ua = input.text.toString().trim()
                if (ua.isEmpty() || ua.contains('\n') || ua.contains('\r')) {
                    Toast.makeText(this, "Enter a single-line User-Agent", Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString("user_agent", ua).apply()
                    Toast.makeText(this, "User-Agent saved", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun currentUa(): String = prefs.getString("user_agent", DEFAULT_UA) ?: DEFAULT_UA

    private fun localIp(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<java.net.Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }?.hostAddress
    } catch (_: Exception) { null }

    private fun buildQrBitmap(content: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) for (x in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
        return bitmap
    }

    private fun copy(value: String, label: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
    }

    private fun titleText(text: String, size: Float, bold: Boolean = false, color: Int = Color.WHITE, gravity: Int = Gravity.START): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            this.gravity = gravity
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun bodyText(text: String, size: Float, color: Int = Color.rgb(177, 194, 220), bold: Boolean = false, gravity: Int = Gravity.START): TextView =
        titleText(text, size, bold, color, gravity)

    private fun iconText(symbol: String): TextView = TextView(this).apply {
        text = symbol
        textSize = 28f
        setTextColor(Color.rgb(117, 169, 255))
        gravity = Gravity.CENTER
    }

    private fun buttonBg(primary: Boolean): GradientDrawable = GradientDrawable().apply {
        setColor(if (primary) Color.rgb(55, 106, 242) else Color.rgb(39, 59, 90))
        cornerRadius = dp(14).toFloat()
    }

    private fun separator(): View = View(this).apply {
        setBackgroundColor(Color.argb(70, 152, 170, 205))
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
    }

    private fun space(heightDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    private fun spaceW(widthDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(widthDp), 1)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
