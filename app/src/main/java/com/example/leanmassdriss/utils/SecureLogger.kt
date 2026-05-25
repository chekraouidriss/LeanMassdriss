package com.example.leanmassdriss.utils

import android.util.Log
import com.example.leanmassdriss.BuildConfig

/**
 * OWASP MASVS-RESILIENCE-3: Centralized production-safe logger.
 * Strips logs in release builds to prevent leakage of sensitive healthcare data.
 */
object SecureLogger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        }
    }
    
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }
}
