package com.example.vidasaludable.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.vidasaludable.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "habitos_reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("habitos_saludables", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("notif_master_enabled", true)) {
            return
        }

        val userName = prefs.getString("user_name", "Usuario")

        val pType = intent.getStringExtra("periodic_type")
        val title: String
        val content: String
        val emoji: String

        when (pType) {
            "WATER" -> {
                if (!prefs.getBoolean("notif_water_enabled", true)) return
                emoji = "💧"
                title = "💧 ¡Hora de Hidratarte, $userName!"
                content = "Recuerda tomar un vaso de agua fresca para mantener tu cuerpo saludable."
            }
            "MOVEMENT" -> {
                if (!prefs.getBoolean("notif_movement_enabled", true)) return
                emoji = "🚶"
                title = "🚶 ¡Tiempo de Moverte, $userName!"
                content = "Levántate un momento, da una caminata breve o haz estiramientos suaves."
            }
            "REST" -> {
                if (!prefs.getBoolean("notif_rest_enabled", true)) return
                emoji = "😴"
                title = "😴 ¡Pausa de Descanso, $userName!"
                content = "Es momento de hacer una pausa activa, relajar tu mente y descansar."
            }
            else -> {
                val rName = intent.getStringExtra("routine_name")
                val rEmoji = intent.getStringExtra("routine_emoji")
                content = if (rName != null) "Es momento de: $rName" else "Es momento de cumplir tus hábitos."
                emoji = rEmoji ?: "⏰"
                title = "$emoji ¡Hora de tu rutina, $userName!"
            }
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Hábitos y Salud",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones y alarmas para rutinas, agua, movimiento y descanso"
            }
            nm?.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pi)

        if (nm != null) {
            val notifId = System.currentTimeMillis().toInt()
            nm.notify(notifId, builder.build())
        }
    }
}
