package com.aziz.banya

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalTime

class CloudBookings {
    private val db = FirebaseFirestore.getInstance()
    private val slots = db.collection("booking_slots_v1")

    fun listen(onResult: (List<Booking>?, String?) -> Unit): ListenerRegistration =
        slots.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(null, "Сервер билан алоқа йўқ")
                return@addSnapshotListener
            }
            if (snapshot == null || snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites()) {
                onResult(null, "Сервер тасдиғи кутилмоқда")
                return@addSnapshotListener
            }
            val records = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    Booking(
                        (doc.getLong("room") ?: return@runCatching null).toInt(),
                        doc.getString("date") ?: return@runCatching null,
                        doc.getString("start") ?: return@runCatching null,
                        (doc.getLong("minutes") ?: return@runCatching null).toInt(),
                        doc.getString("name") ?: return@runCatching null
                    )
                }.getOrNull()
            }.distinctBy { "${it.room}|${it.date}|${it.start}" }
            onResult(records, null)
        }

    private fun references(b: Booking) = (0 until b.minutes / 30).map { n ->
        val minute = LocalTime.parse(b.start).toSecondOfDay() / 60 + n * 30
        slots.document("${b.date}_r${b.room}_$minute")
    }

    private fun fields(b: Booking): Map<String, Any> {
        val minute = LocalTime.parse(b.start).toSecondOfDay() / 60
        return mapOf(
            "bookingId" to "${b.date}_r${b.room}_$minute",
            "room" to b.room, "date" to b.date,
            "start" to b.start, "minutes" to b.minutes,
            "name" to b.name.trim(),
            "createdBy" to (FirebaseAuth.getInstance().currentUser?.uid ?: "")
        )
    }

    fun reserve(b: Booking, callback: (Boolean, String) -> Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            callback(false, "Тизимга киринг")
            return
        }
        val refs = references(b)
        db.runTransaction { tx ->
            val docs = refs.map { tx.get(it) }
            if (docs.any { it.exists() }) throw IllegalStateException("SLOT_TAKEN")
            val data = fields(b)
            refs.forEach { tx.set(it, data) }
            true
        }.addOnSuccessListener { callback(true, "Брон серверда сақланди") }
            .addOnFailureListener {
                callback(false, if (it.message?.contains("SLOT_TAKEN") == true)
                    "Бу вақт банд" else "Сервер тасдиқламади. Интернетни текширинг")
            }
    }

    fun delete(b: Booking, callback: (Boolean, String) -> Unit) {
        val refs = references(b)
        db.runTransaction { tx ->
            val docs = refs.map { tx.get(it) }
            val id = fields(b)["bookingId"]
            if (docs.any { !it.exists() || it.getString("bookingId") != id })
                throw IllegalStateException("CHANGED")
            refs.forEach { tx.delete(it) }
            true
        }.addOnSuccessListener { callback(true, "Брон ўчирилди") }
            .addOnFailureListener { callback(false, "Ўчириб бўлмади. Янгилаб қайта урининг") }
    }

    fun edit(old: Booking, replacement: Booking, callback: (Boolean, String) -> Unit) {
        val oldRefs = references(old)
        val newRefs = references(replacement)
        val allRefs = (oldRefs + newRefs).distinctBy { it.path }
        db.runTransaction { tx ->
            val docs = allRefs.associateBy({ it.path }, { tx.get(it) })
            val oldId = fields(old)["bookingId"]
            if (oldRefs.any { docs[it.path]?.getString("bookingId") != oldId })
                throw IllegalStateException("CHANGED")
            val oldPaths = oldRefs.map { it.path }.toSet()
            if (newRefs.any { it.path !in oldPaths && docs[it.path]?.exists() == true })
                throw IllegalStateException("SLOT_TAKEN")
            oldRefs.forEach { tx.delete(it) }
            val data = fields(replacement)
            newRefs.forEach { tx.set(it, data) }
            true
        }.addOnSuccessListener { callback(true, "Брон ўзгартирилди") }
            .addOnFailureListener { callback(false, "Ўзгартириб бўлмади. Вақт банд ёки маълумот ўзгарган") }
    }
}
