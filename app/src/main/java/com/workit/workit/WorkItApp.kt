package com.workit.workit

import android.app.Application
import com.google.firebase.FirebaseApp

class WorkItApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}