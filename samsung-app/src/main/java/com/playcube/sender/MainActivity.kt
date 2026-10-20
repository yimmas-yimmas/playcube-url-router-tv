package com.playcube.sender

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var linkInput: EditText
    private lateinit var pairInfo: TextView
    private val prefs by lazy { getSharedPreferences("pair", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(32), dp(22), dp(22))
            setBackgroundColor(Color.WHITE)
        }
        fun label(value: String, size: Float): TextView = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(Color.BLACK)
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(label("Send to PlayCube", 25f))
        pairInfo = label("", 15f)
        root.addView(pairInfo)
        root.addView(Button(this).apply {
            text = "Scan PlayCube QR code"
            setOnClickListener { scanQr() }
        })
        linkInput = EditText(this).apply {
            hint = "https://example.com/video.m3u8"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(false)
            minLines = 2
        }
        root.addView(linkInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(Button(this).apply {
            text = "Send to PlayCube"
            setOnClickListener { sendLink() }
        })
        status = label("", 15f)
        root.addView(status)
        root.addView(label("Phone and PlayCube must be on the same Wi-Fi. Start the Wi-Fi receiver on PlayCube before sending.", 13f))
        setContentView(root)
        refreshPair()
        if (intent?.action == Intent.ACTION_SEND) acceptShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_SEND) acceptShare(intent)
    }

    private fun acceptShare(intent: Intent) {
        val shared = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
        val url = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE).find(shared)?.value
        if (url != null) {
            linkInput.setText(url)
            if (isPaired()) sendLink()
            else status.text = "Scan the TV QR code once to pair, then tap Send."
        } else status.text = "No http/https link found in shared text."
    }

    private fun scanQr() {
        status.text = "Opening QR scanner…"
        GmsBarcodeScanning.getClient(this).startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue.orEmpty()
                val uri = Uri.parse(value)
                val host = uri.host
                val token = uri.getQueryParameter("token")
                val port = if (uri.port > 0) uri.port else 8765
                if (uri.scheme != "http" || host.isNullOrBlank() || token.isNullOrBlank() || port != 8765) {
                    status.text = "This isn't a valid PlayCube receiver QR code."
                    return@addOnSuccessListener
                }
                // Only allow private LAN IPv4 addresses; prevents accidental sending of tokens to public hosts.
                val parts = host.split('.').mapNotNull { it.toIntOrNull() }
                val local = parts.size == 4 && parts.all { it in 0..255 } &&
                    (parts[0] == 10 || (parts[0] == 172 && parts[1] in 16..31) ||
                     (parts[0] == 192 && parts[1] == 168))
                if (!local) {
                    status.text = "PlayCube QR must contain a private local IPv4 address."
                    return@addOnSuccessListener
                }
                prefs.edit().putString("host", host).putString("token", token).apply()
                refreshPair()
                status.text = "Paired with PlayCube. Ready to send."
                if (linkInput.text.isNotBlank()) sendLink()
            }
            .addOnCanceledListener { status.text = "QR scan cancelled." }
            .addOnFailureListener { status.text = "QR scanner unavailable: ${it.localizedMessage}" }
    }

    private fun isPaired() = !prefs.getString("host", null).isNullOrBlank() &&
        !prefs.getString("token", null).isNullOrBlank()

    private fun refreshPair() {
        pairInfo.text = if (isPaired()) "Paired TV: ${prefs.getString("host", "")}:8765" else "Not paired — scan the QR code on your TV"
    }

    private fun sendLink() {
        val link = linkInput.text.toString().trim()
        val parsed = Uri.parse(link)
        if (parsed.scheme !in listOf("http", "https") || parsed.host.isNullOrBlank()) {
            status.text = "Enter a valid http/https link."
            return
        }
        if (!isPaired()) { status.text = "Scan the PlayCube QR code first."; return }
        val host = prefs.getString("host", "")!!
        val token = prefs.getString("token", "")!!
        status.text = "Sending…"
        worker.execute {
            val result = try {
                val conn = URL("http://$host:8765/send").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                val body = "token=${URLEncoder.encode(token, "UTF-8")}&url=${URLEncoder.encode(link, "UTF-8")}".toByteArray(Charsets.UTF_8)
                conn.outputStream.use { it.write(body) }
                val code = conn.responseCode
                val response = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()
                if (code == 200) "Sent successfully. ${response.take(160)}" else "TV error $code: ${response.take(150)}"
            } catch (e: Exception) { "Could not reach PlayCube: ${e.localizedMessage}" }
            runOnUiThread { status.text = result }
        }
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }
}
