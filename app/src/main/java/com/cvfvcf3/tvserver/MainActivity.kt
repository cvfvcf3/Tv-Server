package com.cvfvcf3.tvserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private val uiHandler = Handler(Looper.getMainLooper())

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val msg = intent.getStringExtra("msg") ?: return
            addLog(msg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        registerReceiver(logReceiver, IntentFilter("com.cvfvcf3.tvserver.LOG"))

        btnStart.setOnClickListener { startServer() }
        btnStop.setOnClickListener { stopServer() }

        startServer()
    }

    private fun startServer() {
        val intent = Intent(this, ServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        tvStatus.text = "● Server Running — Port 9000"
        tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
        btnStart.isEnabled = false
        btnStop.isEnabled = true
    }

    private fun stopServer() {
        stopService(Intent(this, ServerService::class.java))
        tvStatus.text = "● Server Stopped"
        tvStatus.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        btnStart.isEnabled = true
        btnStop.isEnabled = false
    }

    private fun addLog(msg: String) {
        uiHandler.post {
            val lines = tvLog.text.toString().split("\n").takeLast(20)
            tvLog.text = (lines + msg).joinToString("\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(logReceiver)
    }
}
