package com.aziz.banya

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalTime

/**
 * Each 30-minute room slot is a unique Firestore document.
 * Transaction reads all slots before writing; overlapping reservations conflict.
 * The UI must never report success until the server confirms the transaction.
 */
class CloudBookings {
    private val db = FirebaseFirestore.getInstance()
    private val slots = db.collection("booking_slots_v1")

    fun listen(onResult: (List<Booking>?, String?) -> Unit): ListenerRegistration =
        slots.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(null, "Сервер маълумотларини олиб бўлмади")
                return@addSnapshotListener
            }
            if (snapshot == null || snapshot.metadata.isFromCache) {
                onResult(null, "Интернет ёки сервер тасдиғи кутилмоқда")
                return@addSnapshotListener
            }
            val records = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    Booking(
                        room = (doc.getLong("room") ?: return@runCatching null).toInt(),
                        date = doc.getString("date") ?: return@runCatching null,
                        start = doc.getString("start") ?: return@runCatching null,
                        minutes = (doc.getLong("minutes") ?: return@runCatching null).toInt(),
                        name = doc.getString("name") ?: return@runCatching null
                    )
                }.getOrNull()
            }.distinctBy { "${it.room}|${it.date}|${it.start}" }
            onResult(records, null)
        }

    fun reserve(booking: Booking, onResult: (Boolean, String) -> Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            onResult(false, "Аввал тизимга киринг")
            return
        }
        val start = LocalTime.parse(booking.start)
        if (start.minute % 30 != 0 || booking.minutes !in listOf(60, 90, 120)) {
            onResult(false, "Вақтни 00 ёки 30 дақиқадан бошланг")
            return
        }
        val base = start.hour * 60 + start.minute
        val refs = (0 until booking.minutes / 30).map { offset ->
            val minute = base + offset * 30
            slots.document("${booking.date}_r${booking.room}_${minute}")
        }
        val bookingId = "${booking.date}_r${booking.room}_${base}"
        db.runTransaction { tx ->
            val existing = refs.map { tx.get(it) }
            if (existing.any { it.exists() }) {
                throw IllegalStateException("SLOT_TAKEN")
            }
            val fields = mapOf(
                "bookingId" to bookingId,
                "room" to booking.room,
                "date" to booking.date,
                "start" to booking.start,
                "minutes" to booking.minutes,
                "name" to booking.name,
                "createdBy" to FirebaseAuth.getInstance().currentUser!!.uid
            )
            refs.forEach { tx.set(it, fields) }
            true
        }.addOnSuccessListener {
            onResult(true, "Брон серверда сақланди")
        }.addOnFailureListener { e ->
            onResult(false, if (e.message?.contains("SLOT_TAKEN") == true)
                "Бу вақтда хона банд" else "Сервер бронни тасдиқламади. Интернетни текширинг")
        }
    }
}
