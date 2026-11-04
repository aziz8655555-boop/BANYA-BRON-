package com.aziz.banya

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import java.util.UUID

/**
 * Independent financial ledger. Deleting booking_slots_v1 documents never deletes receipts.
 * Every payment has its own immutable UUID; a reused room/time cannot overwrite history.
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
        date: String,
        room: Int,
        amountKzt: Int,
        method: String,
        callback: (Boolean, String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || bookingId.isBlank() || amountKzt <= 0 ||
            room !in 1..6 || !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ||
            method !in setOf("cash", "kaspi", "card")) {
            callback(false, "Тўлов маълумотлари нотўғри")
            return
        }
        val id = UUID.randomUUID().toString()
        collection.document(id).set(mapOf(
            "bookingId" to bookingId,
            "date" to date,
            "room" to room,
            "amountKzt" to amountKzt,
            "method" to method,
            "recordedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )).addOnSuccessListener { callback(true, "Тўлов ҳисоботга сақланди") }
          .addOnFailureListener { callback(false, "Тўлов сақланмади") }
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
