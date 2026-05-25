package com.example.leanmassdriss

import android.app.Application


class LeanMassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}