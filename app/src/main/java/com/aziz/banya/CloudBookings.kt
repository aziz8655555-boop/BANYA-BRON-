package com.aziz.banya

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import java.time.LocalTime
import java.util.UUID

class CloudBookings {
    private val db = FirebaseFirestore.getInstance()
    private val slots = db.collection("booking_slots_v1")

    fun listen(onResult: (List<Booking>?, String?) -> Unit): ListenerRegistration =
        slots.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
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
                        doc.getString("name") ?: return@runCatching null,
                        doc.getString("permanentId"),
                        doc.getString("phone") ?: ""
                    )
                }.getOrNull()
            }.distinctBy { "${it.room}|${it.date}|${it.start}" }
            onResult(records, null)
        }

    // Existing slot identity is kept unchanged for backward compatibility.
    // Do not use this as a unique payment ID: a cancelled slot may be booked again.
    fun slotBookingId(b: Booking): String {
        val minute = LocalTime.parse(b.start).toSecondOfDay() / 60
        return "${b.date}_r${b.room}_$minute"
    }

    private fun references(b: Booking) = (0 until b.minutes / 30).map { n ->
        val minute = LocalTime.parse(b.start).toSecondOfDay() / 60 + n * 30
        slots.document("${b.date}_r${b.room}_$minute")
    }

    private fun fields(b: Booking, permanentId: String? = null): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            "bookingId" to slotBookingId(b),
            "room" to b.room, "date" to b.date,
            "start" to b.start, "minutes" to b.minutes,
            "name" to b.name.trim(),
            "createdBy" to (FirebaseAuth.getInstance().currentUser?.uid ?: "")
        )
        (permanentId ?: b.permanentId)?.let { data["permanentId"] = it }
        if (b.phone.isNotBlank()) data["phone"] = b.phone
        return data
    }

    // New bookings receive a permanent identity in a separate metadata document.
    // Slot documents remain unchanged to preserve the deployed Firestore rules.
    // Metadata must only be written after the corresponding security rules are deployed.
    fun newPermanentBookingId(): String = UUID.randomUUID().toString()

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

    /**
     * Atomic reservation with a permanent financial identity.
     * Activate only after booking_identity_v1 rules are published in Firebase.
     * Existing reserve() remains unchanged for currently deployed clients.
     */
    fun reserveWithIdentity(
        b: Booking,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            callback(false, "Тизимга киринг", null)
            return
        }
        val refs = references(b)
        val permanentId = newPermanentBookingId()
        val identityRef = db.collection("booking_identity_v1").document(permanentId)
        db.runTransaction { tx ->
            // Firestore transactions require all reads before any writes.
            val slotDocs = refs.map { tx.get(it) }
            val identityDoc = tx.get(identityRef)
            if (slotDocs.any { it.exists() } || identityDoc.exists()) {
                throw IllegalStateException("SLOT_TAKEN")
            }
            val data = fields(b, permanentId)
            refs.forEach { tx.set(it, data) }
            tx.set(identityRef, mapOf(
                "slotBookingId" to slotBookingId(b),
                "date" to b.date,
                "room" to b.room,
                "start" to b.start,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))
            true
        }.addOnSuccessListener {
            callback(true, "Брон ва доимий ID сақланди", permanentId)
        }.addOnFailureListener { error ->
            val msg = if (error.message?.contains("SLOT_TAKEN") == true)
                "Бу вақт банд" else "Брон серверда тасдиқланмади"
            callback(false, msg, null)
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
            val data = fields(replacement, old.permanentId)
            newRefs.forEach { tx.set(it, data) }
            true
        }.addOnSuccessListener { callback(true, "Брон ўзгартирилди") }
            .addOnFailureListener { callback(false, "Ўзгартириб бўлмади. Вақт банд ёки маълумот ўзгарган") }
    }
}
