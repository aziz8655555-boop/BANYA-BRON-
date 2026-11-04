package com.aziz.banya

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules local, inexact reminders for bookings currently received from Firestore.
 * Reconciliation cancels alarms for bookings that have been removed or edited.
 * Android may delay inexact alarms under battery restrictions.
 */
object BookingReminderScheduler {
    private const val PREFS = "banya_reminder_ids"
    private const val KEY = "scheduled"

    private fun key(b: Booking, ending: Boolean): String =
        "${b.date}|${b.room}|${b.start}|${b.minutes}|${b.name}|${ending}"

    private fun id(key: String): Int = key.hashCode()

    private fun intent(context: Context, key: String): PendingIntent {
        val i = Intent(context, BookingReminderReceiver::class.java).apply {
            action = "com.aziz.banya.REMINDER"
            data = android.net.Uri.parse("banya-reminder://${id(key)}")
        }
        return PendingIntent.getBroadcast(
            context, id(key), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun reconcile(context: Context, bookings: List<Booking>) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()
        val now = System.currentTimeMillis()
        val upcoming = mutableMapOf<String, Pair<Booking, Boolean>>()
        bookings.forEach { b ->
            val start = runCatching {
                LocalDateTime.of(LocalDate.parse(b.date), LocalTime.parse(b.start))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull() ?: return@forEach
            for (ending in listOf(false, true)) {
                val at = start + if (ending) (b.minutes - 10L) * 60000L else -15L * 60000L
                if (at > now && at < now + 14L * 24 * 3600000L)
                    upcoming[key(b, ending)] = b to ending
            }
        }
        (old - upcoming.keys).forEach { alarms.cancel(intent(context, it)) }
        upcoming.forEach { (k, pair) ->
            val (b, ending) = pair
            val start = LocalDateTime.of(LocalDate.parse(b.date), LocalTime.parse(b.start))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val at = start + if (ending) (b.minutes - 10L) * 60000L else -15L * 60000L
            val pending = intent(context, k)
            // Update the existing PendingIntent with the latest booking details.
            val messageIntent = Intent(context, BookingReminderReceiver::class.java).apply {
                action = "com.aziz.banya.REMINDER"
                data = android.net.Uri.parse("banya-reminder://${id(k)}")
                putExtra("room", b.room)
                putExtra("name", b.name)
                putExtra("ending", ending)
                putExtra("notificationId", id(k))
            }
            val populated = PendingIntent.getBroadcast(
                context, id(k), messageIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, populated)
        }
        prefs.edit().putStringSet(KEY, upcoming.keys.toSet()).apply()
    }
}
