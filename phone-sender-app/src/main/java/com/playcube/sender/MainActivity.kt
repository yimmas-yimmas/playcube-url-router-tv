package com.playcube.sender

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(7, 18, 32)
    private val surface = Color.rgb(24, 42, 62)
    private val muted = Color.rgb(164, 185, 208)
    private val blue = Color.rgb(45, 124, 255)
    private val worker = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("pair", MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private var pendingShare: String? = null
    private var sending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingShare = extractSharedUrl(intent)
        drawScreen()
        if (pendingShare != null) {
            if (isPaired()) send(pendingShare!!, true)
            else { status.text = "Pair once to send this shared link automatically."; scanQr() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShare = extractSharedUrl(intent)
        if (pendingShare != null) {
            if (isPaired()) send(pendingShare!!, true)
            else { status.text = "Pair once to send this shared link automatically."; scanQr() }
        }
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val raw = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        return Regex("https?://[^\\s<>\\\"']+", RegexOption.IGNORE_CASE).find(raw)?.value?.trimEnd('.', ',', ')')
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    private fun shape(color: Int, radius: Int = 18, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }
    private fun text(value: String, size: Float = 15f, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun pad(v: View, a: Int = 16, b: Int = a) { v.setPadding(dp(a), dp(b), dp(a), dp(b)) }
    private fun addSpace(height: Int) { content.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun card(title: String, subtitle: String, symbol: String, onClick: () -> Unit) {
        val r = row().apply { background = shape(surface); pad(this, 16, 18); isClickable = true; isFocusable = true; setOnClickListener { onClick() } }
        val glyph = text(symbol, 25f, Color.rgb(93, 167, 255)).apply { gravity = Gravity.CENTER }
        r.addView(glyph, LinearLayout.LayoutParams(dp(40), dp(46)))
        val c = column().apply { setPadding(dp(12), 0, 0, 0) }
        c.addView(text(title, 16f, Color.WHITE, true))
        c.addView(text(subtitle, 12f, muted))
        r.addView(c, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r.addView(text("›", 26f, muted))
        content.addView(r, LinearLayout.LayoutParams(-1, -2)); addSpace(10)
    }
    private fun button(label: String, primary: Boolean = true, onClick: () -> Unit): Button = Button(this).apply {
        text = label; isAllCaps = false; textSize = 15f; setTextColor(Color.WHITE)
        background = shape(if (primary) blue else surface, 16)
        setOnClickListener { onClick() }
        minHeight = dp(54)
    }
    private fun drawScreen() {
        val scroller = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(bg) }
        content = column().apply { pad(this, 22, 24) }
        scroller.addView(content); setContentView(scroller)
        val header = row()
        val logo = ImageView(this).apply { setImageResource(R.drawable.sender_logo); scaleType = ImageView.ScaleType.FIT_CENTER }
        header.addView(logo, LinearLayout.LayoutParams(dp(64), dp(64)))
        val heading = column().apply { setPadding(dp(14), 0, 0, 0) }
        heading.addView(text("Send to", 24f, Color.WHITE, true))
        heading.addView(text("tvBOX", 28f, Color.rgb(70, 164, 255), true))
        header.addView(heading)
        content.addView(header); addSpace(8)
        content.addView(text("Send links. Watch on your TV.", 15f, muted))
        addSpace(26)
        val paired = isPaired()
        val connection = column().apply { background = shape(surface); pad(this, 18) }
        connection.addView(text(if (paired) "●  tvBOX paired" else "○  Not connected", 19f, if (paired) Color.rgb(71, 228, 142) else Color.WHITE, true))
        connection.addView(text(if (paired) "tvBOX address: ${prefs.getString("host", "")}:8765" else "Scan the QR code on your TV to pair.", 13f, muted))
        content.addView(connection); addSpace(14)
        content.addView(button(if (paired) "Scan new tvBOX QR" else "Scan QR Code") { scanQr() }, LinearLayout.LayoutParams(-1, dp(54)))
        addSpace(12)
        if (paired) {
            content.addView(button("Test connection", false) { send("https://example.com", false) }, LinearLayout.LayoutParams(-1, dp(54)))
            addSpace(18)
        }
        content.addView(text("QUICK ACTIONS", 13f, muted, true)); addSpace(12)
        card("Send URL", "Paste or type a web or video link", "↗") { manualSend() }
        card("From clipboard", "Send a copied URL instantly", "▤") { clipboardSend() }
        card("Recent links", "Re-send previously shared links", "◷") { showRecent() }
        card("Pairing settings", "Scan QR or forget this TV", "⚙") { pairingSettings() }
        addSpace(10)
        status = text(if (paired) "Ready to receive shared links." else "Pair once, then Share → Send to tvBOX.", 13f, muted)
        content.addView(status)
        addSpace(12)
        content.addView(text("Phone and TV must be on the same Wi-Fi. Keep the TV receiver running.", 12f, muted))
    }
    private fun isPaired() = !prefs.getString("host", null).isNullOrBlank() && !prefs.getString("token", null).isNullOrBlank()
    private fun scanQr() {
        status.text = "Opening QR scanner…"
        GmsBarcodeScanning.getClient(this).startScan()
            .addOnSuccessListener { code ->
                val uri = try { Uri.parse(code.rawValue.orEmpty()) } catch (_: Exception) { null }
                val host = uri?.host
                val token = uri?.getQueryParameter("token")
                val octets = host?.split('.')?.mapNotNull { it.toIntOrNull() }.orEmpty()
                val local = octets.size == 4 && octets.all { it in 0..255 } &&
                    (octets[0] == 10 || (octets[0] == 172 && octets[1] in 16..31) || (octets[0] == 192 && octets[1] == 168))
                if (uri?.scheme != "http" || uri.port != 8765 || !local || token.isNullOrBlank()) {
                    status.text = "Invalid tvBOX QR code. Scan the code displayed by the TV Router."
                    return@addOnSuccessListener
                }
                prefs.edit().putString("host", host).putString("token", token).apply()
                drawScreen()
                val share = pendingShare
                if (share != null) send(share, true)
                else status.text = "Paired! Share any link using Send to tvBOX."
            }
            .addOnCanceledListener { status.text = "QR scan cancelled." }
            .addOnFailureListener { status.text = "Scanner unavailable: ${it.localizedMessage}" }
    }
    private fun manualSend() {
        val input = EditText(this).apply { hint = "https://example.com/video.m3u8"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI; setSingleLine(false) }
        AlertDialog.Builder(this).setTitle("Send URL").setView(input)
            .setNegativeButton("Cancel", null).setPositiveButton("Send") { _, _ -> send(input.text.toString().trim(), false) }.show()
    }
    private fun clipboardSend() {
        val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
        val raw = clip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        val url = Regex("https?://[^\\s<>\\\"']+", RegexOption.IGNORE_CASE).find(raw)?.value
        if (url == null) status.text = "No web URL found in clipboard." else send(url, false)
    }
    private fun pairingSettings() {
        AlertDialog.Builder(this).setTitle("Pairing settings")
            .setItems(arrayOf("Scan new QR code", "Forget paired tvBOX")) { _, which ->
                if (which == 0) scanQr() else AlertDialog.Builder(this).setTitle("Forget this TV?")
                    .setMessage("You'll need to scan its QR code again.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Forget") { _, _ -> prefs.edit().remove("host").remove("token").apply(); drawScreen() }.show()
            }.show()
    }
    private fun history(): JSONArray = try { JSONArray(prefs.getString("recent", "[]")) } catch (_: Exception) { JSONArray() }
    private fun saveHistory(url: String) {
        val old = history(); val updated = JSONArray().put(url)
        for (i in 0 until old.length()) {
            val item = old.optString(i)
            if (item != url && updated.length() < 10) updated.put(item)
        }
        prefs.edit().putString("recent", updated.toString()).apply()
    }
    private fun showRecent() {
        val history = history()
        if (history.length() == 0) { status.text = "No recent links yet."; return }
        val items = Array(history.length()) { history.optString(it) }
        AlertDialog.Builder(this).setTitle("Recent links").setItems(items) { _, index -> send(items[index], false) }.setNegativeButton("Close", null).show()
    }
    private fun send(link: String, fromShare: Boolean) {
        val parsed = try { Uri.parse(link) } catch (_: Exception) { null }
        if (parsed?.scheme !in listOf("http", "https") || parsed?.host.isNullOrBlank()) {
            status.text = "Invalid URL. Only http/https links are supported."; return
        }
        if (!isPaired()) { pendingShare = link; status.text = "Pair with tvBOX first."; scanQr(); return }
        if (sending) return
        sending = true
        status.text = "Sending to tvBOX…"
        val host = prefs.getString("host", "")!!
        val token = prefs.getString("token", "")!!
        worker.execute {
            val result = try {
                val connection = URL("http://$host:8765/send").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000; connection.readTimeout = 5000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                val data = "token=${URLEncoder.encode(token, "UTF-8")}&url=${URLEncoder.encode(link, "UTF-8")}".toByteArray(Charsets.UTF_8)
                connection.outputStream.use { it.write(data) }
                val code = connection.responseCode
                connection.disconnect()
                code == 200
            } catch (_: Exception) { false }
            runOnUiThread {
                sending = false
                if (result) {
                    saveHistory(link); pendingShare = null
                    if (fromShare) { Toast.makeText(this, "Sent to tvBOX", Toast.LENGTH_SHORT).show(); finish() }
                    else status.text = "✓ Sent to tvBOX"
                } else {
                    status.text = "Could not send. Check the tvBOX receiver, IP and Wi-Fi; re-scan QR if the TV IP changed."
                    if (fromShare) AlertDialog.Builder(this).setTitle("Send failed")
                        .setMessage("tvBOX did not respond. Start the TV receiver or re-pair if its IP changed.")
                        .setPositiveButton("Retry") { _, _ -> send(link, true) }
                        .setNegativeButton("Keep open", null).show()
                }
            }
        }
    }
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }
}
