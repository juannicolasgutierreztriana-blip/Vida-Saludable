package com.example.vidasaludable.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.vidasaludable.receiver.AlarmReceiver
import java.util.Calendar

object NotificationHelper {

    const val REQ_CODE_WATER = 9001
    const val REQ_CODE_MOVEMENT = 9002
    const val REQ_CODE_REST = 9003

    fun scheduleRoutineAlarm(context: Context, routineId: Long, name: String, emoji: String, time: String?) {
        if (time == null || !time.contains(":")) return

        try {
            val parts = time.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DATE, 1)
            }

            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("routine_id", routineId)
                putExtra("routine_name", name)
                putExtra("routine_emoji", emoji)
            }

            val requestCode = (routineId % Integer.MAX_VALUE).toInt()
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (am != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                    } else {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                    }
                } catch (e: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                }
            }
        } catch (ignored: Exception) {
        }
    }

    fun cancelRoutineAlarm(context: Context, routineId: Long) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val requestCode = (routineId % Integer.MAX_VALUE).toInt()
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (am != null && pi != null) {
                am.cancel(pi)
            }
        } catch (ignored: Exception) {
        }
    }

    fun syncAllPeriodicAlarms(context: Context) {
        val prefs = context.getSharedPreferences("habitos_saludables", Context.MODE_PRIVATE)
        val masterEnabled = prefs.getBoolean("notif_master_enabled", true)

        val waterEnabled = masterEnabled && prefs.getBoolean("notif_water_enabled", true)
        val waterHours = prefs.getInt("notif_water_interval_hours", 2)

        val movementEnabled = masterEnabled && prefs.getBoolean("notif_movement_enabled", true)
        val movementHours = prefs.getInt("notif_movement_interval_hours", 2)

        val restEnabled = masterEnabled && prefs.getBoolean("notif_rest_enabled", true)
        val restHours = prefs.getInt("notif_rest_interval_hours", 3)

        updatePeriodicAlarm(context, "WATER", REQ_CODE_WATER, waterEnabled, waterHours)
        updatePeriodicAlarm(context, "MOVEMENT", REQ_CODE_MOVEMENT, movementEnabled, movementHours)
        updatePeriodicAlarm(context, "REST", REQ_CODE_REST, restEnabled, restHours)
    }

    private fun updatePeriodicAlarm(context: Context, pType: String, reqCode: Int, enabled: Boolean, hours: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("periodic_type", pType)
        }

        val pi = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (am != null) {
            if (enabled) {
                val intervalMillis = hours * 3600 * 1000L
                val triggerAtMillis = System.currentTimeMillis() + intervalMillis
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, pi)
            } else {
                am.cancel(pi)
            }
        }
    }
}
