package com.example

import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.data.AppContainer
import com.example.ui.crash.CrashReportActivity
import java.io.File

class JumphubApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Global uncaught crash catcher prevents OS force-close and shows error details
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("JUMPHUB_CRASH", "FATAL CRASH on thread ${thread.name}", throwable)
                val crashText = throwable.stackTraceToString()
                try {
                    val crashFile = File(filesDir, "last_crash.txt")
                    crashFile.writeText(crashText)
                } catch (e: Exception) {
                    // ignore file writing errors
                }

                val intent = Intent(this, CrashReportActivity::class.java).apply {
                    putExtra("error_details", crashText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (e: Throwable) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        container = AppContainer(this)
    }
}
