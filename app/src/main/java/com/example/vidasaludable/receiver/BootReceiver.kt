package com.example.vidasaludable.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.vidasaludable.database.DatabaseHelper
import com.example.vidasaludable.notification.NotificationHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val dbHelper = DatabaseHelper(context)
            val routines = dbHelper.getAllRoutines()

            for (r in routines) {
                if (!r.done) {
                    NotificationHelper.scheduleRoutineAlarm(context, r.id, r.name, r.emoji, r.time)
                }
            }

            NotificationHelper.syncAllPeriodicAlarms(context)
        }
    }
}
