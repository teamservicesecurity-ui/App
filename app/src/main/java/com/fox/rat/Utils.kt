package com.fox.rat

import android.content.Context
import android.provider.Settings
import com.google.gson.Gson

object Utils {
    private val gson = Gson()

    fun toJson(obj: Any): String = gson.toJson(obj)

    fun fromJson(json: String): Map<String, Any> {
        return gson.fromJson(json, Map::class.java) as Map<String, Any>
    }

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
}
