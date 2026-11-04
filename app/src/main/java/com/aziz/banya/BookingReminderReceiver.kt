package com.aziz.banya

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Displays booking reminders dispatched by the Android alarm scheduler.
 * Scheduling and runtime notification permission are integrated separately.
 */
class BookingReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "banya_booking_reminders"
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Брон эслатмалари", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val room = intent.getIntExtra("room", 0)
        val name = intent.getStringExtra("name").orEmpty().take(120)
        val ending = intent.getBooleanExtra("ending", false)
        if (room !in 1..6) return
        val title = if (ending) "Брон тугашига 10 дақиқа қолди"
            else "Брон бошланишига 15 дақиқа қолди"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("${room}-хона • $name")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(intent.getIntExtra("notificationId", room), notification)
    }
}
