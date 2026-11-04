package com.aziz.banya

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue

/**
 * Independent financial ledger. Deleting booking_slots_v1 documents never deletes receipts.
 * A stable unique payment ID is supplied by the booking/payment flow; repeated writes are rejected.
 * This service is intentionally not connected to the UI until rules and payment flow are ready.
 */
data class PaymentReceipt(
    val id: String,
    val bookingId: String,
    val date: String,
    val room: Int,
    val amountKzt: Int,
    val method: String,
    val recordedBy: String
)

class PaymentLedger {
    private val collection = FirebaseFirestore.getInstance().collection("payment_receipts")

    fun recordPayment(
        bookingId: String,
        paymentId: String,
        date: String,
        room: Int,
        amountKzt: Int,
        method: String,
        callback: (Boolean, String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || bookingId.isBlank() ||
            !paymentId.matches(Regex("[A-Za-z0-9_-]{16,100}")) || amountKzt <= 0 ||
            room !in 1..6 || !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ||
            method !in setOf("cash", "kaspi", "card")) {
            callback(false, "Тўлов маълумотлари нотўғри")
            return
        }
        val ref = collection.document(paymentId)
        val db = FirebaseFirestore.getInstance()
        db.runTransaction { tx ->
            val existing = tx.get(ref)
            if (existing.exists()) {
                // The same payment ID must never be charged a second time.
                false
            } else {
                tx.set(ref, mapOf(
                    "bookingId" to bookingId,
                    "date" to date,
                    "room" to room,
                    "amountKzt" to amountKzt,
                    "method" to method,
                    "recordedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                ))
                true
            }
        }.addOnSuccessListener { created ->
            callback(created, if (created) "Тўлов ҳисоботга сақланди"
                else "Бу тўлов аввал сақланган")
        }.addOnFailureListener { callback(false, "Тўлов сақланмади") }
    }

    /**
     * Record at most one full payment for a permanent booking identity.
     * The identity document is read inside the same transaction, so callers
     * cannot accidentally attach a payment to an unrelated slot ID.
     * Not yet called by the UI: the active booking UUID must first be
     * carried from Firestore to the booking details screen.
     */
    fun recordFullBookingPayment(
        permanentBookingId: String,
        amountKzt: Int,
        method: String,
        callback: (Boolean, String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null ||
            !permanentBookingId.matches(
                Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            ) ||
            amountKzt <= 0 || amountKzt > 10000000 ||
            method !in setOf("cash", "kaspi", "card")) {
            callback(false, "Тўлов маълумотлари нотўғри")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val identityRef = db.collection("booking_identity_v1").document(permanentBookingId)
        // The receipt document ID is the permanent booking UUID.
        // Therefore a repeated payment cannot create a second receipt.
        val receiptRef = collection.document(permanentBookingId)
        db.runTransaction { tx ->
            val identity = tx.get(identityRef)
            val receipt = tx.get(receiptRef)
            if (!identity.exists()) throw IllegalStateException("IDENTITY_NOT_FOUND")
            if (receipt.exists()) return@runTransaction false

            val date = identity.getString("date")
                ?: throw IllegalStateException("INVALID_IDENTITY")
            val room = identity.getLong("room")?.toInt()
                ?: throw IllegalStateException("INVALID_IDENTITY")
            if (room !in 1..6) throw IllegalStateException("INVALID_IDENTITY")

            tx.set(receiptRef, mapOf(
                "bookingId" to permanentBookingId,
                "date" to date,
                "room" to room,
                "amountKzt" to amountKzt,
                "method" to method,
                "recordedBy" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            ))
            true
        }.addOnSuccessListener { created ->
            callback(created, if (created) "Тўлов ҳисоботга сақланди"
                else "Бу брон учун тўлов аввал сақланган")
        }.addOnFailureListener { error ->
            callback(false, if (error.message?.contains("IDENTITY_NOT_FOUND") == true)
                "Брон идентификатори топилмади" else "Тўлов сақланмади")
        }
    }

    fun listenForDate(date: String, onResult: (List<PaymentReceipt>?, String?) -> Unit): ListenerRegistration =
        collection.whereEqualTo("date", date).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(null, "Ҳисоботни юклаб бўлмади")
                return@addSnapshotListener
            }
            val receipts = snapshot?.documents?.mapNotNull { doc ->
                val room = doc.getLong("room")?.toInt() ?: return@mapNotNull null
                val amount = doc.getLong("amountKzt")?.toInt() ?: return@mapNotNull null
                PaymentReceipt(
                    doc.id,
                    doc.getString("bookingId") ?: return@mapNotNull null,
                    doc.getString("date") ?: return@mapNotNull null,
                    room,
                    amount,
                    doc.getString("method") ?: return@mapNotNull null,
                    doc.getString("recordedBy") ?: return@mapNotNull null
                )
            } ?: emptyList()
            onResult(receipts, null)
        }
}
