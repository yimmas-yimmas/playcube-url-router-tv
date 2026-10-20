package com.playcube.urlrouter

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.Build
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

class LinkReceiverService : Service() {
    companion object {
        const val PORT = 8765
        const val CHANNEL = "router_receiver"
        fun token(context: android.content.Context): String {
            val prefs = context.getSharedPreferences("router_settings", MODE_PRIVATE)
            val existing = prefs.getString("receiver_token", null)
            if (existing != null) return existing
            val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val created = bytes.joinToString("") { "%02x".format(it) }
            prefs.edit().putString("receiver_token", created).apply()
            return created
        }
    }
    private val running = AtomicBoolean(false)
    private var server: ServerSocket? = null
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "PlayCube link receiver", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(this, 0, Intent(this, InfoActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("PlayCube link receiver")
            .setContentText("Ready for links on local Wi-Fi")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(open).setOngoing(true).build()
        startForeground(1001, notification)
        running.set(true)
        worker = Thread { serve() }.also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun serve() {
        try {
            ServerSocket(PORT).use { socket ->
                server = socket
                while (running.get()) {
                    val client = try { socket.accept() } catch (_: Exception) { break }
                    Thread { handle(client) }.start()
                }
            }
        } catch (_: Exception) { }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            try {
                client.soTimeout = 4000
                val input = client.getInputStream().bufferedReader()
                val request = input.readLine() ?: return
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = input.readLine() ?: return
                    if (line.isEmpty()) break
                    val pos = line.indexOf(':')
                    if (pos > 0) headers[line.substring(0, pos).lowercase()] = line.substring(pos + 1).trim()
                    if (headers.size > 40) return
                }
                val method = request.substringBefore(' ')
                val path = request.substringAfter(' ').substringBefore(' ')
                val expected = token(this)
                val supplied = headers["x-playcube-token"]
                val body: String
                val status: String
                if (method == "GET" && path == "/") {
                    status = "200 OK"
                    body = """<!doctype html><html><meta name="viewport" content="width=device-width, initial-scale=1"><title>PlayCube Router</title><body style="font:18px sans-serif;max-width:560px;margin:40px auto;padding:15px"><h2>Send link to PlayCube</h2><form method="POST" action="/send"><input name="token" placeholder="TV access token" style="width:100%;padding:12px"><p><input name="url" type="url" placeholder="https://..." required style="width:100%;padding:12px"></p><button style="padding:12px">Send to TV</button></form></body></html>"""
                } else if (method == "POST" && path == "/send") {
                    val length = headers["content-length"]?.toIntOrNull() ?: 0
                    if (length !in 1..4096) { respond(client, "413 Payload Too Large", "Invalid request"); return }
                    val chars = CharArray(length)
                    var n = 0
                    while (n < length) {
                        val read = input.read(chars, n, length - n)
                        if (read < 0) return
                        n += read
                    }
                    val fields = String(chars).split('&').mapNotNull {
                        val parts = it.split('=', limit = 2)
                        if (parts.size != 2) null else java.net.URLDecoder.decode(parts[0], "UTF-8") to java.net.URLDecoder.decode(parts[1], "UTF-8")
                    }.toMap()
                    val url = fields["url"] ?: ""
                    val auth = fields["token"] ?: supplied
                    val uri = Uri.parse(url)
                    if (auth != expected) { respond(client, "403 Forbidden", "Incorrect access token"); return }
                    if (url.length > 2048 || uri.scheme?.lowercase() !in listOf("http", "https") || uri.host.isNullOrBlank()) {
                        respond(client, "400 Bad Request", "Enter a valid http/https URL"); return
                    }
                    val launch = Intent(this, RouterActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = uri
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    // Android TV may prohibit background activity starts. A notification
                    // provides a reliable, user-initiated way to open the URL.
                    val pending = PendingIntent.getActivity(this, 1, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(1002, Notification.Builder(this, CHANNEL)
                        .setContentTitle("Link received — open on TV")
                        .setContentText(uri.host)
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContentIntent(pending).setAutoCancel(true).build())
                    status = "200 OK"
                    body = "Link received. Open the PlayCube notification on the TV to launch it."
                } else {
                    status = "404 Not Found"
                    body = "Not found"
                }
                respond(client, status, body, method == "GET" && path == "/")
            } catch (_: Exception) { }
        }
    }

    private fun respond(client: Socket, status: String, body: String, html: Boolean = false) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        client.getOutputStream().write(("HTTP/1.1 $status\r\nContent-Type: ${if (html) "text/html" else "text/plain"}; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n").toByteArray(Charsets.UTF_8))
        client.getOutputStream().write(bytes)
        client.getOutputStream().flush()
    }

    override fun onDestroy() {
        running.set(false)
        try { server?.close() } catch (_: Exception) { }
        super.onDestroy()
    }
}
