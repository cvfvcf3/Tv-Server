package com.cvfvcf3.tvserver

import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.*
import java.net.*
import java.util.concurrent.*

class ServerService : Service() {

    private val PORT = 9000
    private val TAG = "TVServer"
    private val CHANNEL_ID = "tv_server_channel"
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification("Server chal raha hai — Port 9000"))
        startServer()
        return START_STICKY
    }

    private fun startServer() {
        if (isRunning) return
        isRunning = true
        executor.execute {
            try {
                serverSocket = ServerSocket(PORT)
                broadcastLog("Server ready — Port $PORT")
                while (isRunning) {
                    try {
                        val client = serverSocket!!.accept()
                        broadcastLog("Phone connected: ${client.inetAddress.hostAddress}")
                        executor.execute { handleClient(client) }
                    } catch (e: Exception) {
                        if (isRunning) Log.e(TAG, "Accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                broadcastLog("Server error: ${e.message}")
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 0
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val sb = StringBuilder()
            while (!socket.isClosed) {
                val ch = reader.read()
                if (ch == -1) break
                val c = ch.toChar()
                if (c == '&') {
                    val cmd = sb.toString().trim()
                    if (cmd.isNotEmpty()) processCommand(cmd)
                    sb.clear()
                } else {
                    sb.append(c)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client error: ${e.message}")
        } finally {
            try { socket.close() } catch (e: Exception) {}
            broadcastLog("Phone disconnected")
        }
    }

    private fun processCommand(cmd: String) {
        broadcastLog("CMD: $cmd")
        try {
            when {
                cmd.startsWith("key=action;") -> {
                    val keycode = cmd.removePrefix("key=action;").trim()
                    executeShell("input keyevent $keycode")
                }
                cmd.startsWith("key=editText;") -> {
                    val text = cmd.removePrefix("key=editText;").trim()
                    executeShell("input text '$text'")
                }
                cmd.startsWith("fname=") -> {
                    val name = cmd.removePrefix("fname=").split("&")[0]
                    broadcastLog("Connected: $name")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Command error: ${e.message}")
        }
    }

    private fun executeShell(command: String) {
        try {
            Runtime.getRuntime().exec(command).waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Shell error: $command — ${e.message}")
        }
    }

    private fun broadcastLog(msg: String) {
        val intent = Intent("com.cvfvcf3.tvserver.LOG")
        intent.putExtra("msg", msg)
        sendBroadcast(intent)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TV Remote Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "TV Server",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { serverSocket?.close() } catch (e: Exception) {}
        executor.shutdownNow()
    }
}
