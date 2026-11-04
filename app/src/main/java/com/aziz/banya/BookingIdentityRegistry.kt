package com.aziz.banya

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.util.UUID

/**
 * Permanent identity registry for new bookings.
 *
 * This class is not wired to the active booking flow yet. It requires dedicated
 * Firestore rules and an atomic reservation+identity design before activation.
 * Never use the reusable date/room/time slot ID as a financial receipt ID.
 */
data class BookingIdentity(
    val id: String,
    val slotBookingId: String,
    val date: String,
    val room: Int,
    val start: String
)

class BookingIdentityRegistry {
    private val collection = FirebaseFirestore.getInstance().collection("booking_identity_v1")

    fun createIdentity(b: Booking): BookingIdentity {
        val minute = java.time.LocalTime.parse(b.start).toSecondOfDay() / 60
        return BookingIdentity(
            id = UUID.randomUUID().toString(),
            slotBookingId = "${b.date}_r${b.room}_$minute",
            date = b.date,
            room = b.room,
            start = b.start
        )
    }

    // Reserved for future migration: do not call before security rules are deployed.
    fun save(identity: BookingIdentity, callback: (Boolean, String) -> Unit) {
        collection.document(identity.id).set(mapOf(
            "slotBookingId" to identity.slotBookingId,
            "date" to identity.date,
            "room" to identity.room,
            "start" to identity.start,
            "createdAt" to FieldValue.serverTimestamp()
        )).addOnSuccessListener {
            callback(true, "Брон идентификатори сақланди")
        }.addOnFailureListener {
            callback(false, "Брон идентификатори сақланмади")
        }
    }
}
